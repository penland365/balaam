(ns balaam.util-test
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.core.reducers :as r]
            [balaam.util :refer :all])
  (:gen-class))

(defspec text?-is-false-when-content-type-is-not-textplain
  100
  (prop/for-all [x gen/string]
    (false? (text? x))))

(deftest text?-is-true-when-content-type-is-textplain
  (testing "text? is true when content-type is text/plain"
    (is (true? (text? "text/plain")))))

(deftest test-param+
  (testing "Combining params"
    (let [params    {:k "scope" :v "foo"}
          param-str (param+ params)]
      (is (= "scope=foo" param-str)))))

(deftest test-query+
  (testing "Combining params into a query string"
    (let [params    (list "scope=foo" "state=moo")
          query-str (r/fold query+ params)]
      (is (= "&scope=foo&state=moo" query-str)))))

(deftest test-build-endpoint
  (testing "A completed endpoint"
    (let [params   (list {:k "scope" :v "foo"} {:k "state" :v "foo"})
          url      "https://api.github.com"
          endpoint (build-endpoint params url)]
      (is (= "https://api.github.com?scope=foo&state=foo" endpoint)))))

(deftest test-in?
  (testing "true if coll contains elem"
    (let [xs ["foo" "bar" "baz"]
          x  (in? xs "foo")]
      (is (true? x)))))
