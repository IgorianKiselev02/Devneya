#!/usr/bin/env bb
(ns devneya.core
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [devneya.gpt :as gpt]
            [devneya.utils :as utils]
            [devneya.exec :as exec])
  (:gen-class :main true))

(def config (utils/load-config "config.yml"))

(def cli-options
  [["-o" "--output-filename FILE" "Output file path"
    :default "./code-path/code.js"]
   ["-x" "--[no-]exec" "Execute the code"
    :default false]
   ["-h" "--help"]])

(defn usage 
  "Composes the summary string"
  [options-summary]
  (->> ["Usage: program-name [options] prompt"
        ""
        "Options:"
        options-summary
        ""
        "Prompt:"
        "Text request to ChatGPT"
        ""
        "Please refer to the manual page for more information."]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments."
  [args]
  (let [{:keys [options arguments summary errors]} (parse-opts args cli-options)
        prompt (str/join " " arguments)]

    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      
      (not (str/blank? prompt))
      {:prompt prompt :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn make-prompt [args]
 (let [{:keys [prompt options exit-message ok?]} (validate-args args)]
   (if exit-message
     (exit (if ok? 0 1) exit-message)
     (let [response (gpt/get-chatgpt-api-response prompt (:api-key config))]
       (spit (:output-filename options) response)
       (if (= (:exec options) true)
         (exec/exec-code (:deno-token config) (:deno-project config) (:output-filename options))
         (println "Code saved in file:" (:output-filename options))
         )
       )
     )
   )
)

(defn -main [& args]
  (make-prompt args))
