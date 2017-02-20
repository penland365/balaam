(ns balaam.resources.data.slack
  (:require [balaam.clients.slack :as slack]
            [cheshire.core :refer :all]
            [clojure.core.reducers :as r]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn- make-group-infos [token groups]
  (pmap #(slack/group-info token (:id %)) groups))

(defn- filter-unread [xs]
  (let [ys      (remove #(nil? (:unread_count_display %)) xs)
        unreads (filter #(< 0 (:unread_count_display %)) ys)]
    unreads))

(defn- sum-unread [xs]
  "Counts all unread count displays in a list of json objects"
  (reduce + (map #(:unread_count_display %) xs)))

(defn- make-group-histories [token xs]
  (pmap #(slack/group-history token (:id %) (:last_read %)) xs))

(defn- make-channel-histories [token xs]
  (pmap #(slack/chan-history token (:id %) (:last_read %)) xs))

(defn- make-channel-infos [token channels]
  (pmap #(slack/channel-info token (:id %)) channels))

(defrecord MU [message predicate])
(defn- count-mentions [messages-collection user-id]
  "Counts all mentions. The messages collection is an array of arrays of messages,
   where the top level array is the channel / group / DM"
  (count (remove #(false? %)
                 (flatten (map (fn [objects]
                                 (map (fn [message]
                                        (let [predicate (str "<@" user-id ">")]
                                          (.contains (:text message) predicate)))
                                      objects))
                               messages-collection)))))

(defn- make-group-data [record]
  (let [groups           (slack/list-groups (:access_token record))
        group-infos      (make-group-infos (:access_token record) groups)
        unread-groups    (filter-unread group-infos)
        group-histories  (make-group-histories (:access_token record) unread-groups)
        mentions         (count-mentions group-histories (:slack_user_id record))]
    {:unread (sum-unread unread-groups) :mentions mentions}))

(defn- make-channel-data [record]
  (let [channels          (slack/list-channels (:access_token record))
        channel-infos     (make-channel-infos (:access_token record) channels)
        unread-channels   (filter-unread channel-infos)
        channel-histories (make-channel-histories (:access_token record) unread-channels)
        mentions          (count-mentions channel-histories (:slack_user_id record))]
    {:unread (sum-unread unread-channels) :mentions mentions}))

(defn- slack+ [x y]
  "A function to be folded into a list of slack records"
  (let [mentions (+ (:mentions x) (:mentions y))
        msgs     (+ (:unread x) (:unread y))]
  {:mentions mentions :unread msgs}))

(defn- data-per-room [record]
  "Gets all relevent data per room."
  (let [groups   (future (make-group-data record))
        channels (future (make-channel-data record))]
    (reduce slack+ [@channels @groups])))

(defn get-data [records]
  "High level function to get collate all Slack data"
  (let [room-counts (pmap data-per-room records)]
    (reduce slack+ room-counts)))
