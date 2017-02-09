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
            [balaam.auth :as auth])
  (:import [java.security SecureRandom]
           [java.util Base64]))

(defn- post-user [user]
  (let [password        (get user "password")
        password-length (count password)]
    (cond
      (< password-length 7) {:status 400 :body {:reason "Password must be at least 7 characters"}}
      :else
        (let [salt      (auth/salt 71)
              encrypted (auth/encrypt password salt)
              result    (db/wait-insert-user (get user "username") encrypted salt)
              error?    (instance? Throwable result)]
          (cond
            (not error?) {:status 201}
            :else {:status 400 :body {:reason (str "username " (get user "username") " exists")}})))))

(defn get-slack [user]
  (log/info user)
  {:status 200})

(defroutes app-routes
  (GET  "/data/slack" {:keys [headers params body] :as request} (auth/authorize request get-slack))
  (POST "/weather" request (tmux/weather (get request :body)))
  (POST "/users"  request (post-user (get request :body)))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
    (wrap-json-body)
    (wrap-json-response)
    (logger/wrap-with-logger)
    (wrap-defaults api-defaults)))
