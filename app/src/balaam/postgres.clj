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

(defn- select-cached-data [uid datum-type]
  (<!! (query! db ["SELECT id, user_id, datum_type::text, seconds_to_cache, data, last_modified_at 
                   FROM balaam.cached_data 
                   WHERE user_id = $1 AND datum_type = $2;" uid datum-type])))

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

(defn insert-pending-github-token [uid state]
  (<!! (insert! db {:table "balaam.github_tokens"} {:user_id uid
                                                    :auth_status "PENDING"
                                                    :state state})))

(defn select-pending-gh-token-by-state [state]
  (<!! (query! db ["SELECT id, user_id, state FROM balaam.github_tokens WHERE auth_status = 'PENDING' AND state = $1;" state])))

(defn update-gh-token [id gh-token]
  (<!! (update! db {:table "balaam.github_tokens" 
                    :where ["id = $1 AND auth_status = 'PENDING'" id]}
                    {:access_token (:access_token gh-token)
                     :auth_status  "COMPLETED"
                     :scope        (:scope gh-token)
                     :token_type   (:token_type gh-token)})))

(defn select-gh-tokens-by-user-id [uid]
  (<!! (query! db ["SELECT id, user_id, access_token FROM balaam.github_tokens WHERE auth_status = 'COMPLETED' AND user_id = $1;" uid])))

(defn- insert-cached-data [uid json datum-type]
  (<!! (insert! db {:table "balaam.cached_data"} {:user_id uid
                                                  :datum_type datum-type
                                                  :data json})))

(defn- update-cached-data [uid json datum-type]
  (<!! (update! db {:table "balaam.cached_data" 
                    :where ["user_id = $1 AND datum_type = $2;" uid datum-type]} 
                    {:data json})))

(defn select-cached-github[uid]
  (select-cached-data uid "GITHUB"))

(defn insert-cached-github [uid data]
  (insert-cached-data uid data "GITHUB"))

(defn update-cached-github [uid data]
  (update-cached-data uid data "GITHUB"))
