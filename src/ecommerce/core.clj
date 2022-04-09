(ns ecommerce.core
  (:use [clojure pprint])
  (:require [datomic.api :as d]
            [ecommerce.database :as db]
            [ecommerce.model :as model])
  (:import (java.util UUID)))

(def connection (db/open-connection!))

(db/create-schema! connection)

(defn uuid [] (UUID/randomUUID))

;create
(let [computer (model/new-product (uuid) "Computer" "new_computer" 2500.00M)
      smartphone (model/new-product (uuid) "Smartphone" "new_smart" 1400.00M)
      keyboard (model/new-product (uuid) "Keyboard" "new_keyboard" 200.00M)]
  (db/add-products! connection [computer smartphone keyboard]))

;update
(d/transact connection [[:db/add 17592186045418 :product/name "Desktop"]])

(d/transact connection [[:db/add 17592186045418 :product/keyword "desktop"]
                        [:db/add 17592186045418 :product/keyword "smart"]])

;delete
(d/transact connection [[:db/retract 17592186045420 :product/name "Keyboard"]])

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
