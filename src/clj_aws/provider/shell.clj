(ns clj-aws.provider.shell
  (:require [cheshire.core      :as cheshire]
            [clojure.java.shell :as shell]))

(defn sh-exec
  "Runs a sh command localy"
  [command]
  (let [result    (shell/sh "sh" "-c" command)
        exit-code (:exit result)
        out       (:out result)
        err       (:err result)]
    (if (= 0 exit-code)
      out
      (throw (Exception. err)))))

(defn exec
  "Executes a command "
  [command]
  (cheshire/parse-string (sh-exec command)))
