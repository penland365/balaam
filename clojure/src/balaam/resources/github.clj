(ns balaam.resources.github
  (:require [balaam.postgres :as db]
            [balaam.util :as u]
            [balaam.clients.github :as ghub]
            [cheshire.core :refer :all]
            [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]])
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
              url    (u/build-endpoint params auth-url)]
          {:status 200 :body {:url url}}))))

(defn- redirect-error-html []
  (str "<h1>Balaam: an error has occurred during Github OAuth.</h1>"))
(defn- redirect-success-html [username]
  (str "<h1>Balaam thanks you " username "!</h1><h2>Github integration had been enabled.</h2>"))

(defn register-callback [params]
  (let [results (db/select-pending-gh-token-by-state (:state params))]
    (cond
      (not= (count results) 1) (redirect-error-html)
      :else 
        (let [resp     (ghub/post-access-token (:code params) (:state params))
              gh-token (parse-string (:body resp) true)
              update-result (db/update-gh-token (:id (first results)) gh-token)]
          (cond
            (not= (:updated update-result) 1) (redirect-error-html)
            :else
              (let [users (db/select-user-by-id (:user_id (first results)))]
                (redirect-success-html (:username (first users)))))))))
