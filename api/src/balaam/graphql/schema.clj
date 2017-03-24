(ns balaam.graphql.schema
  (:require
    [balaam.postgres :as db]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [clojure.tools.logging :as log]
    [com.walmartlabs.lacinia.schema :as schema]
    [com.walmartlabs.lacinia.util :refer [attach-resolvers]]))

(defn- resolve-viewer
  [context args _value]
  (log/info "The id is --> " (:id args))
  {:id 1
   :username "Luke Skywalker"})

(defn- index-users
  "List all users"
  [context args value]
  (db/select-users))

(defn balaam-schema
  []
  (-> (io/resource "balaam-schema.edn")
      slurp
      edn/read-string
      (attach-resolvers {:resolve-viewer resolve-viewer
                         :resolve-users  index-users})
      schema/compile))
