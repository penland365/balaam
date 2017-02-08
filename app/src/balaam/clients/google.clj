(ns balaam.clients.google
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [cheshire.core :refer :all]
            [environ.core :refer [env]])
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:gen-class))

(def google-api-key
    (env :google-api-key))

(defn ap [wifi]
  (hash-map
    :macAddress          (get wifi "mac_address")
    :age                 (get wifi "age")
    :channel             (get wifi "channel")
    :signalStrength      (get wifi "signal_strength")
    :signalToNoiseRation (get wifi "signal_to_noise_ratio")))

(defn- request [wifis]
  {
    :query-params { :key google-api-key }
    :content-type :json
    :accept :json
    :form-params {
      :considerIp :false
      :wifiAccessPoints wifis
    }})

(defn- post-location [req]
  (try+
    (client/post "https://www.googleapis.com/geolocation/v1/geolocate" req {:throw-entire-message? true})
    (catch {:status 400} {:keys [body]}
      (prn body))
    (catch {:status 404} {:keys [body]}
      (prn body))
    (catch Object _
      (prn (:throwable &throw-context) "unexpected error")
      (throw+))))

(defn location [wifis]
  (let [xs   (doall (map ap wifis))
        req  (request xs)
        resp (post-location req)
        loc  (get (parse-string (get resp :body) true) :location)]
    (log/info "Current Location --> " loc)
    loc))

