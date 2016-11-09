(ns commcare-cli.repl
  (:require [commcare-cli.reader.simple-jline :as simple-jline]))

(defn done-commands [eof]
  #{eof "" "quit" "(quit)" "exit" "(exit)"})

(defn done? [eof expression]
  ((done-commands eof) expression))

(defn run-repl [{:keys [prompt subsequent-prompt history-file
                        process-func
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
        (do
          (process-func (first forms))
          (recur))))))

;; [String -> None] String -> None
(defn start-repl [process-func repl-name]
  (let [options {:read-input-line-fn
                 (fn [] (simple-jline/safe-read-line
                          {:no-jline true
                           :prompt-string ""}))
                 :read-line-fn (partial simple-jline/safe-read-line println)
                 :process-func process-func
                 :history-file repl-name}]
    (run-repl options)
    (simple-jline/shutdown)))

(defn main [options]
  (let [options (assoc options :read-input-line-fn
                       (fn []
                         (simple-jline/safe-read-line
                           {:no-jline true
                            :prompt-string ""})))
        options (assoc options
                       :read-line-fn
                       (partial
                         simple-jline/safe-read-line println))
        options (assoc options :process-func println)]
    (run-repl options)
    (simple-jline/shutdown)))
