(ns ecommerce.core
  (:use [clojure pprint])
  (:require [datomic.api :as d]
            [ecommerce.database :as db]
            [ecommerce.model :as model])
  (:import (java.util UUID)))

(def connection (db/open-connection!))

(db/create-schema! connection)

(defn uuid [] (UUID/randomUUID))

;create categories
(def electronics (model/new-category (uuid) "Electronics"))
(def sports (model/new-category (uuid) "Sports"))

(db/add-categories! connection [electronics sports])

(pprint (db/all-categories (d/db connection)))

;create products
(let [computer (model/new-product (uuid) "Computer" "new_computer" 2500.00M (:category/id electronics))
      smartphone (model/new-product (uuid) "Smartphone" "new_smart" 1400.00M (:category/id electronics))
      ball (model/new-product (uuid) "Ball" "new_ball" 50.00M (:category/id ball))]
  (db/add-products! connection [computer smartphone keyboard]))

(def mouse (model/new-product (uuid) "Mouse" "new_mouse" 70.00M (:category/id electronics)))

(db/add-products! connection [mouse])

(pprint (db/find-product-by-id (d/db connection) (:product/id mouse)))

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

(db/delete-database!)
