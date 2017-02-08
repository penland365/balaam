(ns balaam.tmux
  (:require [balaam.clients.google :as google]
            [balaam.clients.darksky :as darksky]
            [clojure.core.cache :as cache]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]])
  (:gen-class))

(def cache(atom(cache/ttl-cache-factory {} :ttl 120000)))

(defn- hit-or-miss [c k v]
  (if (cache/has? c k)
    (cache/hit c k)
    (cache/miss c k v)))

(defn- fresh-weather [wifis]
  (log/info "Fetching fresh-weather")
  (let [location (google/location wifis)
        weather  (darksky/weather location)]
    (log/info "Caching Weather -------------> " weather)
    (format "%s %s" (:temperature weather) (:icon weather))))

(defn- cached-weather [wifis]
  (swap! cache hit-or-miss :weather (delay(#(fresh-weather wifis)))))

(defn weather [wifis]
  (let [x (cached-weather wifis)]
    (:weather x)))
