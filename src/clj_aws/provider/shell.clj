(ns clj-aws.provider.shell
  (:require [cheshire.core      :as cheshire]
            [clojure.java.shell :as shell]))

(defn sh-exec
  "Runs a sh command localy"
  [command]
  (:out (shell/sh "sh" "-c" command)))

(defn exec
  "Executes a command "
  [command]
  (cheshire/parse-string (sh-exec command)))




()
