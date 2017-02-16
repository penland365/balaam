(ns balaam.util
  (:require [clojure.core.reducers :as r]
            [clojure.string :as str])
  (:import  [java.security SecureRandom]
            [java.util Base64]))

(defn in?
  "true if coll contains elem"
  [coll elem]
  (some #(= elem %) coll))

(defn text? [content-type]
  "Determines if the content-type is text/plain for response formatting"
  (str/starts-with? content-type "text/plain"))

(defn salt [num-bytes]
  "Returns a secure ( yet still pseudo ) Random Salt. Note that the length of the Base64 encoded 
  salt may differ from the length passed in, which determines the number of random bytes chosen."
  (let [random    (SecureRandom.)
        xs        (byte-array num-bytes)
        _         (.nextBytes random xs)
        encoder   (Base64/getEncoder)]
    (.encodeToString encoder xs)))

(defn param+ [param]
  "Takes a map of param key and value, combining them"
  (str (:k param) "=" (:v param)))

(defn query+ 
  "A function to be folded into a list of param pairs to build a query string"
  ([] "")
  ([x y] (str x "&" y)))

(defn build-endpoint [params url]
  (let [param-pairs     (map param+ params)
        combined-params (r/fold query+ param-pairs)
        query-str       (str/replace-first combined-params #"&" "?")]
    (str url query-str)))
