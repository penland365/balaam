(ns balaam.resources.data
  (:require [balaam.clients.darksky :as ds]
            [balaam.clients.github :as gh]
            [balaam.clients.google :as google]
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

(defn- valid-cache? [xs]
  "Determine if the cached weather is still valid - 1. Do we have any weather?
                                                    2. If we have weather, has it expired yet?"
  (and (not (empty? xs)) (not (expired? xs))))

(defn- determine-location [args]
  (let [wifis (:wifis args)
        lat   (:latitude args)
        lng   (:longitude args)]
    "Lat and Lng take precdence of WIFIS, as we assume the user knows more."
    (if (and (not (nil? lat)) (not (nil? lng)))
      (google/get-locale lat lng)
      (google/location wifis))))

(defn weather [user args]
  (let [content (get (first args) "accept")
        xs      (db/select-cached-weather (:id user))]
    "If the cached weather is valid, just return it"
    (log/info "valid-cache? " (valid-cache? xs))
    (if (valid-cache? xs)
      (fmt-weather [content (:data (first xs))])
      (let [location (determine-location (last args))
            weather  (ds/weather location)]
        "If the cache was empty, we are inserting weather for the first time"
        (if (empty? xs)
          (db/insert-cached-weather (:id user) weather)
          (db/update-cached-weather (:id user) weather))
        (fmt-weather [content weather])))))
