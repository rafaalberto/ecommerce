(ns ecommerce.db.entity
  (:require [clojure.walk :as walk]))

(defn- dissoc-db-id [entities]
  (if (map? entities)
    (dissoc entities :db/id)
    entities))

(defn to-entity [entities]
  (walk/prewalk dissoc-db-id entities))