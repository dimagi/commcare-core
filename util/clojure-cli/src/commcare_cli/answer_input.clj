(ns commcare-cli.answer_input
  (:require [commcare-cli.helpers :as helpers]
            [clojure.string :as string])
  (:import [org.javarosa.form.api FormEntryController]
           [org.javarosa.core.model Constants]
           [org.javarosa.core.model.data AnswerDataFactory SelectMultiData UncastData]))

(defn set-question-answer [entry-controller string-value move-forward?]
  (try
    (let [entry-prompt (.getQuestionPrompt (.getModel entry-controller))
          value (.cast (AnswerDataFactory/template
                         (.getControlType entry-prompt)
                         (.getDataType entry-prompt))
                       (UncastData. string-value))
          response (.answerQuestion entry-controller value)]
      (cond
        (and move-forward? (= response FormEntryController/ANSWER_OK)) (.stepToNextEvent entry-controller)
        (= response FormEntryController/ANSWER_REQUIRED_BUT_EMPTY) (println "Answer is required")
        (= response FormEntryController/ANSWER_CONSTRAINT_VIOLATED) (println (.getConstraintText entry-prompt))))
    (catch Exception e
      (println (.getMessage e)))))

;; FormEntryPrompt [List-of SelectChoice] -> [List-of SelectChoice]
(defn get-selected-choices [entry-prompt choices]
  (let [raw-answer-data (.getAnswerValue entry-prompt)]
    (java.util.Vector.
      (if (nil? raw-answer-data)
        '()
        (let [selections (.cast (SelectMultiData.) (.uncast raw-answer-data))]
          (filter (fn [choice] (.isInSelection selections (.getValue choice))) choices))))))

;; FormEntryPrompt -> String
(defn get-selected-choices-string [entry-prompt]
  (let [choices (.getSelectChoices entry-prompt)
        choice-selections (get-selected-choices entry-prompt choices)
        selections (if (empty? choice-selections)
                     choice-selections
                     (java.util.Vector. (map #(.selection %) choice-selections)))]
    (.getString (.uncast (SelectMultiData. selections)))))

;; FormEntryPrompt String -> String
(defn join-selections [entry-prompt new-select]
  (let [current-selection (get-selected-choices-string entry-prompt)]
    (string/join #" "
                 (distinct (cons new-select
                                 (string/split current-selection #" "))))))

;; FormEntryPrompt String [List-of SelectChoice] -> [(or String Nil) Direction]
(defn process-choice-input [entry-prompt user-input choices]
  (let [question-type (.getControlType entry-prompt)]
    (if (and (string/blank? user-input) (= Constants/CONTROL_SELECT_MULTI question-type))
      `(~(get-selected-choices-string entry-prompt) :forward)
      (let [index (helpers/validate-number-input user-input (count choices))]
        (cond
          (= index -1) '(nil :forward)
          (= Constants/CONTROL_SELECT_ONE question-type) `(~(.getValue (nth choices index)) :forward)
          (= Constants/CONTROL_SELECT_MULTI question-type) `(~(join-selections entry-prompt (.getValue (nth choices index))) :stay))))))

;; FormEntryController String -> [(or String Nil) Direction]
(defn process-input [entry-controller user-input]
  (let [entry-prompt (.getQuestionPrompt (.getModel entry-controller))
        choices (.getSelectChoices entry-prompt)]
    (if (not (nil? choices))
      (process-choice-input entry-prompt user-input choices)
      `(~user-input :forward))))

;; FormEntryController String -> Direction
(defn answer-question-event [entry-controller user-input]
    (let [[string-value direction] (process-input entry-controller user-input)
          move-forward? (= direction :forward)]
      (when (not (nil? string-value))
        (set-question-answer entry-controller string-value move-forward?))
      :forward))

(defn create-new-repeat [entry-controller user-input]
  (let [i (helpers/validate-number-input user-input 2)]
    (cond
      (= 1 i) (doto entry-controller (.newRepeat) (.stepToNextEvent))
      (= 2 i) (.stepToNextEvent entry-controller))))

;; FormEntryController String -> NavAction
(defn answer-question [entry-controller user-input]
  "Answer's current question with user input. Returns :finish to process form,
  :exit to quit form, :forward to next question"
  (let [event (.getEvent (.getModel entry-controller))]
    (cond
      (= event FormEntryController/EVENT_BEGINNING_OF_FORM) (do (.stepToNextEvent entry-controller) :forward)
      (= event FormEntryController/EVENT_END_OF_FORM) :finish
      (= event FormEntryController/EVENT_QUESTION) (answer-question-event entry-controller user-input)
      (= event FormEntryController/EVENT_REPEAT) :exit
      (= event FormEntryController/EVENT_REPEAT_JUNCTURE) :exit
      (= event FormEntryController/EVENT_PROMPT_NEW_REPEAT) (do (create-new-repeat entry-controller user-input) :forward)
      :else (doall (println "Bad state; quitting") false))))
