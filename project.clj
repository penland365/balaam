(defproject balaam "0.0.1-SNAPSHOT"
  :description "A Development server for making Oracles"
  :url "https://github.com/penland365/balaam"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1"]
                 [clj-http "2.3.0"]
                 [cheshire "5.6.3"]
                 [ring/ring-defaults "0.2.1"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler balaam.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
