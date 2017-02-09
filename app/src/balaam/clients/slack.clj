(ns balaam.clients.slack
  (:require [balaam.postgres :as db]
            [cheshire.core :refer :all]
            [clj-http.client :as client]
            [clojure.core.reducers :as r]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]])
  (:gen-class))

(def client-id
  (env :slack-client-id))
(def client-secret
  (env :slack-client-secret))
(def redirect-uri
  (env :slack-redirect-uri))
(def scopes
  "identify,channels:history,channels:read")

(defn- auth-req-params [user-id]
  [
    {:k "client_id"    :v client-id}
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

(defn- build-endpoint [params url]
  (let [param-pairs     (map param+ params)
        combined-params (r/fold query+ param-pairs)
        query-str       (str/replace-first combined-params #"&" "?")]
    (str url query-str)))

(defn get-auth [user]
  (let [params     (auth-req-params (:id user))
        endpoint (build-endpoint params "https://slack.com/oauth/authorize")]
    {:status 200 :body { :url endpoint }}))

(defn- redirect-error-html [user-id]
  (str "<h1>Balaam: an error has occurred during slack OAuth.</h1><h2>No user with id " user-id " found.</h2>"))

(defn- redirect-success-html [username]
  (str "<h1>Balaam thanks you " username "!</h1><h2>Slack integration had been enabled</h2>"))

(defn- redirect-error-code-html []
  (str "<h1>Balaam: an error has occurred during slack OAuth.</h1><h2>An invalid code was found. Please try again.</h2>"))

(defn- token-request [code]
  {:query-params {:client_id     client-id
                  :client_secret client-secret 
                  :redirect-uri  redirect-uri
                  :code          code}})

(defn- token-params [code]
  [{:k "client_id"     :v client-id}
   {:k "client_secret" :v client-secret}
   {:k "redirect_uri"  :v redirect-uri}
   {:k "code"          :v code}])

(defn- get-token [code]
  (let [params   (token-params code)
        endpoint (build-endpoint params "https://slack.com/api/oauth.access")]
    (client/get endpoint))) 

(defn redirect [params]
  (let [db-users (db/select-user-by-id (:state params))]
    (cond
      (not= (count db-users) 1) (redirect-error-html  (:state params))
      :else 
        (let [token-resp (get-token (:code params))
              token      (parse-string (:body token-resp) true)]
          (cond
            (false? (:ok token)) (redirect-error-code-html)
            :else
              (let [db-user (first db-users)
                    result  (db/insert-slack-token token (:id db-user))
                    error?  (instance? Throwable result)]
                (cond
                  (not error?) (redirect-success-html (:username db-user)
                  :else (redirect-error-html)))))))))

(defn- list-channels [token]
  (let [params   (list {:k "token" :v token} {:k "exclude_archived" :v true })
        endpoint (build-endpoint params "https://slack.com/api/channels.list")
        resp     (client/get endpoint)]
    (:channels (parse-string (:body resp) true))))

(defn- find-gen [channels]
  (first (filter #(= "share" (:name %)) channels)))

(defn- channel-info [token channel]
  (let [params   (list {:k "token" :v token} { :k "channel" :v channel })
        endpoint (build-endpoint params "https://slack.com/api/channels.info")
        resp     (client/get endpoint)]
    (:channel (parse-string (:body resp) true))))

(defn- channel-history [token channel ts]
  (let [params   (list {:k "token" :v token} {:k "channel" :v channel} {:k "oldest" :v ts} {:k "inclusive" :v true})
        endpoint (build-endpoint params "https://slack.com/api/channels.history")
        resp     (client/get endpoint)]
    (log/info endpoint)
    (parse-string (:body resp) true)))

(defn- count-mentions [messages user-id]
  (let [xs (map #(:text %) messages)
        predicate (str "<@" user-id ">")
        ys (map #(.contains % predicate) xs)
        zs (filter #(true? %) ys)]
    (count zs)))

(defn get-status-line [user]
  (let [slack-tokens (db/select-slack-tokens-by-user-id (:id user))
        slack-token  (:access_token (first slack-tokens))
        channels     (list-channels slack-token)
        general      (find-gen channels)
        channel-id   (:id general)
        gen          (channel-info slack-token channel-id)
        history      (channel-history slack-token channel-id (:last_read gen))
        mention-count (count-mentions (:messages history) (:slack_user_id (first slack-tokens)))]
    (str "Slack Mentions " mention-count)))
