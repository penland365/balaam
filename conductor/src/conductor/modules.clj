(ns conductor.modules
  (:require [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [conductor.docker :as docker]
            [conductor.util :as util])
  (:gen-class))

(def api-container-name  "penland365/balaam")
(def api-http-port       (env :api-http-port))
(def google-api-key      (env :google-api-key))
(def darksky-api-key     (env :darksky-api-key))
(def slack-client-id     (env :slack-client-id))
(def slack-client-secret (env :slack-client-secret))
(def slack-redirect-uri  (env :slack-redirect-uri))
(def database            (env :database))
(def database-port       (env :database-port))
(def database-username   (env :database-username))
(def database-password   (env :database-password))

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

(def api-env [
               (str "HTTP_PORT=" api-http-port)
               (str "GOOGLE_API_KEY=" google-api-key)
               (str "DARKSKY_API_KEY=" darksky-api-key)
               "DATABASE_HOSTNAME=172.17.0.1" 
               (str "DATABASE_PORT=" database-port)
               (str "DATABASE_USERNAME=" database-username)
               (str "DATABASE_PASSWORD=" database-password)
               (str "DATABASE=" database)
               (str "SLACK_CLIENT_ID=" slack-client-id)
               (str "SLACK_CLIENT_SECRET=" slack-client-secret)
               (str "SLACK_REDIRECT_URI=" slack-redirect-uri)])

(defn- api-container-body [api-version]
  (log/info "THE api-http-port IS " api-http-port)
  (json/write-str {:Env api-env
                   :ExposedPorts {
                     (str api-http-port "/tcp") {}
                   }
                   :HostConfig { 
                     :PortBindings { 
                       (str api-http-port "/tcp") [
                         {
                           :HostPort api-http-port 
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

