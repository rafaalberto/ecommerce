(ns ecommerce.db.product
  (:require [datomic.api :as d]
            [ecommerce.db.entity :as db.entity]
            [ecommerce.db.model :as db.model]
            [schema.core :as s])
  (:use [clojure pprint])
  (:import (java.util UUID)))

(defn add! [connection products]
  (d/transact connection products))

(s/defn update!
  [connection
   former-product :- db.model/Product
   new-product :- db.model/Product]
  (let [product-id (:product/id former-product)
        attributes (disj (clojure.set/intersection (set (keys former-product))
                                                   (set (keys new-product)))
                         :product/id)
        transactions (map (fn [attribute] [:db/cas [:product/id product-id] attribute
                                           (get former-product attribute)
                                           (get new-product attribute)])
                          attributes)]
    (d/transact connection transactions)))

(s/defn update-price!
  [connection
   product-id :- UUID
   former-price :- BigDecimal
   new-price :- BigDecimal]
  (d/transact connection [[:db/cas [:product/id product-id]
                           :product/price former-price new-price]]))

(defn delete! [connection product]
  (d/transact connection [[:db/retract [:product/id (:product/id product)]
                           :product/name (:product/name product)]]))

(s/defn view! [connection product-id :- UUID]
  (d/transact connection [[:increment-view product-id]]))

(s/defn find-all :- [db.model/Product]
  [db]
  (db.entity/to-entity (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
                              :where [?product :product/name]]
                            db)))

(s/defn ^:private find-by-id :- (s/maybe db.model/Product)
  [db product-id :- UUID]
  (let [result (d/pull db '[* {:product/category [*]}] [:product/id product-id])
        product (db.entity/to-entity result)]
    (if (:product/id product)
      product
      nil)))

(s/defn find-one [db product-id :- UUID]
  (let [product (find-by-id db product-id)]
    (when (nil? product)
      (throw (ex-info "There is no product" {:type :not-found
                                             :id   product-id})))
    product))

(def rules
  '[
    [(stock ?product ?stock)
     [?product :product/quantity ?stock]]
    [(stock ?product ?stock)
     [?product :product/digital true]
     [(ground 100) ?stock]]
    [(sell? ?product)
     (stock ?product ?stock)
     [(> ?stock 0)]]
    [(product-at-category ?product ?category-name)
     [?category :category/name ?category-name]
     [?product :product/category ?category]]
    ])

(s/defn find-all-stock-available :- [db.model/Product]
  [db]
  (db.entity/to-entity (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
                              :where [?product :product/name]
                              [?product :product/quantity ?stock]
                              [(> ?stock 0)]]
                            db)))

(s/defn find-all-stock-available-rules :- [db.model/Product]
  [db]
  (db.entity/to-entity (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
                              :in $ %
                              :where (sell? ?product)]
                            db rules)))

(s/defn find-one-stock-available :- (s/maybe db.model/Product)
  [db product-id :- UUID]
  (let [query '[:find (pull ?product [* {:product/category [*]}]) .
                :in $ ?product-id
                :where [?product :product/id ?product-id]
                [?product :product/quantity ?stock]
                [(> ?stock 0)]]
        result (d/q query db product-id)
        product (db.entity/to-entity result)]
    (if (:product/id product)
      product
      nil)))

(s/defn find-one-stock-available-rule :- (s/maybe db.model/Product)
  [db product-id :- UUID]
  (let [query '[:find (pull ?product [* {:product/category [*]}]) .
                :in $ % ?product-id
                :where [?product :product/id ?product-id]
                (sell? ?product)]
        result (d/q query db rules product-id)
        product (db.entity/to-entity result)]
    (if (:product/id product)
      product
      nil)))

(defn find-all-by-minimum-price [db minimum-price]
  (d/q '[:find ?id ?name ?price
         :in $ ?minimum-price
         :keys id name price
         :where [?entity :product/price ?price]
         [(>= ?price ?minimum-price)]
         [?entity :product/id ?id]
         [?entity :product/name ?name]]
       db minimum-price))

(defn find-all-by-keyword [db product-keyword]
  (d/q '[:find (pull ?product [*])
         :in $ ?product-keyword
         :where [?product :product/keyword ?product-keyword]]
       db product-keyword))

(defn fetch-products-and-categories [db]
  (d/q '[:find ?product-name ?category-name
         :keys product category
         :where [?product :product/name ?product-name]
         [?product :product/category ?category]
         [?category :category/name ?category-name]]
       db))

(defn find-all-by-category [db category-name]
  (d/q '[:find (pull ?category [:category/name {:product/_category [:product/name :product/slug]}])
         :in $ ?category-name
         :where [?category :category/name ?category-name]]
       db category-name))

(defn fetch-summary [db]
  (d/q '[:find (min ?price) (max ?price) (count ?price) (sum ?price)
         :keys min max quantity amount
         :with ?product
         :where [?product :product/price ?price]]
       db))

(defn fetch-summary-by-category [db]
  (d/q '[:find ?category-name (min ?price) (max ?price) (count ?price) (sum ?price)
         :keys category min max quantity amount
         :with ?product
         :where [?product :product/price ?price]
         [?product :product/category ?category]
         [?category :category/name ?category-name]]
       db))

(defn fetch-most-expensive [db]
  (d/q '[:find (pull ?product [*])
         :where [(q '[:find (max ?price)
                      :where [_ :product/price ?price]] $) [[?price]]]
         [?product :product/price ?price]]
       db))

(s/defn find-by-categories-and-digital :- [db.model/Product]
  [db
   categories :- [s/Str]
   digital? :- s/Bool]
  (db.entity/to-entity (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
                              :in $ % [?category-name ...] ?is-digital?
                              :where (product-at-category ?product ?category-name)
                              [?product :product/digital ?is-digital?]]
                            db rules categories digital?)))