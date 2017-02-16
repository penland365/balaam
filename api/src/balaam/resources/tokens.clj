(ns balaam.resources.tokens
  (:require [balaam.postgres :as db]
            [cheshire.core :refer :all]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn idx-slack-tokens [user args]
  "Lists all Slack tokens for a user"
  (let [db-tokens (db/select-slack-tokens-by-uid (:id user))
        tokens    (map #(clojure.set/rename-keys % {:inserted_at :created_at}) db-tokens)]
  {:status 200 :body tokens}))
