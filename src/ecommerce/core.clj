(ns ecommerce.core
  (:use [clojure pprint])
  (:require [datomic.api :as d]
            [ecommerce.database :as db]
            [ecommerce.model :as model]
            [schema.core :as s])
  (:import (java.util UUID)))

(s/set-fn-validation! true)

(db/delete-database!)

(def connection (db/open-connection!))

(db/create-schema! connection)

(defn uuid [] (UUID/randomUUID))

;create categories
(def electronics (model/new-category (uuid) "Electronics"))
(def sports (model/new-category (uuid) "Sports"))

(db/add-categories! connection [electronics sports])

(pprint (db/all-categories (d/db connection)))

;create products
(let [computer (model/new-product (uuid) "Computer" "new_computer" 2500.00M (:category/id electronics) 0)
      smartphone (model/new-product (uuid) "Smartphone" "new_smart" 1400.00M (:category/id electronics) 10)
      ball (model/new-product (uuid) "Ball" "new_ball" 50.00M (:category/id sports) 0)]
  (db/add-products! connection [computer smartphone ball]))

(def mouse (assoc
             (model/new-product (uuid) "Mouse" "new_mouse" 70.00M (:category/id electronics) 15)
             :product/digital true))

(db/add-products! connection [mouse])

(pprint (db/one-product! (d/db connection) (:product/id mouse)))
;(pprint (db/one-product! (d/db connection) (uuid)))

(println "update data")

(def product-to-update {:id     (:product/id mouse)
                        :fields [{:product/name "Mouse2"}
                                 {:product/slug "mouse2fff"}
                                 {:product/price 120.00M}
                                 {:product/category [:category/id (:category/id electronics)]}]})

(db/update-product! connection product-to-update)

(println "Delete data")
(db/delete-products! connection mouse)

;read
(println "Current data")
(pprint (db/all-products (d/db connection)))

;as-of
(println "Former data")
(pprint (db/all-products
          (d/as-of (d/db connection) #inst "2022-04-07T21:24:44.250")))

(pprint (db/all-products-by-minimum-price (d/db connection) 1000.00M))

(pprint (db/all-products-by-keyword (d/db connection) "smart"))

(pprint (db/all-products-and-categories (d/db connection)))

(pprint (db/all-products-by-category (d/db connection) "Electronics"))

(pprint (db/products-summary (d/db connection)))

(pprint (db/products-summary-by-category (d/db connection)))

(pprint (db/product-most-expensive (d/db connection)))

(pprint (db/all-products-available (d/db connection)))

(def products-available (db/all-products-available (d/db connection)))
(db/one-product-available (d/db connection) (:product/id (first products-available)))

(pprint (db/all-products-available-with-rules (d/db connection)))

(def product-available (db/one-product-available-with-rule (d/db connection)
                                                           (:product/id (second products-available))))
(pprint product-available)

(pprint (db/products-by-categories (d/db connection) ["Electronics"] true))