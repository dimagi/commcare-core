(ns commcare-cli.form_player
  (:require [commcare-cli.helpers :as helpers]
            [commcare-cli.repl :as repl]
            [commcare-cli.answer_input :as answer]
            [clojure.stacktrace :as st]
            [clojure.string :as string])
  (:import [java.io IOException]
           [org.javarosa.core.model Constants]
           [org.javarosa.core.model.data SelectMultiData]
           [org.javarosa.core.model.condition EvaluationContext]
           [org.javarosa.core.model.trace StringEvaluationTraceSerializer]
           [org.javarosa.engine XFormEnvironment]
           [org.javarosa.form.api FormEntrySession FormEntryController
            FormEntrySessionReplayer FormEntrySessionReplayer$ReplayError]
           [org.javarosa.model.xform XFormSerializingVisitor]
           [org.javarosa.xpath XPathNodeset XPathParseTool]
           [org.javarosa.xpath.expr FunctionUtils]
           [org.javarosa.xpath.parser XPathSyntaxException]))

(def debug-mode? (atom false))

(def ^:const
  HELP_MESSAGE
  (helpers/long-str
    ":next - Move to next question"
    ":back - Move to previous question"
    ":quit / :cancel - Exit form without processing it"
    ":finish - Finish and process form"
    ":print <instance> - Print xml of a data instance. e.g. ':print jr://instance/casedb'"
    ":eval <expr> - Evaluate an xpath expression. If empty, enter eval mode"
    ":entry-session - Shows current form entry session. You can copy this string and restore to that spot in form entry using the ':replay' command."
    ":replay <sesssion-string> - Replays the form entry session provided, which should be acquired by the 'entry-session' command. Safest to run this command right when you open the form."
    ":relevant - Prints the evaluation trace of the expression for the current question's display condition"
    ":debug - Toggles debug mode off/on. When debug mode is enabled, the evaluation trace for any xpath expressions requested will be printed after the value is printed."))

