(ns commcare-cli.core
  (:require [clojure.tools.cli :as cli]
            [commcare-cli.app_host :as app_host])
  (:gen-class))

(defn parse-args [args]
  (cli/cli args
             ["-h" "--help" "Show this help screen" :flag true]
             ["-a" "--app" "Provide a CommCare app (ccz) to run"]
             ["-u" "--username" "Mobile worker username"]
             ["-p" "--password" "Mobile worker password"]
             ["-r" "--restore" "Restore xml file for a mobile worker"]))

(defn launch
  ""
  [options]
  (when (nil? (:app options)) (doall (println "must provide an app via -a") (System/exit 0)))
  (let [app
        (if (some? (:restore options))
          (app_host/install-app-with-restore (:app options) (:restore options))
          (let [username (if (:username options) (:username options) (do (println "username: ") (read-line)))
                password (if (:password options) (:password options) (do (println "password: ") (read-line)))]
          (app_host/install-app-with-creds (:app options) username password)))]
    (app_host/nav-loop app)))


(defn -main [& args]
  (let [[options args banner]
          (try (parse-args args)
            (catch Exception e
              (println (.getMessage e))
              (parse-args ["--help"])))]
    (if (:help options)
      (println banner)
      (launch options))
    (shutdown-agents)))
