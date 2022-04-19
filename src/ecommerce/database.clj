(ns ecommerce.database
  (:require [datomic.api :as d]
            [schema.core :as s]
            [ecommerce.model :as model]
            [clojure.walk :as walk]
            [ecommerce.db-schema :refer [schema]])
  (:use [clojure pprint])
  (:import (java.util UUID)))

(def db-uri "datomic:dev://localhost:4334/ecommerce")

(defn open-connection! []
  (d/create-database db-uri)
  (d/connect db-uri))

(defn delete-database! []
  (d/delete-database db-uri))

(defn create-schema! [connection]
  (d/transact connection schema))

(defn- dissoc-db-id [entities]
  (if (map? entities)
    (dissoc entities :db/id)
    entities))

(defn- to-entity [entities]
  (walk/prewalk dissoc-db-id entities))

(s/defn all-products :- [model/Product]
  [db]
  (to-entity (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
                    :where [?product :product/name]]
                  db)))

(s/defn ^:private find-product-by-id :- (s/maybe model/Product)
  [db product-id :- UUID]
  (let [result (d/pull db '[* {:product/category [*]}] [:product/id product-id])
        product (to-entity result)]
    (if (:product/id product)
      product
      nil)))

(s/defn one-product! [db product-id :- UUID]
  (let [product (find-product-by-id db product-id)]
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
    ])

(s/defn all-products-available :- [model/Product]
  [db]
  (to-entity (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
                    :where [?product :product/name]
                    [?product :product/quantity ?stock]
                    [(> ?stock 0)]]
                  db)))

(s/defn all-products-available-with-rules :- [model/Product]
  [db]
  (to-entity (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
                    :in $ %
                    :where (sell? ?product)]
                  db rules)))

(s/defn one-product-available :- (s/maybe model/Product)
  [db product-id :- UUID]
  (let [query '[:find (pull ?product [* {:product/category [*]}]) .
                :in $ ?product-id
                :where [?product :product/id ?product-id]
                [?product :product/quantity ?stock]
                [(> ?stock 0)]]
        result (d/q query db product-id)
        product (to-entity result)]
    (if (:product/id product)
      product
      nil)))

(s/defn one-product-available-with-rule :- (s/maybe model/Product)
  [db product-id :- UUID]
  (let [query '[:find (pull ?product [* {:product/category [*]}]) .
                :in $ % ?product-id
                :where [?product :product/id ?product-id]
                (sell? ?product)]
        result (d/q query db rules product-id)
        product (to-entity result)]
    (if (:product/id product)
      product
      nil)))

(defn all-products-by-minimum-price [db minimum-price]
  (d/q '[:find ?id ?name ?price
         :in $ ?minimum-price
         :keys id name price
         :where [?entity :product/price ?price]
         [(>= ?price ?minimum-price)]
         [?entity :product/id ?id]
         [?entity :product/name ?name]]
       db minimum-price))

(defn all-products-by-keyword [db product-keyword]
  (d/q '[:find (pull ?product [*])
         :in $ ?product-keyword
         :where [?product :product/keyword ?product-keyword]]
       db product-keyword))

(defn add-products! [connection products]
  (d/transact connection products))

(s/defn add-categories!
  [connection categories :- [model/Category]]
  (d/transact connection categories))

(defn- update-converter [product]
  (let [fields (:fields product)]
    (reduce (fn [items item] (conj items
                                   [:db/add [:product/id (:id product)]
                                    (first (keys item)) (first (vals item))]))
            []
            fields)))

(defn update-product! [connection product]
  (println (update-converter product))
  (d/transact connection (update-converter product)))

(defn delete-products! [connection product]
  (d/transact connection [[:db/retract [:product/id (:product/id product)]
                           :product/name (:product/name product)]]))

(s/defn all-categories :- [model/Category]
  [db]
  (to-entity (d/q '[:find [(pull ?category [*]) ...]
                    :where [?category :category/id]]
                  db)))

(defn all-products-and-categories [db]
  (d/q '[:find ?product-name ?category-name
         :keys product category
         :where [?product :product/name ?product-name]
         [?product :product/category ?category]
         [?category :category/name ?category-name]]
       db))

(defn all-products-by-category [db category-name]
  (d/q '[:find (pull ?product [:product/name :product/slug {:product/category [:category/name]}])
         :in $ ?category-name
         :where [?category :category/name ?category-name]
         [?product :product/category ?category]]
       db category-name))

(defn all-products-by-category [db category-name]
  (d/q '[:find (pull ?category [:category/name {:product/_category [:product/name :product/slug]}])
         :in $ ?category-name
         :where [?category :category/name ?category-name]]
       db category-name))

(defn products-summary [db]
  (d/q '[:find (min ?price) (max ?price) (count ?price) (sum ?price)
         :keys min max quantity amount
         :with ?product
         :where [?product :product/price ?price]]
       db))

(defn products-summary-by-category [db]
  (d/q '[:find ?category-name (min ?price) (max ?price) (count ?price) (sum ?price)
         :keys category min max quantity amount
         :with ?product
         :where [?product :product/price ?price]
         [?product :product/category ?category]
         [?category :category/name ?category-name]]
       db))

(defn product-most-expensive [db]
  (d/q '[:find (pull ?product [*])
         :where [(q '[:find (max ?price)
                      :where [_ :product/price ?price]] $) [[?price]]]
         [?product :product/price ?price]]
       db))

(s/defn products-by-categories :- [model/Product]
  [db
   categories :- [s/Str]
   digital? :- s/Bool]
  (to-entity (d/q '[:find [(pull ?product [* {:product/category [*]}]) ...]
                    :in $ [?category-name ...] ?is-digital?
                    :where
                    [?category :category/name ?category-name]
                    [?product :product/category ?category]
                    [?product :product/digital ?is-digital?]]
                  db categories digital?)))