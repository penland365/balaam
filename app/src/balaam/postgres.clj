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

(defn- insert-user [username password salt]
  (<!! (insert! db {:table "balaam.users"} {:username username
                                            :password password
                                            :salt     salt})))
(defn wait-insert-user [username password salt]
  (insert-user username password salt))

