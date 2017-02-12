(ns balaam.clients.github
  (:require [balaam.postgres :as db]
            [balaam.util :as u]
            [cheshire.core :refer :all]
            [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]])
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:import [java.net URLEncoder])
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
        (let [encoded-state (URLEncoder/encode state "UTF-8")
              params        (conj base-auth-params {:k "state" :v encoded-state})
              url           (u/build-endpoint params auth-url)]
          {:status 200 :body {:url url}}))))

(def access-token-url "https://github.com/login/oauth/access_token")
(def base-at-params [{:k "client_id" :v client-id} {:k "client_secret" :v client-secret}])

(defn post-access-token [code state]
  (let [params (conj base-at-params {:k "code" :v code} {:k "state" :v state})]
  (try+
    (client/post "https://github.com/login/oauth/access_token" 
                 {:accept  :json
                  :content :json
                  :query-params {:client_id     client-id
                                 :client_secret client-secret
                                 :code          code
                                 :state         state}}
                 {:throw-entire-message? true})
    (catch {:status 400} {:keys [body]}
      (log/error body))
    (catch {:status 404} {:keys [body]}
      (log/error body))
    (catch Object _
      (log/error (:throwable &throw-context) "unexpected error")
      (throw+)))))

(defn get-notifications [access-token]
  (let [resp (client/get "https://api.github.com/notifications" 
                         {:accept "application/vnd.github.v3+json"
                          :oauth-token access-token})
        xs   (parse-string (:body resp) true)]
    xs))

(defn get-branch-status [access-token owner repo branch]
  (let [url  (str "https://api.github.com/repos/" owner "/" repo "/commits/" branch "/statuses")
        resp (client/get url {:accept "application/vnd.github.v3+json"
                              :oauth-token access-token})
        xs   (parse-string (:body resp) true)]
    xs))
