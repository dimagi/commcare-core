(ns commcare-cli.form_player
  (:require [commcare-cli.helpers :as helpers]
            [clojure.string :as string])
  (:import [java.io FileInputStream BufferedInputStream FileNotFoundException]
           [org.commcare.util CommCareConfigEngine]
           [org.commcare.util.cli MenuScreen EntityScreen]
           [org.commcare.core.parse ParseUtils]
           [org.javarosa.core.util.externalizable LivePrototypeFactory]
           [org.javarosa.core.services.locale Localization]
           [org.javarosa.core.services.storage StorageManager]
           [org.javarosa.core.model.instance FormInstance]
           [org.commcare.session SessionFrame]
           [org.commcare.util.mocks CLISessionWrapper MockUserDataSandbox]))

(defn print-choice [is-multi-select? is-selected? index choice-text]
  (println
    (when is-multi-select? (string/join "[" (if is-selected? "X" " ") "]"))
    (+ 1 index)
    ") "
    choices-text))

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
        choices-text (map .getSelectChoiceText choices)
        selected-choices (get-selected-choices entry-prompt choices)
        is-selected? (map (fn [c] (some #{c} selected-choices)) choices)]
    (doall
      (map
        (partial print-choice is-multi-select?)
        is-selected?
        indices
        choices-text))))

(defn show-question [entry-prompt]
  (println (.getQuestionPrompt entry-prompt))
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
  (case (getEvent (.getModel entry-controller))
    FormEntryController/EVENT_BEGINNING_OF_FORM (println "Form Start: Press Return to proceed")
    FormEntryController/EVENT_END_OF_FORM (println "Form End: Press Return to Complete Entry")
                ;;mProcessOnExit = true;
    FormEntryController/EVENT_GROUP (do (step-func entry-controller)
                                        (show-event entry-controller step-func))
    FormEntryController/EVENT_QUESTION (show-question (.getQuestionPrompt (.getModel entry-controller)))
    FormEntryController/EVENT_REPEAT (do (step-func entry-controller)
                                         (show-event entry-controller step-func))
    FormEntryController/EVENT_REPEAT_JUNCTURE (println "Repeats Not Implemented, press return to exit")
    FormEntryController/EVENT_PROMPT_NEW_REPEAT (new-repeat-question)
  ))

(defn process-command [user-input]
  "TODO")

(defn answer-question [user-input]
  "TODO")

(defn process-loop [entry-controller]
  (show-event entry-controller
              ;; TODO: detect when user steps back w/ ':back'
              (fn [entry-controller] (entry-controller stepToNextEvent)))
  (let [user-input (read-line)]
    (when (if (string/starts-with? user-input ":")
            (process-command user-input)
            (answer-question user-input))
      (recur entry-controller))))

;; FormDef CommCareSession String -> None
(defn play [form-def session locale]
  (let [env (.XFormEnvironment form-def nil)]
    (when (not (nil? locale))
      (.setLocale env locale))
    (let [entry-controller (.setup env (.getIIF session iif))]
      (process-loop entry-controller))))

