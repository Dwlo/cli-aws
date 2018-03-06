(ns clj-aws.command-line
  (:require [clj-aws.ecr.ecr-client :as    ecr-client]
            [clojure.string         :as    string]
            [clojure.pprint         :refer [pprint]]
            [cheshire.core          :as    cc]
            [clojure.tools.cli      :refer [parse-opts]])
  (:gen-class))

(def cli-options
  [
   ["-r" "--ecr-repositories repos" "The ecr repositories"]
   ["-h" "--help"]])

(defn usage
  [options-summary]
  (->> ["Usage: program-name [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        " repositories-level  Get the ECR repositories level"
        ""]
       (string/join \newline)))

(defn error-msg
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
;;    (println (get-in options [:ecr-repositories]))
    (cond
      (:help options) {:exit-message (usage summary) :ok? true}
      errors          {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"repositories-level"} (first arguments)))
                      {:action (first arguments) :options options}
      :else           {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn print-as-json
  [data]
  (-> data
      (cc/generate-string {:pretty true})
      println))


(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [ecr-repos (-> options
                          (get-in [:ecr-repositories])
                          (string/split #"\s"))]
        (case action
          "repositories-level" (-> (ecr-client/get-repositories-level ecr-repos)
                                   print-as-json))))
    (System/exit 0)))
