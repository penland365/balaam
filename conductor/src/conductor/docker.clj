(ns conductor.docker
  (:require [clojure.core.async :as async]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.java.io :refer [make-output-stream]]
            [clj-http.client :as client]
            [org.httpkit.client :as http])
  (:import [java.io DataInputStream])
  (:gen-class))

(defn die [reason]
  (println reason)
  (java.lang.System/exit 1))

(defn info []
  (let [{:keys [body error] :as resp} @(http/get "http://127.0.0.1:2376/version")]
    (when error
      (die (str "Docker GET /version error. " error)))
    body))

(defn api-version[]
  "Gets and then parses the Docker Version from the docker daemon"
  "1.27")
  ;;(get (json/read-str (info)) "ApiVersion"))

(defn- build-url [api-version endpoint]
  (str "http://127.0.0.1:2376/v" api-version endpoint))

(defn ls []
  "Lists all docker containers. docker ls
   See https://docs.docker.com/engine/api/v1.26/#tag/Container for more information" 
  (let [resp @(http/get (str "http://127.0.0.1:2376/v" (api-version) "/containers/json"))]
    (json/read-str (:body resp))))

(defn create [body]
  "Creates a Docker container. docker create ..... 
   See https://docs.docker.com/engine/api/v1.26/#operation/ContainerCreate for 
   more information."
  (let [resp @(http/post (build-url (api-version) "/containers/create") 
                         {:headers {"Content-Type" "application/json"
                                    "Accept"       "application/json"}
                          :body body
                          :debug true})]
    (log/info resp)
    (when (not= (:status resp) 201)
      (prn resp))
      ;;(die (str "Error creating Container " body ", HTTP status " (:status resp))))
    (json/read-str (:body resp))))

(defn stop [id]
  "Stops a Docker container. docker stop ID
   See https://docs.docker.com/engine/api/v1.26/#operation/ContainerStop for 
   more information."
  (let [resp @(http/post (build-url (api-version) (str "/containers/" id "/stop"))
                         ;;The param t is the number of seconds to wait before 
                         ;; Docker kills the container
                         {:query-params {:t 10}})]
    (when (or (= (:status resp) "204") (= (:status resp) "304"))
      (die (str "Error stopping " id " , HTTP status code " (:status resp))))))

(defn start [id]
  "Starts a Docker container. docker start ID
   See https://docs.docker.com/engine/api/v1.26/#operation/ContainerStart for
   more information."
  (let [resp @(http/post (build-url (api-version) (str "/containers/" id "/start")))]
    (when (or (= (:status resp) "204") (= (:status resp) "304"))
      (die (str "Error starting container " id ", HTTP status " (:status resp))))))

(defn rm [id]
  "Removes a Docker container. docker rm ID
   See https://docs.docker.com/engine/api/v1.26/#operation/ContainerDelete for 
   more information."
  (let [resp @(http/delete (build-url (api-version) (str "/containers/" id)))]
    (when (not= (:status resp) 204)
      (die (str "Error removing container with id " id ", HTTP status " (:status resp))))))

(defn logs [id]
  "Attaches to a container and then returns all log files as a stream.
   See https://docs.docker.com/engine/api/v1.26/#operation/ContainerTop for more
   information."
  (http/get (build-url (api-version) (str "/containers/" id "/logs"))
            {:query-params {:stdout     :true
                            :stderr     :true
                            :follow     true
                            :timestamps true}}
            :as :stream))

(defn stream-logs! [id out-chan]
  "Attaches to a container and then returns all log files as a stream.
   See https://docs.docker.com/engine/api/v1.27/#operation/ContainerTop for more
   information."
  (let [url (build-url (api-version) (str "/containers/" id "/logs"))]
    (with-open [stream (DataInputStream. (:body (client/get url {:query-params {:stdout true
                                                                                :stderr true
                                                                                :follow true
                                                                                :timestamps true}
                                                                 :as :stream})))] 
      (loop []
        (let [next-byte (.readByte stream)]
          (when-not (= -1 next-byte)
            (async/>!! out-chan next-byte)
            (recur)))))))
