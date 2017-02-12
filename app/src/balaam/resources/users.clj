(ns balaam.resources.users
  (:require [balaam.postgres :as db]
            [balaam.util :as u]
            [cheshire.core :refer :all]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn index-users [x]
  "Lists all users"
  {:status 200 :body (db/select-users)})

(defn show-user [id]
  "Shows a user with an id or username"
  (log/info id)
  (if (number? id)
    (let [results (db/select-external-user-by-id id)
          error?  (instance? Throwable results)]
      (cond
        (true? error?)   {:status 400}
        (empty? results) {:status 404 :body {:error (str "No user with id " id " found.")}}
        :else {:status 200 :body (first results)}))
    (let [results (db/select-external-user-by-username id)
          error?  (instance? Throwable results)]
      (cond
        (true? error?)   {:status 400}
        (empty? results) {:status 404 :body {:error (str "No user with username " id " found.")}}
        :else {:status 200 :body (first results)}))))
