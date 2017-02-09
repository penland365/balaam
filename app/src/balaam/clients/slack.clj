(ns balaam.clients.slack
  (:require [cheshire.core :refer :all]
            [clj-http.client :as client]
            [clojure.core.reducers :as r]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]])
  (:gen-class))

(def slack-client-id
  "2457831732.139175926980")
(def scopes
  "identify,channels:history,channels:read")
(def redirect-uri
  "http://104.198.135.229")

(defn- auth-req-params [user-id]
  [
    {:k "client_id"    :v slack-client-id}
    {:k "scope"        :v scopes}
    {:k "redirect_uri" :v redirect-uri}
    {:k "state"        :v user-id}])

(defn- param+ [param]
  "Takes a map of param key and value, combining them"
  (str (:k param) "=" (:v param)))

(defn- query+ 
  "A function to be folded into a list of param pairs to build a query string"
  ([] "")
  ([x y] (str x "&" y)))

(defn get-auth [user]
  (let [param-pairs     (map param+ (auth-req-params (:id user)))
        combined-params (r/fold query+ param-pairs)
        query-str       (str/replace-first combined-params #"&" "?")
        endpoint        (str "https://slack.com/oauth/authorize" query-str)]
    {:status 200 :body { :url endpoint }}))

