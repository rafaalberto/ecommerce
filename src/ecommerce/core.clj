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
(d/transact connection [[:db/add 17592186045419 :product/price 1780.00M]])

;delete
(d/transact connection [[:db/retract 17592186045418 :product/name "Computer"]])

;read
(pprint (db/all-products (d/db connection)))

(db/delete-database)
