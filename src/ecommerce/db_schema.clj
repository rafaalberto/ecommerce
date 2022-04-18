(ns ecommerce.db-schema)

(def product-schema [{:db/ident       :product/name
                      :db/valueType   :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/doc         "Product name"}
                     {:db/ident       :product/slug
                      :db/valueType   :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/doc         "Path to access product by HTTP"}
                     {:db/ident       :product/price
                      :db/valueType   :db.type/bigdec
                      :db/cardinality :db.cardinality/one
                      :db/doc         "Product price"}
                     {:db/ident       :product/keyword
                      :db/valueType   :db.type/string
                      :db/cardinality :db.cardinality/many
                      :db/doc         "Keywords"}
                     {:db/ident       :product/id
                      :db/valueType   :db.type/uuid
                      :db/cardinality :db.cardinality/one
                      :db/unique      :db.unique/identity
                      :db/doc         "Product ID"}
                     {:db/ident       :product/category
                      :db/valueType   :db.type/ref
                      :db/cardinality :db.cardinality/one
                      :db/doc         "Category ID"}
                     {:db/ident       :product/quantity
                      :db/valueType   :db.type/long
                      :db/cardinality :db.cardinality/one
                      :db/doc         "Quantity of product"}
                     {:db/ident       :product/digital
                      :db/valueType   :db.type/boolean
                      :db/cardinality :db.cardinality/one
                      :db/doc         "Is it digital?"}])

(def category-schema [{:db/ident       :category/name
                       :db/valueType   :db.type/string
                       :db/cardinality :db.cardinality/one}
                      {:db/ident       :category/id
                       :db/valueType   :db.type/uuid
                       :db/cardinality :db.cardinality/one
                       :db/unique      :db.unique/identity}])

(def schema (flatten (conj []
                           product-schema
                           category-schema)))