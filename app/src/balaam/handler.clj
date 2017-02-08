(ns balaam.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.tools.logging :as log]
            [ring.middleware.json :refer [wrap-json-params wrap-json-body wrap-json-response]]
            [ring.middleware.logger :as logger]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [balaam.tmux :as tmux]))

(defroutes app-routes
  (POST "/weather" request (tmux/weather (get request :body)))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
    (wrap-json-body)
    (wrap-json-response)
    (logger/wrap-with-logger)
    (wrap-defaults api-defaults)))
