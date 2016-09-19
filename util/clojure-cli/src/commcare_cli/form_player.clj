(ns commcare-cli.form_player
  (:require [commcare-cli.helpers :as helpers]
            [commcare-cli.repl :as repl]
            [clojure.stacktrace :as st]
            [clojure.string :as string])
  (:import [java.io FileInputStream BufferedInputStream FileNotFoundException]
           [org.commcare.util CommCareConfigEngine]
           [org.commcare.util.cli MenuScreen EntityScreen]
           [org.commcare.core.parse ParseUtils]
           [org.javarosa.core.util.externalizable LivePrototypeFactory]
           [org.javarosa.core.services.locale Localization]
           [org.javarosa.core.services.storage StorageManager]
           [org.javarosa.core.model Constants]
           [org.javarosa.core.model.data AnswerDataFactory SelectMultiData UncastData]
           [org.javarosa.core.model.condition EvaluationContext]
           [org.javarosa.core.model.instance FormInstance]
           [org.javarosa.core.model.trace StringEvaluationTraceSerializer]
           [org.javarosa.engine XFormEnvironment]
           [org.javarosa.form.api FormEntrySession FormEntryController 
            FormEntrySessionReplayer FormEntrySessionReplayer$ReplayError]
           [org.javarosa.xpath XPathNodeset XPathParseTool]
           [org.javarosa.xpath.expr XPathFuncExpr]
           [org.javarosa.xpath.parser XPathSyntaxException]
           [org.commcare.session SessionFrame]
           [org.commcare.util.mocks CLISessionWrapper MockUserDataSandbox]))

(def debug-mode? (atom false))

(defn print-choice [is-multi-select? is-selected? index choice-text]
  (println
    (when is-multi-select? (string/join "[" (if is-selected? "X" " ") "]"))
    (+ 1 index)
    ") "
    choice-text))

;; FormEntryPrompt [List-of SelectChoice] -> [List-of SelectChoice]
(defn get-selected-choices [entry-prompt choices]
  (let [raw-answer-data (.getAnswerValue entry-prompt)]
    (if (nil? raw-answer-data)
      '()
      (let [selections (.cast (SelectMultiData.) (.uncast raw-answer-data))]
        (filter (fn [choice] (.isInSelection selections (.getValue choice))) choices)))))

