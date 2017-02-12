(ns balaam.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.tools.logging :as log]
            [ring.middleware.json :refer [wrap-json-params wrap-json-body wrap-json-response]]
            [ring.middleware.logger :as logger]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [clojure.string :as str]
            [balaam.tmux :as tmux]
            [balaam.postgres :as db]
            [balaam.clients.slack :as slack]
            [balaam.resources.data :as data]
            [balaam.resources.github :as gh]
            [balaam.resources.users :as users]
            [balaam.util :as u]
            [balaam.auth :as auth])
  (:import [java.security SecureRandom]
           [java.util Base64]))

(defn- post-user [user]
  (let [password        (get user "password")
        password-length (count password)]
    (cond
      (< password-length 7) {:status 400 :body {:reason "Password must be at least 7 characters"}}
      :else
        (let [salt      (u/salt 71)
              encrypted (auth/encrypt password salt)
              result    (db/wait-insert-user (get user "username") encrypted salt)
              error?    (instance? Throwable result)]
          (cond
            (not error?) {:status 201}
            :else {:status 400 :body {:reason (str "username " (get user "username") " exists")}})))))

(defn- test-route [request]
  (log/info request)
  {:status 201})

(defn namespace-then-auth [request handler]
  (auth/user-namespace-exists? request auth/namespace-auth handler))

(def authorize-then-respond (comp tmux/get-slack auth/authorize))

(defroutes app-routes
  (GET "/data/weather" request (auth/authorize request tmux/get-weather (:headers request) (:params request)))
  (GET "/data/slack" request (auth/authorize request tmux/get-slack (:headers request))) 
  (GET "/data/github" request(auth/authorize request data/github (:headers request) (:params request)))

  (GET "/users" [] users/index-users)
  (GET "/users/:id" [id] (users/show-user id))
  (POST "/users"  request (post-user (get request :body)))

  (GET "/redirects/slack" request (slack/redirect (get request :params)))
  (GET "/:username/slack/auth"  {:keys [headers username] :as request}
        (namespace-then-auth request slack/get-auth))

  (GET "/callbacks/github" request (gh/register-callback (:params request)))
  (GET "/:username/github/auth" {:keys [headers username] :as request}
       (namespace-then-auth request gh/get-auth))

  (route/not-found "Not Found"))

(def app
  (-> app-routes
    (wrap-json-body)
    (wrap-json-response)
    (logger/wrap-with-logger)
    (wrap-defaults api-defaults)))
