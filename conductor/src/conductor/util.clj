(ns conductor.util
  (:require [clojure.core.async :as async]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [conductor.docker :as docker]
            [org.httpkit.client :as http])
  (:gen-class))

(defn stream-logs! [id]
  (let [out-chan (async/chan)]
    (async/thread (docker/stream-logs! id out-chan))
    (async/go-loop [current-read-bytes []]
                   (let [[v port] (async/alts! [out-chan (async/timeout 5000)])]
                     (if (= port out-chan)
                       (recur (conj current-read-bytes v))
                       (do
                         (when (seq current-read-bytes)
                           (log/info (String. (byte-array (map byte current-read-bytes)) "UTF-8")))
                         (recur [])))))))
