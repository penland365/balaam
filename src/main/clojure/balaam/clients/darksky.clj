(ns balaam.clients.darksky
  (:require [clj-http.client :as client])
  (:require [cheshire.core :refer :all])
  (:require [clojure.string :as str])
  (:gen-class))

(defn- endpoint [location, api-key]
  (format "https://api.darksky.net/forecast/%s/%s,%s" api-key (get location :lat) (get location :lng))
)

(defn- weather-response [location, api-key]
  (client/get (endpoint location api-key))
)

(defn- weather-json [response]
  (parse-string (get response :body) true)
)

(defn- currently [weather-json]
  (get weather-json :currently)
)

(defn- weather-icon [icon]
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

(defn weather [location, api-key]
  (let [now (currently (weather-json (weather-response location, api-key)))]
    {:temperature (get now :temperature) :icon (weather-icon (get now :icon))}))
