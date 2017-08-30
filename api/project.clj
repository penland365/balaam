(defproject balaam "0.2.7"
  :description "A Development server for making Oracles"
  :url "https://github.com/penland365/balaam"
  :min-lein-version "2.0.0"
  :dependencies [[alaisi/postgres.async "0.8.0"]
                 [org.clojure/clojure "1.8.0"]
                 [cheshire "5.6.3"]
                 [clj-http "2.3.0"]
                 [compojure "1.5.1"]
                 [crypto-password "0.2.0"]
                 [environ "1.1.0"]
                 [org.clojure/core.cache "0.6.5"]
                 [org.clojure/test.check "0.9.0"]
                 [org.slf4j/slf4j-api "1.7.22"]
                 [org.slf4j/slf4j-simple "1.7.22"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [ring.middleware.logger "0.5.0"]
                 [slingshot "0.12.2"]]
  :plugins [[lein-codox "0.10.3"]
            [lein-ring "0.9.7"]]
  :ring {:handler balaam.handler/app
         :nrepl {:start? true
                 :port   10071}}
  :profiles { :dev { :dependencies [[javax.servlet/servlet-api "2.5"]
                                         [ring/ring-mock "0.3.0"]]}})
