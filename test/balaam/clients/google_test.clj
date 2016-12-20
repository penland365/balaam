(ns balaam.clients.google-test
  (:require [clojure.test :refer :all]
            [balaam.clients.google :refer :all]
            [cheshire.core :refer :all]))

(defn- wifis [] [
  {
    :bssid   "04:62:73:72:df:da"
    :rssi    -80
    :channel 64
  },
  {
    :bssid   "04:62:73:79:29:aa"
    :rssi    -78
    :channel 52
  }])

(defn- google-response [] {
  :status 200
  :headers [{:Server "GSE"}]
  :body (generate-string {
    :location {
      :lat 33.141591999999996
      :lng -96.81763049999999
    }
    :accuracy 5833.0
  })})

(deftest test-google-wifi-access-point 
  (testing "a function from an Airport map to a wifiAccessPoint map"
    (let [wifi (#'balaam.clients.google/wifi-access-point (first (wifis)))]
      (is (= "04:62:73:72:df:da" (get wifi :macAddress)))
      (is (= -80                 (get wifi :signalStrength)))
      (is (= 64                  (get wifi :channel)))
      (is (= 0                   (get wifi :age))))))

(deftest test-google-request-no-wifi
  (testing "valid request JSON with no wifiAccessPoints"
    (let [request (#'balaam.clients.google/request [] "api-key")]
      (is (= {:key "api-key"} (get request :query-params)))
      (is (= :json (get request :content-type)))
      (is (= :json (get request :accept)))
      (let [form-params (get request :form-params)]
        (is (= :true (get form-params :considerIp)))
        (is (= true (empty? (get form-params :wifiAccessPoints))))))))

(deftest test-google-request-wifis
  (testing "valid request JSON with wifiAccessPoints"
    (let [request (#'balaam.clients.google/request (wifis) "api-key")]
      (is (= {:key "api-key"} (get request :query-params)))
      (is (= :json (get request :content-type)))
      (is (= :json (get request :accept)))
      (let [form-params (get request :form-params)]
        (is (= :true (get form-params :considerIp)))
        (is (= 2 (count (get form-params :wifiAccessPoints))))))))

(deftest test-google-parse-location
  (testing "parse location from a 200 OK response from Google"
    (let [location (#'balaam.clients.google/parse-location (google-response))]
      (is (= 33.141591999999996 (get location :lat)))
      (is (= -96.81763049999999 (get location :lng))))))
