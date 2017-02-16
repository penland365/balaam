(ns balaam.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.tools.logging :as log]
            [ring.middleware.json :refer [wrap-json-params wrap-json-body wrap-json-response]]
            [ring.middleware.logger :as logger]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [clojure.string :as str]
            [balaam.resources.tokens :as tk]
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

(defn- test-route [request]
  (log/info request)
  {:status 201})

(defn namespace-then-auth [request handler]
  (auth/user-namespace-exists? request auth/namespace-auth handler))

(def authorize-then-respond (comp tmux/get-slack auth/authorize))

(defroutes app-routes
  (GET "/data/weather" request (auth/authorize request data/weather (:headers request) (:params request)))
  (GET "/data/github" request(auth/authorize request data/github (:headers request) (:params request)))
  (GET "/data/slack" request (auth/authorize request tmux/get-slack (:headers request))) 

  (GET "/users" [] users/index-users)
  (GET "/users/:id" [id] (users/show-user id))
  (POST "/users"  request (users/post-user (get request :body)))
  (PATCH "/users" request (auth/authorize request users/patch-user (:body request)))

  (GET "/tokens/slack" request (auth/authorize request tk/idx-slack-tokens))

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
