(ns balaam.tmux-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [balaam.tmux :refer :all]))

(comment deftest test-tmux
  (testing "tmux weather"
    (let [response (app (mock/request :get "/tmux/weather"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World")))))

