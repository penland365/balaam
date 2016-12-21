(defproject balaam "0.0.1-SNAPSHOT"
  :description "A Development server for making Oracles"
  :url "https://github.com/penland365/balaam"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1"]
                 [clj-http "2.3.0"]
                 [cheshire "5.6.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.22"]
                 [org.slf4j/slf4j-simple "1.7.22"]
                 [org.clojure/core.cache "0.6.5"]
                 [ring/ring-defaults "0.2.1"]]
  :plugins [[lein-ring "0.9.7"]]
  :source-paths ["src/main/clojure"]
  :resource-paths ["src/main/resources/profiles/prod"]
  :ring {:handler balaam.handler/app}
  :profiles {:prod {:resource-paths ["src/main/resources/profiles/prod"]}
             :dev  {:resource-paths ["src/main/resources/profiles/dev"]
                    :dependencies [[javax.servlet/servlet-api "2.5"]
                                         [ring/ring-mock "0.3.0"]]}})
