(ns commcare-cli.repl
  (:require [commcare-cli.reader.simple-jline :as simple-jline]))

(defn done-commands [eof]
  #{eof 'quit 'exit '(quit) '(exit)
    "quit" "(quit)" "exit" "(exit)"})

(defn done? [eof expression]
  ((done-commands eof) expression))

(defn run-repl [{:keys [prompt subsequent-prompt history-file
                        input-stream output-stream read-line-fn]
                 :as options}]
  (loop []
    (let [eof (Object.)
          forms (simple-jline/parsed-forms
                  {:request-exit eof
                   :prompt-string "> "
                   :read-line-fn read-line-fn
                   :history-file history-file
                   :input-stream input-stream
                   :output-stream output-stream})]
      (if (done? eof (first forms))
        nil
        (let [x ""]
          (println forms)
          (recur))))))

(defn main [options]
  (let [options (assoc options :read-input-line-fn
                       (fn []
                         (simple-jline/safe-read-line
                           {:no-jline true
                            :prompt-string ""})))
        options (assoc options
                       :read-line-fn
                       (partial
                         simple-jline/safe-read-line println))]
    (run-repl options)
    (simple-jline/shutdown)))
