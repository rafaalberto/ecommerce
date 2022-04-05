(ns ecommerce.core
  (:use [clojure pprint])
  (:require [datomic.api :as d]
            [ecommerce.database :as db]
            [ecommerce.model :as model]))

(def connection (db/open-connection))

(db/create-schema connection)

(let [computer (model/new-product "Computer 2" "new_computer" 2500.00M)]
  (d/transact connection [computer]))


