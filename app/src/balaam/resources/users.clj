(ns balaam.resources.users
  (:require [balaam.auth :as auth]
            [balaam.postgres :as db]
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

(defn- short-passwd? [pword]
  (< (count pword) 7))

(def short-passwd-resp {:status 400 :body {:reason "Password must be at least 7 characters"}})

(defn post-user [user]
  (let [password        (get user "password")]
    (if (short-passwd? password)
      (short-passwd-resp)
      (let [salt      (u/salt 71)
            encrypted (auth/encrypt password salt)
            result    (db/wait-insert-user (get user "username") encrypted salt)]
        (if (instance? Throwable result)
          {:status 400 :body {:reason (str "username " (get user "username") " exists")}}
          {:status 201})))))

(defn patch-user [user args]
  (let [pword (get (first args) "password")]
    (cond
      (nil? pword)                  {:status 400 :body {:error "Password required to Patch user."}}
      (true? (short-passwd? pword)) {:status 400 :body {:error "Password must be at least 7 characters."}}
      :else
        (let [salt      (u/salt 71)
              encrypted (auth/encrypt pword salt)
              results   (db/update-user (:id user) encrypted salt)]
          (if (instance? Throwable results)
            {:status 500 :body {:postgresql_error_code (.getCode results)
                                :url "https://www.postgresql.org/docs/9.6/static/errcodes-appendix.html"}}
            {:status 204})))))
