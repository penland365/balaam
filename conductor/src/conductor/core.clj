(ns conductor.core
  (:require [clojure.data.json :as json]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str]
            [conductor.modules :as mods]
            [conductor.db :as db]
            [conductor.docker :as docker])
  (:gen-class))

(def cli-options
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   ["-db" "--db-version DB VERSION" "balaam database version"
    :default "latest"]
   ["-api" "--api-version API VERSION" "balaam api version"
    :default "latest"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Conductor. Orchestrating a balaam development environment"
        ""
        "Usage: conductor -db v0.1.3 -api v0.2.0 backend"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  db       Runs the database container by itself"
        "  api      Runs the API container by itself"
        "  backend  Runs both the DB and API together"
        ""
        "Please refer to the project README for more information."]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"db" "api" "backend"} (first arguments)))
      {:action (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message))
      (case action
        "db"      (db/run-db! options)
        "api"     (mods/run-api! options)
        "backend" 
        (let [f-db (future (db/run-db! options))
              f-api (future (mods/run-api! options))]
          (Thread/sleep Long/MAX_VALUE)))))
