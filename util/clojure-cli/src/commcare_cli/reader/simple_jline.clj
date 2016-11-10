(ns commcare-cli.reader.simple-jline
  (:import [java.io File FileInputStream FileDescriptor
            PrintStream ByteArrayOutputStream IOException]
           [jline.console ConsoleReader]
           [jline.console.history FileHistory MemoryHistory]
           [jline.internal Configuration Log]))

(def ^:private current-console-reader (atom nil))

(defn- make-history-file [^String history-path]
  (if history-path
    (let [history-file (File. history-path)]
      (if (.getParentFile history-file)
        history-file
        (File. "." history-path)))
    (File. (System/getProperty "user.home") ".jline-commcare-cli.history")))

(defn reset-reader [^ConsoleReader reader]
  (when reader
    (.clear (.getCursorBuffer reader))))

(defn shutdown
  ([] (shutdown {:reader @current-console-reader}))
  ([{:keys [^ConsoleReader reader] :as state}]
   (when reader
     (reset-reader reader)
     (.restore (.getTerminal reader))
     (.shutdown reader))
   (reset! current-console-reader nil)))

(defn null-output-stream []
  (proxy [java.io.OutputStream] []
    (write [& args])))

(defn set-jline-output! []
  (when (and (not (Boolean/getBoolean "jline.internal.Log.trace"))
             (not (Boolean/getBoolean "jline.internal.Log.debug")))
    (Log/setOutput (PrintStream. ^java.io.OutputStream (null-output-stream)))))

(defn- initialize-jline []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(when-let [reader @current-console-reader]
                                (shutdown {:reader reader}))))
  (when (= "dumb" (System/getenv "TERM"))
    (.setProperty (Configuration/getProperties) "jline.terminal" "none"))
  (set-jline-output!))

(defmulti flush-history type)
(defmethod flush-history FileHistory
  [^FileHistory history]
  (try (.flush history)
    (catch IOException e)))
(defmethod flush-history MemoryHistory
  [history])

(defprotocol InteractiveLineReader
  (interactive-read-line [this])
  (prepare-for-next-read [this]))

(extend-protocol InteractiveLineReader
  ConsoleReader
  (interactive-read-line [reader]
    (try (.readLine reader)
         (catch UnsupportedOperationException e "")))
  (prepare-for-next-read [reader]
    (flush-history (.getHistory reader))
    (when-let [completer (first (.getCompleters reader))]
      (.removeCompleter reader completer))))

(defn setup-console-reader
  [{:keys [prompt-string reader input-stream output-stream
           history-file blink-parens]
    :or {input-stream (FileInputStream. FileDescriptor/in)
         output-stream System/out
         prompt-string "=> "
         blink-parens true}
    :as state}]
  (let [reader (ConsoleReader. input-stream output-stream)
        file-history (FileHistory. (make-history-file history-file))
        history (try
                  (flush-history file-history)
                  file-history
                  (catch IOException e
                    (MemoryHistory.)))]
    (.setBlinkMatchingParen (.getKeys reader) blink-parens)
    (reset! current-console-reader reader)
    (doto reader
      (.setHistory history)
      (.setHandleUserInterrupt true)
      (.setExpandEvents false)
      (.setPaginationEnabled true)
      (.setPrompt prompt-string))))

(def jline-state (atom {}))

(defn get-input-line [state]
  (when-not (:reader state)
    (initialize-jline))
  (shutdown state)
  (if (:no-jline state)
    (assoc (dissoc state :no-jline)
           :reader nil
           :input (read-line))
    (let [reader (setup-console-reader state)
          input (try (interactive-read-line reader)
                  (catch jline.console.UserInterruptException e
                    :interrupted))]
      (prepare-for-next-read reader)
      (if (= :interrupted input)
        (assoc state
               :reader reader
               :input ""
               :interrupted true)
        (assoc state
               :reader reader
               :input input
               :interrupted nil)))))

(defn safe-read-line
  ([{:keys [prompt-string completer-factory no-jline input-stream output-stream
            history-file]
     :as options}]
   (swap! jline-state
          assoc
          :no-jline no-jline
          :history-file history-file
          :prompt-string prompt-string
          :completer-factory completer-factory)
   (when input-stream
     (swap! jline-state assoc :input-stream input-stream))
   (when output-stream
     (swap! jline-state assoc :output-stream output-stream))
   (swap! jline-state get-input-line)
   (if (:interrupted @jline-state) ;; TODO: don't do this same check in 2 places
     :interrupted
     (:input @jline-state)))
  ([completion-eval-fn
      {:keys [ns] :as state}]
     (safe-read-line state)))

(defn parse-text [text] 
  text)

(defn parsed-forms
  "Requires the following options:
  - request-exit: the value to return on completion/EOF
  - read-line-fn: a function that takes an options map that will include :ns
                and :prompt-string.
  - prompt-string: for customizing the prompt
  And returns a seq of *strings* representing complete forms."
  [{:keys [request-exit read-line-fn] :as options}]
  (if-let [next-text (read-line-fn options)]
     (let [interrupted? (= :interrupted next-text)
           parsed-text (when-not interrupted? (parse-text next-text))]
       (if interrupted?
         (list "")
         (list parsed-text)))
     (list request-exit)))

