(ns balaam.postgres
  (:require [postgres.async :refer :all]
            [clojure.core.async :refer [<!! go]]))

(def db (open-db {:hostname "192.168.99.100"
                  :port 5432
                  :database "balaam"
                  :username "moses"
                  :password "buniversity1"
                  :pool-size 25}))

(defn select-user [username]
  (<!! (query! db ["SELECT id, username, password, salt FROM balaam.users WHERE username = $1" username])))

(defn- insert-user [username password salt]
  (<!! (insert! db {:table "balaam.users"} {:username username
                                            :password password
                                            :salt     salt})))
(defn wait-insert-user [username password salt]
  (insert-user username password salt))

