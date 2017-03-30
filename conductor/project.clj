(defproject conductor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[http-kit "2.2.0"]
                 [environ "1.1.0"]
                 [org.clojure/clojure "1.8.0"]
                 [clj-http "3.4.1"]
                 [org.clojure/core.async "0.3.442"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-simple "1.7.22"]
                 [org.slf4j/slf4j-api "1.7.22"]
                 [org.slf4j/slf4j-log4j12 "1.7.22"]]
  :main ^:skip-aot conductor.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
