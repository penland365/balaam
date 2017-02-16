(ns balaam.clients.darksky
  (:require [cheshire.core :refer :all]
            [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]])
  (:gen-class))

(def darksky-api-key
  (env :darksky-api-key))

(defn- get-weather [location]
  (let [lat (:lat location)
        lng (:lng location)
        url (format "https://api.darksky.net/forecast/%s/%s,%s" darksky-api-key lat lng)]
    (client/get url)))

(defn- parse-currently [resp]
  (:currently (parse-string (:body resp) true)))

(defn- get-weather-icon [icon]
  (cond
    (compare icon "clear-day") "☀️"
    (compare icon "clear-night") "🌚"
    (compare icon "rain") "🌧"
    (compare icon "snow") "❄️"
    (compare icon "sleet") "🌨"
    (compare icon "wind") "💨"
    (compare icon "fog") "☁️"
    (compare icon "cloudy") "☁️"
    (compare icon "partly-cloudy-day") "⛅"
    (compare icon "partly-cloudy-night") "☁︎"
    (compare icon "hail") "🌨"
    (compare icon "thunderstorm") "⛈"
    (compare icon "tornado") "🌪"
    :else "🎮"))

(defn weather [location]
  (let [resp (get-weather location)
        now  (parse-currently resp)
        temp (:temperature now)
        icon (get-weather-icon (:icon now))]
    {:temperature temp :icon icon :locale (:locale location)}))