(defn print-choice [is-multi-select? is-selected? index choice-text]
  (println
    (if is-multi-select? (string/join `("[" ~(if is-selected? "X" " ") "]")) "")
    (+ 1 index)
    ")"
    choice-text))

(defn show-choices [entry-prompt choices]
  (let [is-multi-select? (= Constants/CONTROL_SELECT_MULTI (.getControlType entry-prompt))
        indices (range (count choices))
        choices-text (map (fn [c] (.getSelectChoiceText entry-prompt c)) choices)
        selected-choices (answer/get-selected-choices entry-prompt choices)
        is-selected? (map (fn [c] (some #{c} selected-choices)) choices)]
    (doall
      (map
        (partial print-choice is-multi-select?)
        is-selected?
        indices
        choices-text))))

(defn show-question [entry-prompt]
  (println (.getQuestionText entry-prompt))
  (let [choices (.getSelectChoices entry-prompt)]
    (when (not (nil? choices))
      (show-choices entry-prompt choices))
    (when (= Constants/CONTROL_TRIGGER (.getControlType entry-prompt))
      (println "Press Return to proceed"))))

(defn new-repeat-question []
  (println "Add new repeat?")
  (println "1) Yes, add a new repeat group")
  (println "2) No, continue to the next question"))

;; FormEntryController [FormEntryController -> None] -> None
(defn show-event [entry-controller step-func]
  (helpers/clear-view)
  (let [event (.getEvent (.getModel entry-controller))]
    (cond
      (= event FormEntryController/EVENT_BEGINNING_OF_FORM) (println "Form Start: Press Return to proceed")
      (= event FormEntryController/EVENT_END_OF_FORM) (println "Form End: Press Return to Complete Entry")
      (= event FormEntryController/EVENT_GROUP) (do (step-func entry-controller)
                                                    (show-event entry-controller step-func))
      (= event FormEntryController/EVENT_QUESTION) (show-question (.getQuestionPrompt (.getModel entry-controller)))
      (= event FormEntryController/EVENT_REPEAT) (do (step-func entry-controller)
                                                     (show-event entry-controller step-func))
      (= event FormEntryController/EVENT_REPEAT_JUNCTURE) (println "Repeats Not Implemented, press return to exit")
      (= event FormEntryController/EVENT_PROMPT_NEW_REPEAT) (new-repeat-question))))

(defn display-relevant [entry-model]
  (let [debug-info
        (.getDebugInfo entry-model
                       (.getFormIndex entry-model)
                       "relevant"
                       (StringEvaluationTraceSerializer.))]
    (if (nil? debug-info)
      (println "No display logic defined")
      (println debug-info))))

;; FormEntryController String -> NavAction
(defn replay-entry-session [entry-controller command]
  (if (string/blank? command)
    (println "Invalid command, please provide session string to replay")
    (try
      (do (FormEntrySessionReplayer/tryReplayingFormEntry
            entry-controller
            (FormEntrySession/fromString command))
          :forward)
      (catch FormEntrySessionReplayer$ReplayError e
        (println "Error replaying form: " (.getMessage e))
        (println "Aborting form entry")
        :exit))))

(defn get-eval-ctx [entry-model in-debug-mode?]
  (let [pre-eval-ctx (.getEvaluationContext (.getForm entry-model))
        current-index (.getFormIndex entry-model)
        eval-ctx (if (.isInForm current-index)
                   (EvaluationContext. pre-eval-ctx (.getReference current-index))
                   pre-eval-ctx)]
      (when in-debug-mode? (.setDebugModeOn eval-ctx))
      eval-ctx))

(defn print-result [value in-debug-mode? eval-ctx]
  (println (if (instance? XPathNodeset value)
             (FunctionUtils/getSerializedNodeset (cast XPathNodeset value))
             (FunctionUtils/toString value)))
  (when (and in-debug-mode? (not (nil? (.getEvaluationTrace eval-ctx))))
    (println (.serializeEvaluationLevels
               (StringEvaluationTraceSerializer.)
               (.getEvaluationTrace eval-ctx)))))

(defn eval-expr [entry-controller in-debug-mode? raw-expr]
  (try
    (let [expr (XPathParseTool/parseXPath raw-expr)
          eval-ctx (get-eval-ctx (.getModel entry-controller) in-debug-mode?)]
      (print-result (.eval expr eval-ctx) in-debug-mode? eval-ctx))
    (catch XPathSyntaxException e
      (println "Parse error: " (.getMessage e)))
    (catch Exception e
      (st/print-stack-trace e)
      (println "Eval error: " (.getMessage e)))))

(defn eval-mode [entry-controller input]
  (if (string/blank? input)
    (repl/start-repl (partial eval-expr entry-controller @debug-mode?) ".eval")
    (eval-expr entry-controller @debug-mode? input)))

;; FormDef -> (or Byte[] Nil)
(defn get-main-instance [form-def]
  (try (let [instance (.serializeInstance (XFormSerializingVisitor.) (.getInstance form-def))]
         (helpers/ppxml (String. instance))
         instance)
       (catch IOException e
         (doall (println "Error serializing XForm")
                nil))))

(defn print-instance [entry-controller instance]
  (if (nil? instance)
    (get-main-instance (.getForm (.getModel entry-controller)))
    (println "TODO: implement print instance for non-main instance")))

;; FormEntryController String -> NavAction
;; where NavAction is one of [:forward :back :exit :finish]
(defn process-command [entry-controller user-input]
  (let [[command arg] (map string/trim (string/split user-input #" " 2))]
    (cond
      (= command ":help") (do (println HELP_MESSAGE) :forward)
      (= command ":next") (do (.stepToNextEvent entry-controller) :forward)
      (= command ":back") (do (.stepToPreviousEvent entry-controller) :back)
      (= command ":quit") :exit
      (= command ":cancel") :exit
      (= command ":finish") :finish
      (= command ":print") (do (print-instance entry-controller arg) :forward)
      (= command ":eval") (do (eval-mode entry-controller arg) :forward)
      (= command ":replay") (replay-entry-session entry-controller arg)
      (= command ":entry-session") (do
                                     (println (.getFormEntrySessionString entry-controller))
                                     :forward)
      (= command ":relevant") (do
                                (display-relevant (.getModel entry-controller))
                                :forward)
      (= command ":debug") (do
                             (swap! debug-mode? not @debug-mode?)
                             (println "Expression debuggion: "
                                      (if @debug-mode? "ENABLED" "DISABLED"))
                             :forward)
      :else (do (println "Invalid command: " command) :forward))))

;; FormEntryController -> (or Byte[] Nil)
(defn process-form [entry-controller]
  (let [form (.getForm (.getModel entry-controller))]
    (.postProcessInstance form)
    (helpers/clear-view)
    (get-main-instance form)))

;; FormEntryController Boolean -> (or Byte[] Nil)
(defn process-loop [entry-controller forward?]
  (show-event entry-controller
              (fn [entry-controller] (if forward?
                                       (.stepToNextEvent entry-controller)
                                       (.stepToPreviousEvent entry-controller))))
  (let [user-input (read-line)
        next-action (if (string/starts-with? user-input ":")
                      (process-command entry-controller user-input)
                      (answer/answer-question entry-controller user-input))]
    (cond (or (= next-action :forward) (= next-action :back)) (recur entry-controller (= next-action :forward))
          (= next-action :finish) (process-form entry-controller)
          (= next-action :exit) (doall (println "Exit without saving")
                                       nil))))

;; FormDef CommCareSession String String -> (or Byte[] Nil)
(defn play [form-def session locale today-date]
  (let [env (XFormEnvironment. form-def)]
    (when (not (nil? locale))
      (.setLocale env locale))
    (when (not (nil? today-date))
      (println today-date)
      (.setToday env today-date))
    (let [entry-controller (.setup env (.getIIF session))]
      (process-loop entry-controller true))))

