(ns balaam.clients.google
  (:require [clj-http.client :as client])
  (:require [cheshire.core :refer :all])
  (:gen-class))

(defn- wifi-access-point [wifi]
  (hash-map
    :macAddress     (get wifi :bssid)
    :signalStrength (get wifi :rssi)
    :channel        (get wifi :channel)
    :age 0
  ))

(defn- request [wifis api-key]
  {
    :query-params { :key api-key }
    :content-type :json
    :accept :json
    :form-params {
      :considerIp :true
      :wifiAccessPoints (map wifi-access-point wifis)
    }
  })

(defn- location-response [api-key]
  (client/post "https://www.googleapis.com/geolocation/v1/geolocate" (request [] api-key)))

(defn- parse-location [response]
  (get (parse-string (get response :body) true) :location))

(defn lat-long [api-key]
  (parse-location (location-response api-key))
)
