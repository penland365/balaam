(ns balaam.resources.data
  (:require [balaam.clients.darksky :as ds]
            [balaam.clients.github :as gh]
            [balaam.clients.google :as google]
            [balaam.clients.slack :as slack]
            [balaam.resources.data.slack :as sdata]
            [balaam.postgres :as db]
            [cheshire.core :refer :all]
            [clojure.core.reducers :as r]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import [java.time Instant]
           [java.time ZonedDateTime])
  (:gen-class))

(defn- text? [content-type]
  "Determines if the content-type is text/plain for response formatting"
  (str/starts-with? content-type "text/plain"))

(defn- expired? [results]
  (let [x             (first results)
        last-modified (.toInstant (:last_modified_at x))
        expires-at    (.plusSeconds last-modified (:seconds_to_cache x))
        now           (Instant/now)]
    (.isAfter now expires-at)))

(defn- format-github-response [xs]
  (if (text? (:content xs))
    (if (nil? (:state (:data xs)))
      (format "Mentions %s Notifs %s" (:mentions (:data xs)) (:notifications (:data xs)))
      (format "Mentions %s Notifs %s Branch State %s" (:mentions (:data xs))
                                                       (:notifications (:data xs))
                                                       (:state (:data xs))))
    {:status 200 :body (:data xs)}))

(defn- github-mention+
  "A function to be folded into a list of github notifications to count the number
  of mentions"
  ([] 0)
  ([x y] (cond
           (= "mention"      (:reason y)) (+ x 1)
           (= "team_mention" (:reason y)) (+ x 1)
           :else x)))

(defn- get-github-notifications [uid content]
  (let [tokens   (db/select-gh-tokens-by-user-id uid)
        notifs   (gh/get-notifications (:access_token (first tokens)))
        mentions (r/fold github-mention+ notifs)
        data     {:notifications (count notifs) :mentions mentions}]
    {:data data :content content}))

(defn- get-ref-status [uid owner repo ref]
  (let [tokens   (db/select-gh-tokens-by-user-id uid)
        statuses (gh/get-branch-status (:access_token (first tokens)) owner repo ref)
        xs       (map (fn [x]
                        {:state (:state x)
                         :updated_at (ZonedDateTime/parse (:created_at x))})
                      statuses)
        ys       (sort-by :updated_at xs)]
    (:state (last ys))))

(defn- load-github [uid content]
  (log/warn "Loading new set of Github Notifications")
  (let [data (get-github-notifications uid content)
        _    (db/insert-cached-github uid (:data data))]
    data))

(defn- refresh-github [uid content]
  (log/warn "Refreshing Github Notifications")
  (let [data (get-github-notifications uid content)
        _    (db/update-cached-github uid (:data data))]
    data))

(defn- refresh-github-status [uid content xs]
  (log/warn "Refreshing Github Notifications and Status")
  (let [data   (get-github-notifications uid content)
        status (get-ref-status uid (:owner xs) (:repo xs) (:ref xs))
        ys     (assoc-in data [:data :state] status)
        _      (db/update-cached-github uid (:data ys))]
    (log/info data)
    (log/info ys)
    (log/info status)
    ys))

(def load-and-format-github    (comp format-github-response load-github))
(def refresh-and-format-github (comp format-github-response refresh-github))
(def refresh-and-format-status-github (comp format-github-response refresh-github-status))
(defn- status-check? [args]
  (let [owner  (:owner (last args))
        repo   (:repo  (last args))
        branch (:ref   (last args))
        xs [owner repo branch]]
    (every? some? xs)))

(defn- no-status-check [user args]
  "If no branch check has been requested, return normal Github Notifications
  and Mentions"
  (let [results (db/select-cached-github (:id user))
        content (get (first args) "accept")]
    (cond
      (empty? results)                         (load-and-format-github (:id user) content)
      (true? (expired?  results))              (refresh-and-format-github (:id user) content)
      (some? (:state (:data (first results)))) (refresh-and-format-github (:id user) content)
      :else
        (let [data (:data (first results))
              content (get (first args) "accept")]
        (format-github-response {:data data :content content})))))

