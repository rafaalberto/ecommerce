(ns ecommerce.db.config
  (:require [datomic.api :as d]
            [ecommerce.db.schema :refer [schema]])
  (:use [clojure pprint]))

(def db-uri "datomic:dev://localhost:4334/ecommerce")

(defn open-connection! []
  (d/create-database db-uri)
  (d/connect db-uri))

(defn delete-database! []
  (d/delete-database db-uri))

(defn create-schema! [connection]
  (d/transact connection schema))
