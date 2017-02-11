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
                  (not error?) (redirect-success-html (:username db-user))
                  :else 
                    (redirect-error-html (:id db-user)))))))))

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
  (let [params   (list {:k "token" :v token} {:k "channel" :v channel} {:k "oldest" :v ts} {:k "inclusive" :v false})
        endpoint (build-endpoint params "https://slack.com/api/channels.history")
        resp     (client/get endpoint)]
    (parse-string (:body resp) true)))

(defrecord MU [message predicate])

(defn- cont [mu]
  (let [x (.contains (:message mu) (:predicate mu))]
    (cond
      (true? x) (log/info (:message mu)))
     x))

(defn- count-mentions [messages user-id]
  (let [xs        (map #(:text %) messages)
        predicate (str "<@" user-id ">")
        mus       (map #(MU. % predicate) xs)
        ys        (map cont mus)
        zs        (filter #(true? %) ys)]
    (count zs)))

(defrecord ChanTokenId [info token userid])
(defn- mentions-in-channel [tuple]
  (let [info       (:info tuple)
        stoken     (:token tuple)
        suserid    (:userid tuple)
        channel-id (:id info)
        lread      (:last_read info)
        history    (channel-history stoken channel-id lread)
        messages   (:messages history)]
    (count-mentions messages suserid)))

(defrecord ChanRead [read? info])
(defn- unread_chan? [stokenchan]
  (let [stoken  (:stoken stokenchan)
        channel (:channel stokenchan)
        info    (channel-info stoken (:id channel))]
    (cond
      (nil? (:unread_count_display info))   (ChanRead. false info)
      (= (:unread_count_display info) 0)    (ChanRead. false info)
      :else (ChanRead. true info))))

(defrecord STokenChan [stoken channel])
(defn- unread-channels [channels stoken]
  (let [token-channels   (map #(STokenChan. stoken %) channels)
        unread_channels? (pmap unread_chan? token-channels)
        unread_channels  (filter #(:read? %) unread_channels?)
        xs               (map #(:info %) unread_channels)]
    xs))

(defn- sum-unread [channels]
  (let [xs (map #(:unread_count_display %) channels)]
    (reduce + xs)))

(defn- sum-notifications [infos stoken suserid]
  (let [xs (map #(ChanTokenId. % stoken suserid) infos)
        ys (pmap mentions-in-channel xs)]
    (reduce + ys)))

(defn get-status-line [user]
  (let [slack-tokens     (db/select-slack-tokens-by-user-id (:id user))
        slack-token      (:access_token (first slack-tokens))
        slack-user-id    (:slack_user_id (first slack-tokens))
        channels         (list-channels slack-token)
        unread-chans     (unread-channels channels slack-token)
        unread-msg-count (sum-unread unread-chans)
        notifications    (sum-notifications unread-chans slack-token slack-user-id)]
    (str "Notifications " notifications " Messages " unread-msg-count)))

(defn- data-per-room [record]
  "Gets all relevent data per room."
  (let [stoken           (:access_token record)
        suid             (:slack_user_id record)
        channels         (list-channels stoken)
        unread-chans     (unread-channels channels stoken)
        unread-msg-count (sum-unread unread-chans)
        notifications    (sum-notifications unread-chans stoken suid)]
    {:mentions notifications :unread unread-msg-count}))

;;(defn get-data [tokens]
;;  (let [stoken           (:access_token (first tokens))
;;        suid             (:slack_user_id (first tokens))
;;        channels         (list-channels stoken)
;;        unread-chans     (unread-channels channels stoken)
;;        unread-msg-count (sum-unread unread-chans)
;;        notifications    (sum-notifications unread-chans stoken suid)]
;;    {:mentions notifications :unread unread-msg-count}))

(defn- slack+ [x y]
  "A function to be folded into a list of slack records"
  (let [mentions (+ (:mentions x) (:mentions y))
        msgs     (+ (:unread x) (:unread y))]
  {:mentions mentions :unread msgs}))


(defn get-data [records]
  (let [room-counts (pmap data-per-room records)]
    (reduce slack+ room-counts)))
