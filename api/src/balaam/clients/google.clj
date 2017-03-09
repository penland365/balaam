(ns balaam.clients.google
  (:require [balaam.util :as util]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [cheshire.core :refer :all]
            [environ.core :refer [env]])
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:gen-class))

(def google-api-key
    (env :google-api-key))

(defn ap [wifi]
  (hash-map
    :macAddress         (:mac_address wifi)
    :age                (:age wifi)
    :channel            (:channel wifi)
    :signalStrength     (:signal_strength wifi)
    :signalToNoiseRatio (:signal_to_noise_ratio wifi)))

(defn- request [wifis]
  {
    :query-params { :key google-api-key }
    :content-type :json
    :accept :json
    :form-params {
      :considerIp :false
      :wifiAccessPoints wifis
    }})

(defn- reverse-geocode [lat lng]
  (try+
    (client/post "https://maps.googleapis.com/maps/api/geocode/json" 
                 {:accept :json
                  :query-params {:latlng (format "%s,%s" lat lng)
                                 :key google-api-key }
                  :content-type :json}
                 {:throw-entire-message? true})
    (catch {:status 400} {:keys [body]}
      (log/error body))
    (catch {:status 404} {:keys [body]}
      (log/error body))
    (catch Object _
      (prn (:throwable &throw-context) "unexpected error")
      (log/error (:throwable &throw-context) "unexpected error")
      (throw+))))

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

(defn- parse-locality [json]
  "Parse the short name from the locality address field in the Google reverse Geocoding
   response. See https://developers.google.com/maps/documentation/geocoding/start for more
   information."
  (let [comps (:address_components (first (:results json)))
        xs    (filter #(util/in? (:types %) "locality") comps)]
    (:short_name (first (doall xs)))))

(defrecord Location [lat lng locale])

(defn location [wifis]
  (log/info "GETTING LOCATION")
  (let [xs     (doall (map ap wifis))
        req    (request xs)
        resp   (post-location req)
        loc    (get (parse-string (get resp :body) true) :location)
        json   (parse-string (:body (reverse-geocode (:lat loc) (:lng loc))) true)
        locale (parse-locality json)]
    (Location. (:lat loc) (:lng loc) locale)))

(defn get-locale [lat lng]
  (log/info "GETTING LOCALE")
  (let [resp (reverse-geocode lat lng)
        json (parse-string (:body resp) true)
        locale (parse-locality json)]
    (Location. lat lng locale)))

