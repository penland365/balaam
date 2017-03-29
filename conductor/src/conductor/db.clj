(ns conductor.db
  (:require [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]]
            [conductor.docker :as docker]
            [conductor.util :as util])
  (:gen-class))

(defn- active-db? [containers db-version]
  (let [active-dbs (filter (fn [x]
                             (= (str "penland365/balaam-db:" db-version) (get x "Image")))
                           containers)]
    (= 1 (count active-dbs))))

(defn- active-db [containers db-version]
  (let [active-dbs (filter (fn [x]
                             (= (str "penland365/balaam-db:" db-version) (get x "Image")))
                           containers)]
    (first active-dbs)))

(def db-env
  ["POSTGRES_PASSWORD=buniversity1"
   "POSTGRES_DB=balaam"
   "POSTGRES_USER=moses"])

(defn- blank-db! [db-version]
  "Ensures that there is no Docker container currently on on the daemon running
   the provided image"
  (let [containers (docker/ls)]
    (when (active-db? containers db-version)
      (let [container (active-db containers db-version)]
        (docker/stop (get container "Id"))
        (docker/rm   (get container "Id"))))))

(defn- container-body [db-version]
  (json/write-str {:Env db-env
                   :HostConfig { 
                     :PortBindings { 
                       "5432/tcp" [
                         {
                           :HostPort "4501"
                           ;;:HostIp   "0.0.0.0"
                         }
                       ]
                     }
                    } 
                   :Image (str "penland365/balaam-db:" db-version)}))

(defn- start-db! [db-version]
  (let [container (docker/create (container-body db-version))] 
    (docker/start (get container "Id"))
    (get container "Id")))

(defn run-db! [options]
  (blank-db! (:db-version options))
  (util/stream-logs! (start-db! (:db-version options))))
