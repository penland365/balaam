(ns balaam.auth
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [crypto.password.scrypt :as passwd]
            [balaam.postgres :as db])
  (:import  [java.security SecureRandom]
            [java.util Base64])
  (:gen-class))

(defrecord UserPass [username password])

(defn- parse-user-pass [auth-header]
  (let [encoded-user-pass (last (str/split auth-header #" "))
        decoder           (Base64/getDecoder)
        decoded-xs        (.decode decoder encoded-user-pass)
        decoded-str       (String. decoded-xs)
        username          (first (str/split decoded-str #":"))
        password          (last (str/split decoded-str #":"))]
    (UserPass. username password)))

(defn user-namespace-exists? [request & handlers]
  "For resources that are namespaced by user, this function checks to ensure that user exists
  prior to the authorization stage. For example, a request GET /balaam/... will continue down
  the handler chain if the user balaam exists. If that user does not exist, a 404 is returned
  prior to any authorization attempt"
  (let [user     (get-in request [:params :username])
        db-users (db/select-user user)]
    (cond
      (not= (count db-users) 1) {:status 404}
      :else
        (let [db-user (first db-users)]
          ((first handlers) request db-user (last handlers))))))

(defn namespace-auth [request & args]
  "Determines whether a user has authorization for a namespaced resource that has already
  been determined to be present. If the user is valid and has access, the handler is invoked."
  (let [header-value (get-in request [:headers "authorization"])]
    (cond 
      (= header-value nil) {:status 401 :body {:reason "username and password required"}}
      :else 
        (let [user-pass (parse-user-pass header-value)]
          (cond
            (instance? Throwable user-pass) {:status 401}
            (= user-pass nil) {:status 401}
            :else
              (let [db-user (first args)
                    salted  (str (:salt db-user) "." (:password user-pass))
                    valid   (passwd/check salted (:password db-user))]
                (cond
                  (not valid) {:status 401 :body {:reason "invalid username or password"}}
                  (not= (:username user-pass) (:username db-user)) {:status 401 :body {:reason "invalid username or password"}}
                  :else
                    ((last args) db-user))))))))



(defn authorize [request handler ]
  "Determines whether the user has Authorization to a specific resource.
   If the answer is yes, the handler is invoked"
  (let [header-value (get-in request [:headers "authorization"])]
    (cond 
      (= header-value nil) {:status 401 :body {:reason "username and password required"}}
      :else 
        (let [user-pass (parse-user-pass header-value)]
          (cond
            (instance? Throwable user-pass) {:status 401}
            (= user-pass nil) {:status 401}
            :else
              (let [users (db/select-user (:username user-pass))]
                (cond
                  (not= (count users) 1) {:status 401 :body {:reason "invalid username or password"}}
                  :else
                    (let [db-user (first users)
                          salted  (str (:salt db-user) "." (:password user-pass))
                          valid   (passwd/check salted (:password db-user))]
                      (cond
                        (not valid) {:status 401 :body {:reason "invalid username or password"}}
                        :else
                          (handler db-user))))))))))

(defn salt [num-bytes]
  "Returns a secure ( yet still pseudo ) Random Salt. Note that the length of the Base64 encoded 
  salt may differ from the length passed in, which determines the number of random bytes chosen."
  (let [random    (SecureRandom.)
        xs        (byte-array num-bytes)
        _         (.nextBytes random xs)
        encoder   (Base64/getEncoder)]
    (.encodeToString encoder xs)))

(defn encrypt [password s]
  (let [comb      (str s "." password)
        encrypted (passwd/encrypt comb)]
    encrypted))
