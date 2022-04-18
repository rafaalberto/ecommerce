(ns ecommerce.model
  (:require [schema.core :as s])
  (:import (java.util UUID)))

(def Category
  {:category/id   UUID
   :category/name s/Str})

(def CategoryID [(s/one s/Keyword "")
                 (s/one UUID "")])

(def Product
  {:product/id                        UUID
   :product/name                      s/Str
   :product/slug                      s/Str
   :product/price                     BigDecimal
   (s/optional-key :product/category) Category
   (s/optional-key :product/quantity) s/Int
   (s/optional-key :product/digital)  s/Bool})

(defn new-product [id name slug price category-id quantity]
  {:product/id       id
   :product/name     name
   :product/slug     slug
   :product/price    price
   :product/category [:category/id category-id]
   :product/quantity quantity
   :product/digital  false})

(defn new-category [id name]
  {:category/id   id
   :category/name name})