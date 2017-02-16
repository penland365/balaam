(ns balaam.tmux
  (:require [balaam.clients.google :as google]
            [balaam.clients.darksky :as darksky]
            [balaam.clients.slack :as slack]
            [balaam.postgres :as db]
            [cheshire.core :refer :all]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import [java.time.temporal ChronoUnit]
           [java.time Instant])
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
        (let [slack (slack/get-data ss)
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
        (let [slack (slack/get-data ss)
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
(defn get-slack [user args]
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