(defn- status-check [user args]
  (let [results (db/select-cached-github (:id user))
        content (get (first args) "accept")]
    (cond
      (empty? results)                        (load-and-format-github (:id user) content)
      (true? (expired?  results))             (refresh-and-format-status-github 
                                                (:id user)
                                                content
                                                { :owner (:owner (last args))
                                                  :repo  (:repo  (last args))
                                                  :ref   (:ref   (last args))})
      (nil? (:state (:data (first results)))) (refresh-and-format-status-github 
                                                (:id user)
                                                content
                                                { :owner (:owner (last args))
                                                  :repo  (:repo  (last args))
                                                  :ref   (:ref   (last args))})
      :else
        (let [data (:data (first results))
              content (get (first args) "accept")]
        (format-github-response {:data data :content content})))))

(defn github [user args]
  (if (status-check? args) (status-check user args) (no-status-check user args)))

(defn- fmt-weather [xs]
  (let [content (first xs)
        weather (last xs)]
    (cond
      (true? (text? content)) 
        (if (nil? (:locale weather))
          (format "%s %s" (:temperature weather) (:icon weather))
          (format "%s %s %s" (:locale weather)
                             (:temperature weather)
                             (:icon weather)))
      :else {:status 200 :body weather})))

(defn- get-fresh-weather [wifis]
  (let [location (google/location wifis)]
    (ds/weather location)))

(defn- refresh-weather [wifis content uid]
  (let [weather (get-fresh-weather wifis)
        _       (db/update-cached-weather uid weather)]
  [content weather]))

(defn- load-weather [wifis content uid]
  (let [weather (get-fresh-weather wifis)
        _       (db/insert-cached-weather uid weather)]
    [content weather]))

(def refresh-fmt-weather (comp fmt-weather refresh-weather))
(def load-fmt-weather    (comp fmt-weather load-weather))
(defn weather [user args]
  (let [content (get (first args) "accept")
        wifis   (parse-string (:wifis (last args)) true)
        xs      (db/select-cached-weather (:id user))]
    (cond
      (empty? xs)           (load-fmt-weather wifis content (:id user))
      (true? (expired? xs)) (refresh-fmt-weather wifis content (:id user))
      :else
        (fmt-weather [content (:data (first xs))]))))

(defrecord SlackXs [slack content error?])

(defn- load-slack [uid content]
  "Fetches slack data via balaam.clients.slack. The only difference between
  this and reload-slack data is that this calls an insert function for the
  database while reload-slack calls an update function on the existing
  cached data."
  (let [ss (db/select-slack-tokens-by-user-id uid)]
    (cond
      (empty? ss) (SlackXs. nil content true)
      :else
        (let [slack (sdata/get-data ss)
              _     (db/insert-cached-slack uid slack)]
          (SlackXs. slack content false)))))

(defn- reload-slack [uid content]
  "Fetches slack data via balaam.clients.slack. The only difference between
  this and load-slack data is that this calls an update function for the
  database while load-slack calls an insert function for a new cached_data record."
  (let [ss (db/select-slack-tokens-by-user-id uid)]
    (cond
      (empty? ss) (SlackXs. nil content true)
      :else
        (let [slack (sdata/get-data ss)
              _     (db/update-cached-slack uid slack)]
          (SlackXs. slack content false)))))

(defn- format-slack-resp [xs]
  "Formats slack response based on requested content-type. xs is assumed to be
   SlackXs, containing the slack data plus Accepted Content-Type header value.
   If the error field is set to true, a 404 is passed back."
  (cond
    (true? (:error? xs)) {:status 404 :body {:error "No slack authorization tokens found."}}
    :else
      (let [slack (:slack xs)]
        (cond
          (true? (text? (:content xs))) (format "Mentions %s Unread %s" (:mentions slack) (:unread slack))
          :else {:status 200 :body slack}))))

(def load-and-format-slack (comp format-slack-resp load-slack))
(def reload-and-format-slack (comp format-slack-resp reload-slack))
(defn show-slack [user args]
  "Gets slack data. This data is cached for a period of 2 minutes before being refreshed. 
   The response type is determined by the Accept header. Unless specified to text/plain,
   it defaults to application/json."
  (let [content (get (first args) "accept")
        xs      (db/select-cached-slack (:id user))]
    (cond
      (empty? xs)           (load-and-format-slack (:id user) content)
      (true? (expired? xs)) (reload-and-format-slack (:id user) content)
      :else
        (let [slack   (:data (first xs))
              slackxs (SlackXs. slack content false)]
          (format-slack-resp slackxs)))))
