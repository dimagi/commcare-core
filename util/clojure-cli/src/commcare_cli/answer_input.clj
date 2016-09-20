(ns commcare-cli.answer_input
  (:require [commcare-cli.helpers :as helpers]
            [clojure.string :as string])
  (:import [org.javarosa.form.api FormEntryController]
           [org.javarosa.core.model Constants]
           [org.javarosa.core.model.data AnswerDataFactory SelectMultiData UncastData]))

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
        (= response FormEntryController/ANSWER_CONSTRAINT_VIOLATED) (println (.getConstraintText entry-prompt))))
    (catch Exception e
      (println (.getMessage e)))))

;; FormEntryPrompt [List-of SelectChoice] -> [List-of SelectChoice]
(defn get-selected-choices [entry-prompt choices]
  (let [raw-answer-data (.getAnswerValue entry-prompt)]
    (if (nil? raw-answer-data)
      '()
      (let [selections (.cast (SelectMultiData.) (.uncast raw-answer-data))]
        (filter (fn [choice] (.isInSelection selections (.getValue choice))) choices)))))


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
        (let [index (helpers/validate-number-input user-input (count choices))]
          (cond (= Constants/CONTROL_SELECT_ONE question-type) (.getValue (nth choices index))
                ;; TODO: does this let you select multiple things?
                (= Constants/CONTROL_SELECT_MULTI question-type) (.getValue (nth choices index)))))
      user-input)))

;; FormEntryController String -> NavAction
(defn answer-question-event [entry-controller user-input]
    (let [string-value (process-input entry-controller user-input)]
      (set-question-answer entry-controller string-value)
      :forward))

(defn create-new-repeat [entry-controller user-input]
  (let [i (helpers/validate-number-input user-input 2)]
    (cond
      (= 1 i) (doto entry-controller (.newRepeat) (.stepToNextEvent))
      (= 2 i) (.stepToNextEvent entry-controller))))

;; FormEntryController String -> NavAction
(defn answer-question [entry-controller user-input]
  (let [event (.getEvent (.getModel entry-controller))]
    (cond
      (= event FormEntryController/EVENT_BEGINNING_OF_FORM) (do (.stepToNextEvent entry-controller) :forward)
      (= event FormEntryController/EVENT_END_OF_FORM) :finish
      (= event FormEntryController/EVENT_QUESTION) (answer-question-event entry-controller user-input)
      (= event FormEntryController/EVENT_REPEAT) :exit
      (= event FormEntryController/EVENT_REPEAT_JUNCTURE) :exit
      (= event FormEntryController/EVENT_PROMPT_NEW_REPEAT) (do (create-new-repeat entry-controller user-input) :forward)
      :else (doall (println "Bad state; quitting") false))))
