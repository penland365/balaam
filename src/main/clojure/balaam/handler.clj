(ns balaam.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [balaam.tmux :as tmux]))

(defroutes app-routes
  (GET "/tmux/weather/:g-key/:ds-key" [g-key ds-key] (tmux/weather g-key ds-key))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
