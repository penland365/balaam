(ns balaam.postgres
  (:require [clojure.core.async :refer [<!! go]]
            [environ.core :refer [env]]
            [postgres.async :refer :all]
            [postgres.async.json])
  (:gen-class))

(def db (open-db {:hostname (env :database-hostname)
                  :port     (Integer. (env :database-port))
                  :database (env :database) 
                  :username (env :database-username) 
                  :password (env :database-password)
                  :pool-size 25}))

(defn select-user [username]
  (<!! (query! db ["SELECT id, username, password, salt FROM balaam.users WHERE username = $1" username])))

(defn select-user-by-id [id]
  (<!! (query! db ["SELECT id, username, password, salt FROM balaam.users WHERE id = $1" id])))

(defn- insert-user [username password salt]
  (<!! (insert! db {:table "balaam.users"} {:username username
                                            :password password
                                            :salt     salt})))
(defn wait-insert-user [username password salt]
  (insert-user username password salt))

(defn insert-slack-token [slack user-id]
  (<!! (insert! db {:table "balaam.slack_tokens"} {:user_id         user-id
                                                   :access_token    (:access_token slack)
                                                   :scope           (:scope slack)
                                                   :slack_user_id   (:user_id slack)
                                                   :slack_team_name (:team_name slack)
                                                   :slack_team_id   (:team_id slack)})))

(defn select-slack-tokens-by-user-id [user-id]
  (<!! (query! db ["SELECT id, user_id, access_token, scope, slack_user_id, slack_team_name, slack_team_id FROM balaam.slack_tokens WHERE user_id = $1" user-id])))

(defn select-cached-weather [uid]
  (<!! (query! db ["SELECT id, user_id, datum_type::text, seconds_to_cache, data, last_modified_at FROM balaam.cached_data WHERE user_id = $1 AND datum_type = 'WEATHER';" uid])))

(defn insert-cached-weather [uid json]
  (<!! (insert! db {:table "balaam.cached_data"} {:user_id uid
                                                  :datum_type "WEATHER"
                                                  :data json})))
(defn update-cached-weather [uid json]
  (<!! (update! db {:table "balaam.cached_data" :where ["user_id = $1 AND datum_type = 'WEATHER'" uid]} {:data json})))

(defn select-cached-slack [uid]
  (<!! (query! db ["SELECT id, user_id, datum_type::text, seconds_to_cache, data, last_modified_at FROM balaam.cached_data WHERE user_id = $1 AND datum_type = 'SLACK';" uid])))

(defn insert-cached-slack [uid json]
  (<!! (insert! db {:table "balaam.cached_data"} {:user_id uid
                                                  :datum_type "SLACK"
                                                  :data json})))

(defn update-cached-slack [uid json]
  (<!! (update! db {:table "balaam.cached_data" :where ["user_id = $1 AND datum_type = 'SLACK'" uid]} {:data json})))
