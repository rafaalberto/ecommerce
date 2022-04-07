(ns ecommerce.core
  (:use [clojure pprint])
  (:require [datomic.api :as d]
            [ecommerce.database :as db]
            [ecommerce.model :as model]))

(def connection (db/open-connection))

(db/create-schema connection)

;create
(let [computer (model/new-product "Computer" "new_computer" 2500.00M)
      smartphone (model/new-product "Smartphone" "new_smart" 1400.00M)
      keyboard (model/new-product "Keyboard" "new_keyboard" 200.00M)]
  (d/transact connection [computer smartphone keyboard]))

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

(db/delete-database)
