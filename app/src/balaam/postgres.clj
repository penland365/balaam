(ns balaam.postgres
  (:require [postgres.async :refer :all]
            [clojure.core.async :refer [<!! go]]
            [environ.core :refer [env]]))

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
