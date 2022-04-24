(ns ecommerce.core
  (:use [clojure pprint])
  (:require [datomic.api :as d]
            [ecommerce.db.config :as db.config]
            [ecommerce.db.product :as db.product]
            [ecommerce.db.category :as db.category]
            [ecommerce.db.model :as db.model]
            [schema.core :as s])
  (:import (java.util UUID)))

(s/set-fn-validation! true)

(db.config/delete-database!)

(def connection (db.config/open-connection!))

(db.config/create-schema! connection)

(defn uuid [] (UUID/randomUUID))

(def electronics (db.model/new-category (uuid) "Electronics"))
(def sports (db.model/new-category (uuid) "Sports"))

(db.category/add! connection [electronics sports])

(pprint (db.category/find-all (d/db connection)))

(defn create-products-samples []
  (let [computer (db.model/new-product (uuid) "Computer" "new_computer" 2500.00M (:category/id electronics) 0)
        smartphone (db.model/new-product (uuid) "Smartphone" "new_smart" 1400.00M (:category/id electronics) 10)
        ball (db.model/new-product (uuid) "Ball" "new_ball" 50.00M (:category/id sports) 0)]
    (db.product/add! connection [computer smartphone ball])))

(create-products-samples)

(def mouse (assoc
             (db.model/new-product (uuid) "Mouse" "new_mouse" 70.00M (:category/id electronics) 15)
             :product/digital true))

(db.product/add! connection [mouse])

(pprint (db.product/find-one (d/db connection) (:product/id mouse)))

(db.product/delete! connection mouse)

(def first-product (first (db.product/find-all (d/db connection))))
(pprint first-product)

(pprint @(db.product/update-price! connection
                                   (:product/id first-product) 2500.00M 1000.00M))

(def product-to-update {:product/id    (:product/id first-product)
                        :product/price 1030.00M
                        :product/slug  "/computer3"
                        :product/name  "Computer2 Updated"})

(pprint @(db.product/update! connection first-product product-to-update))

(def increment-view
  #db/fn {
          :lang   :clojure
          :params [db product-id]
          :code
          (let [views (d/q '[:find ?views .
                             :in $ ?product-id
                             :where [?product :product/id ?product-id]
                             [?product :product/views ?views]]
                           db product-id)
                current (or views 0)
                updated (inc current)]
            [{:product/id    product-id
              :product/views updated}])})

(pprint @(d/transact connection [{:db/ident :increment-view
                                  :db/fn    increment-view
                                  :db/doc   "Increment view quantity"}]))

(pprint (db.product/view! connection (:product/id first-product)))

(pprint (db.product/find-all (d/db connection)))

(pprint (db.product/find-all
          (d/as-of (d/db connection) #inst "2022-04-07T21:24:44.250")))

(def products-available (db.product/find-all-stock-available (d/db connection)))

(pprint products-available)

(pprint (db.product/find-all-stock-available-rules (d/db connection)))

(pprint (db.product/find-one-stock-available (d/db connection)
                                             (:product/id (first products-available))))

(def product-available (db.product/find-one-stock-available-rule (d/db connection)
                                                                 (:product/id (second products-available))))
(pprint product-available)

(pprint (db.product/find-all-by-minimum-price (d/db connection) 1000.00M))

(pprint (db.product/find-all-by-keyword (d/db connection) "smart"))

(pprint (db.product/fetch-products-and-categories (d/db connection)))

(pprint (db.product/find-all-by-category (d/db connection) "Electronics"))

(pprint (db.product/fetch-summary (d/db connection)))

(pprint (db.product/fetch-summary-by-category (d/db connection)))

(pprint (db.product/fetch-most-expensive (d/db connection)))

(pprint (db.product/find-by-categories-and-digital (d/db connection) ["Electronics"] true))
