(ns ecommerce.database
  (:require [datomic.api :as d]))

(def db-uri "datomic:dev://localhost:4334/ecommerce")

(defn open-connection! []
  (d/create-database db-uri)
  (d/connect db-uri))

(defn delete-database! []
  (d/delete-database db-uri))

(def schema [{:db/ident       :product/name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "Product name"}
             {:db/ident       :product/slug
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc         "Path to access product by HTTP"}
             {:db/ident       :product/price
              :db/valueType   :db.type/bigdec
              :db/cardinality :db.cardinality/one
              :db/doc         "Product price"}
             {:db/ident       :product/keyword
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/many
              :db/doc         "Keywords"}
             {:db/ident       :product/id
              :db/valueType   :db.type/uuid
              :db/cardinality :db.cardinality/one
              :db/unique      :db.unique/identity
              :db/doc         "Product ID"}])

(defn create-schema! [connection]
  (d/transact connection schema))

;(defn all-products-pull [db]
;  (d/q '[:find (pull ?product [*])
;         :where [?product :product/name]]
;       db))
;
;(defn all-products [db]
;  (d/q '[:find (pull ?entity [:db/id :product/name :product/price])
;         :where [?entity :product/name]]
;       db))

(defn all-products [db]
  (d/q '[:find ?entity ?id ?name ?price
         :keys transaction-id id name price
         :where [?entity :product/id ?id]
         [?entity :product/name ?name]
         [?entity :product/price ?price]]
       db))

(defn find-product-by-id [db uuid]
  (d/pull db '[*] [:product/id uuid]))

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

(defn- vector-to-map [product]
  (let [fields (:fields product)]
    (reduce (fn [items item] (conj items
                                   [:db/add [:product/id (:id product)]
                                    (first (keys item)) (first (vals item))]))
            []
            fields)))

(defn update-product! [connection product]
  (d/transact connection (vector-to-map product)))

(defn delete-products! [connection product]
  (d/transact connection [[:db/retract [:product/id (:product/id product)]
                           :product/name (:product/name product)]]))
