(ns ecommerce.model)

(defn new-product [id name slug price category-id]
  {:product/id       id
   :product/name     name
   :product/slug     slug
   :product/price    price
   :product/category [:category/id category-id]})

(defn new-category [id name]
  {:category/id   id
   :category/name name})