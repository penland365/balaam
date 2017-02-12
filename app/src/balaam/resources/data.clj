(ns balaam.resources.data
  (:require [balaam.clients.github :as gh]
            [balaam.postgres :as db]
            [cheshire.core :refer :all]
            [clojure.core.reducers :as r]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import [java.time Instant])
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
  (cond
    (true? (text? (:content xs))) (format "Notifications %s Mentions %s" 
                                          (:notifications (:data xs))
                                          (:mentions (:data xs)))
    :else {:status 200 :body (:data xs)}))

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

(def load-and-format-github    (comp format-github-response load-github))
(def refresh-and-format-github (comp format-github-response refresh-github))
(defn github [user args]
  (let [results (db/select-cached-github (:id user))
        content (get (first args) "accept")]
    (cond
      (empty? results)            (load-and-format-github (:id user) content)
      (true? (expired?  results)) (refresh-and-format-github (:id user) content)
      :else
        (let [data (:data (first results))
              content (get (first args) "accept")]
        (format-github-response {:data data :content content})))))
