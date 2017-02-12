(ns balaam.util-test
  (:require [clojure.test :refer :all]
            [clojure.core.reducers :as r]
            [balaam.util :refer :all])
  (:gen-class))

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
