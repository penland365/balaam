(ns balaam.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [balaam.tmux :as tmux]))

(defroutes app-routes
  (GET "/tmux/weather" [] (tmux/weather))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
