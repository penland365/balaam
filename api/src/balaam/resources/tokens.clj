(ns balaam.resources.tokens
  (:require [balaam.postgres :as db]
            [cheshire.core :refer :all]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn- rename-inserted-at [m]
  (clojure.set/rename-keys m {:inserted_at :created_at}))

(defn idx-slack-tokens [user args]
  "Lists all Slack tokens for a user"
  (let [db-tokens (db/select-slack-tokens-by-uid (:id user))
        tokens    (map #(rename-inserted-at %) db-tokens)]
  {:status 200 :body tokens}))

(defn show-slack-token [user args]
  "Show Slack token with provided id"
  (let [result (db/select-slack-token-by-id-uid (:id user) (first args))
        error? (instance? Throwable result)]
    (cond
      (true? error?) {:status 400 :body {:postgresql_error_code (.getCode result)
                                         :url "https://www.postgresql.org/docs/9.6/static/errcodes-appendix.html"}}
      (empty? result) {:status 404}
      :else {:status 200 :body (rename-inserted-at (first result))})))
