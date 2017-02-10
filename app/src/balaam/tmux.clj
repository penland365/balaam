(ns balaam.tmux
  (:require [balaam.clients.google :as google]
            [balaam.clients.darksky :as darksky]
            [balaam.postgres :as db]
            [cheshire.core :refer :all]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import [java.time.temporal ChronoUnit]
           [java.time Instant])
  (:gen-class))

(defn- format-response [xs]
  (let [content (first xs)
        weather (last xs)
        text? (str/starts-with? content "text/plain")]
    (cond
      (true? text?) (format "%s %s" (:temperature weather) (:icon weather))
      :else {:status 200 :body weather})))

(defn- expired? [results]
  (let [x          (first results)
        fixed      (.minus (.toInstant (:last_modified_at x)) 6 ChronoUnit/HOURS) ;; to UTC because OMG SQL Timestamp
        expires-at (.plusSeconds fixed (:seconds_to_cache x))
        now        (Instant/now)]
    (.isAfter now expires-at)))

(defn- get-fresh-weather [wifis]
  (let [location (google/location wifis)]
    (darksky/weather location)))

(defn- refresh-weather [wifis content uid]
  (let [weather (get-fresh-weather wifis)
        _       (db/update-cached-weather uid weather)]
  [content weather]))

(defn- load-weather [wifis content uid]
  (let [weather (get-fresh-weather wifis)
        _       (db/insert-cached-weather uid weather)]
    [content weather]))

(def refresh-and-format (comp format-response refresh-weather))
(def load-and-format    (comp format-response load-weather))

(defn get-weather [user args]
  (let [content (get (first args) "accept")
        wifis   (parse-string (:wifis (last args)) true)
        xs      (db/select-cached-weather (:id user))]
    (cond
      (empty? xs)           (load-and-format wifis content (:id user))
      (true? (expired? xs)) (refresh-and-format wifis content (:id user))
      :else
        (format-response [content (:data (first xs))]))))
