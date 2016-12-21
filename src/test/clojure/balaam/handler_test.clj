(ns balaam.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [balaam.handler :refer :all]))

(deftest test-app
  (testing "tmux weather"
    (let [response (app (mock/request :get "/tmux/weather"))]
      (is (= (:status response) 200))
      (is (= (:headers {"Content-Type" "text/plain"})))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
