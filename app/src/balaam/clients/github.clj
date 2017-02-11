(ns balaam.clients.github
  (:require [balaam.postgres :as db]
            [balaam.util :as u]
            [cheshire.core :refer :all]
            [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]])
  (:gen-class))

(def client-id
  (env :github-client-id))
(def client-secret
  (env :github-client-secret))

(def auth-url "https://github.com/login/oauth/authorize")
(def scopes "notifications,repo:status")
(def base-auth-params [{:k "scope" :v scopes} {:k "allow_signup" :v false} 
                       {:k "client_id" :v client-id}])

(defn get-auth [user]
  (let [state  (u/salt 71)
        result (db/insert-pending-github-token (:id user) state)
        error? (instance? Throwable result)]
    (cond
      (true? error?) {:status 400 :body {:postgresql_error_code (.getCode result)
                                         :url "https://www.postgresql.org/docs/9.6/static/errcodes-appendix.html"}}
      :else 
        (let [params (conj base-auth-params {:k "state" :v state})
              url    (u/build-endpoint params auth-url)]
          {:status 200 :body {:url url}}))))
