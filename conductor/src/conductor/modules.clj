(ns conductor.modules
  (:require [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [conductor.docker :as docker]
            [conductor.util :as util])
  (:gen-class))

(def api-container-name "penland365/balaam")

(defn- present-container? [containers version nm]
  "Checks to see if a container is present on the docker machine. 
   Note that this is status independent."
  (let [container-name    (str nm ":" version)
        loaded-containers (filter (fn [x]
                             (= container-name (get x "Image")))
                           containers)]
    (not (>= 0 (count loaded-containers)))))

(defn- get-api-container [containers api-version]
  (let [container-name    (str api-container-name ":" api-version)
        loaded-containers (filter (fn [x]
                             (= container-name (get x "Image")))
                           containers)]
    (first loaded-containers)))

(defn- blank-api! [api-version]
  "Ensures that there is no Docker container currently on on the daemon running
   the provided image"
  (let [containers (docker/ls)]
    (when (present-container? containers api-version api-container-name)
      (let [container (get-api-container containers api-version)]
        (docker/stop (get container "Id"))
        (docker/rm   (get container "Id"))))))

(def api-http-port "8080")

(def api-env
  ["HTTP_PORT=8080"
   "GOOGLE_API_KEY=AIzaSyADzpGbV67hYNHv2ReFFVuRywqzcznX9K0"
   "DARKSKY_API_KEY=061b1c75227f26d05a389965d4e3cf14" 
   "DATABASE_HOSTNAME=172.17.0.1" 
   "DATABASE_PORT=4501" 
   "DATABASE_USERNAME=moses" 
   "DATABASE_PASSWORD=buniversity1" 
   "DATABASE=balaam" 
   "SLACK_CLIENT_ID=2457831732.139175926980" 
   "SLACK_CLIENT_SECRET=fc3b83d8be1fa65d2f95fdcbaf30033d"
   "SLACK_REDIRECT_URI=http://104.198.135.229/redirects/slack"])

(defn- api-container-body [api-version]
  (json/write-str {:Env api-env
                   :ExposedPorts {
                     "8080/tcp" {}
                   }
                   :HostConfig { 
                     :PortBindings { 
                       "8080/tcp" [
                         {
                           :HostPort "4500"
                           :HostIp   ""
                         }
                       ]
                     }
                    } 
                   :PublishAllPorts true
                   :Image (str api-container-name ":" api-version)}))

(defn- start-api! [api-version]
  (let [container (docker/create (api-container-body api-version))
        id        (get container "Id")] 
    (docker/start id)
    id))

(defn run-api! [options]
  "Runs the API Server. We first ensure that there is no existing API server
   running on docker. After that, we create a new server, start it, then begin
   streaming the logs"
  (blank-api! (:api-version options))
  (util/stream-logs! (start-api! (:api-version options))))
