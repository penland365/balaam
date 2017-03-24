(ns balaam.graphql.handler
  (:require
    [balaam.graphql.schema :refer [balaam-schema]]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [clojure.data.json :as json]
    [clojure.string :as str]
    [com.walmartlabs.lacinia :refer [execute]]
    [com.walmartlabs.lacinia.schema :as schema]
    [com.walmartlabs.lacinia.util :refer [attach-resolvers]]))

(defn variable-map
  "Reads the `variables` query parameter, which contains a JSON string
  for any and all GraphQL variables to be associated with this request.
  Returns a map of the variables (using keyword keys)."
  [request]
  (let [vars (get-in request [:query-params :variables])]
    (if-not (str/blank? vars)
      (json/read-str vars :key-fn keyword)
      {})))

(defn extract-query
  [request]
  (case (:request-method request)
    :get (get-in request [:query-params :query])
    :post (slurp (:body request))
    :else ""))

(defn- graphql-handler
  "Accepts a GraphQL query via GET or POST, and executes the query.
  Returns the result as text/json."
  [compiled-schema request]
  (let [context {:cache (atom {})}]
    (let [vars (variable-map request)
          query (extract-query request)
          result (execute compiled-schema query vars context)
          status (if (-> result :errors seq)
                   400
                   200)]
      {:status status
       :headers {"Content-Type" "application/json"}
       :body (json/write-str result)})))

(defn get-graphql [request]
  (graphql-handler (balaam-schema) request))

(defn post-graphql [request]
  (graphql-handler (balaam-schema) request))