(defn show-choices [entry-prompt choices]
  (let [is-multi-select? (= Constants/CONTROL_SELECT_MULTI (.getControlType entry-prompt))
        indices (range (count choices))
        choices-text (map (fn [c] (.getSelectChoiceText entry-prompt c)) choices)
        selected-choices (get-selected-choices entry-prompt choices)
        is-selected? (map (fn [c] (some #{c} selected-choices)) choices)]
    (doall
      (map
        (partial print-choice is-multi-select?)
        is-selected?
        indices
        choices-text))))

(defn show-question [entry-prompt]
  (println entry-prompt)
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
      ;;mProcessOnExit = true;
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

(defn replay-entry-session [entry-controller command]
  (if (string/blank? command)
    (println "Invalid command, please provide session string to replay")
    (try
      (do (FormEntrySessionReplayer/tryReplayingFormEntry
            entry-controller
            (FormEntrySession/fromString command))
          false)
      (catch FormEntrySessionReplayer$ReplayError e
        (println "Error replaying form: " (.getMessage e))
        (println "Aborting form entry")
        true))))

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
             (XPathFuncExpr/getSerializedNodeset (cast XPathNodeset value))
             (XPathFuncExpr/toString value)))
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

(defn eval-mode [entry-controller command]
  (let [input (string/trim command)]
    (if (string/blank? input)
      (repl/start-repl (partial eval-expr entry-controller @debug-mode?))
      (eval-expr entry-controller input @debug-mode?))))

(defn process-command [entry-controller command]
  (cond
    (= command "next") (do (.stepToNextEvent entry-controller) true)
    (= command "back") (do (.stepToPreviousEvent entry-controller) true)
    (= command "quit") false ;; TODO
    (= command "cancel") false ;; TODO
    (= command "finish") false ;; TODO
    (= command "print") false ;; TODO
    (string/starts-with? command "eval") (do (eval-mode
                                               entry-controller
                                               (subs command 4))
                                             true)
    (string/starts-with? command "replay") (replay-entry-session
                                             (string/trim (subs command 6)))
    (= command "entry-session") (println (.getFormEntrySessionString entry-controller))
    (= command "relevant") (display-relevant (.getModel entry-controller))
    (= command "debug") (do (swap! debug-mode? not @debug-mode?)
                            (println "Expression debuggion: "
                                     (if @debug-mode? "ENABLED" "DISABLED")))
    :else (println "Invalid command: " command)
  ))

(defn validate-number-input [user-input max-number]
  (try
    (let [i (Integer/parseInt user-input)]
      (if (or (< i 1) (> i max-number))
        (doall
          (println "Enter a number between 1 and " max-number)
          -1)
        (- i 1)))
    (catch NumberFormatException e
      (doall
        (println "Enter a number between 1 and " max-number)
        -1))))

(defn set-question-answer [entry-controller string-value]
  (try
    (let [entry-prompt (.getQuestionPrompt (.getModel entry-controller))
          value (.cast (AnswerDataFactory/template
                         (.getControlType entry-prompt)
                         (.getDataType entry-prompt))
                       (UncastData. string-value))
          response (.answerQuestion entry-controller value)]
      (cond
        (= response FormEntryController/ANSWER_OK) (.stepToNextEvent entry-controller)
        (= response FormEntryController/ANSWER_REQUIRED_BUT_EMPTY) (println "Answer is required")
        (= response FormEntryController/ANSWER_CONSTRAINT_VIOLATED) (println (.getConstraintText entry-controller))))
    (catch Exception e
      (println (.getMessage e)))))

(defn get-selected-choices-string [entry-prompt]
  (let [choices (.getSelectChoices entry-prompt)
        selections (get-selected-choices entry-prompt choices)]
    (.getString (.uncast (SelectMultiData. selections)))))

(defn process-input [entry-controller user-input]
  (let [entry-prompt (.getQuestionPrompt (.getModel entry-controller))
        question-type (.getControlType entry-prompt)
        choices (.getSelectChoices entry-prompt)]
    (if (not (nil? choices))
      (if (and (string/blank? user-input) (= Constants/CONTROL_SELECT_MULTI question-type))
        (get-selected-choices-string entry-prompt)
        (let [index (validate-number-input user-input choices)]
          (cond (= Constants/CONTROL_SELECT_ONE question-type) (.getValue (nth choices index))
                ;; TODO: does this let you select multiple things?
                (= Constants/CONTROL_SELECT_MULTI question-type) (.getValue (nth choices index)))))
      user-input)))

(defn answer-question-event [entry-controller user-input]
    (let [string-value (process-input entry-controller user-input)]
      (set-question-answer entry-controller string-value)
      true))

(defn create-new-repeat [entry-controller user-input]
  (let [i (validate-number-input user-input 2)]
    (cond
      (= 1 i) (doto entry-controller (.newRepeat) (.stepToNextEvent))
      (= 2 i) (.stepToNextEvent entry-controller))))

(defn answer-question [entry-controller user-input]
  (let [event (.getEvent (.getModel entry-controller))]
    (cond
      (= event FormEntryController/EVENT_BEGINNING_OF_FORM) (do (.stepToNextEvent entry-controller) true)
      (= event FormEntryController/EVENT_END_OF_FORM) false ;; TODO mProcessOnExit = true;
      (= event FormEntryController/EVENT_QUESTION) (answer-question-event entry-controller user-input)
      (= event FormEntryController/EVENT_REPEAT) false 
      (= event FormEntryController/EVENT_REPEAT_JUNCTURE) false
      (= event FormEntryController/EVENT_PROMPT_NEW_REPEAT) (do (create-new-repeat entry-controller user-input) true)
      :else (doall (println "Bad state; quitting") false))))

(defn process-loop [entry-controller]
  (show-event entry-controller
              ;; TODO: detect when user steps back w/ ':back'
              (fn [entry-controller] (.stepToNextEvent entry-controller)))
  (let [user-input (read-line)]
    (when (if (string/starts-with? user-input ":")
            (process-command entry-controller (subs user-input 1))
            (answer-question entry-controller user-input))
      (recur entry-controller))))

;; FormDef CommCareSession String String -> None
(defn play [form-def session locale today-date]
  (let [env (XFormEnvironment. form-def)]
    (when (not (nil? locale))
      (.setLocale env locale))
    (when (not (nil? today-date))
      (println today-date)
      (.setToday env today-date))
    (let [entry-controller (.setup env (.getIIF session))]
      (process-loop entry-controller))))

