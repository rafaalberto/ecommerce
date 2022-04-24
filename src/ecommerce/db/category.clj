(ns ecommerce.db.category
  (:require [datomic.api :as d]
            [ecommerce.db.entity :as db.entity]
            [ecommerce.db.model :as db.model]
            [schema.core :as s]))

(s/defn add!
  [connection categories :- [db.model/Category]]
  (d/transact connection categories))

(s/defn find-all :- [db.model/Category]
  [db]
  (db.entity/to-entity (d/q '[:find [(pull ?category [*]) ...]
                              :where [?category :category/id]]
                            db)))