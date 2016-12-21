(ns balaam.tmux
  (:require [balaam.clients.google :as google]
            [balaam.clients.darksky :as darksky]
            [clojure.tools.logging :as log]
            [clojure.core.cache :as cache])
  (:gen-class))

(def cache(atom(cache/ttl-cache-factory {} :ttl 120000)))

(defn- hit-or-miss [c k v]
  (if (cache/has? c k)
    (cache/hit c k)
    (cache/miss c k v)))

(defn- fresh-weather [google-api-key darksky-api-key]
  (log/info "Fetching fresh-weather")
  (let [x (darksky/weather (google/location google-api-key) darksky-api-key)]
    (format "%s %s" (get x :temperature) (get x :icon))))

(defn- cached-weather [google-api-key darksky-api-key]
  (swap! cache hit-or-miss :weather (delay(#(fresh-weather google-api-key darksky-api-key)))))

(defn weather [google-api-key darksky-api-key]
  (get (cached-weather google-api-key darksky-api-key) :weather))
