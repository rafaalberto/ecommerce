(ns ecommerce.model)

(defn new-product [id name slug price]
  {:product/id    id
   :product/name  name
   :product/slug  slug
   :product/price price})