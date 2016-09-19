(ns commcare-cli.app_host
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as string]
            [commcare-cli.dispatch :as dispatch]
            [commcare-cli.form_player :as form-player]
            [commcare-cli.helpers :as helpers])
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

(defrecord App [session engine sandbox])

(def today-date (atom nil))

(defn long-str [& strings] (string/join "\n" strings))
(def ^:const
  HELP_MESSAGE
  (long-str
    ":exit / :quit - Terminate the session and close the CLI app"
    ":update (-f) - Update the application live against the newest version on the server. -f optional flag to grab the newest build instead of the newest starred build"
    ":home - Navigate to the home menu of the app"
    ":lang <lang> - change the language to <lang> (e.g. :lang en)"
    ":stack - Show the current session frame stack"
    ":help - Show this message"))

(defn build-user-sandbox [prototype-factory restore-file]
  (let [sandbox (MockUserDataSandbox. prototype-factory)
        restore (try (BufferedInputStream. (FileInputStream. restore-file))
                     (catch FileNotFoundException e
                       (println "No restore file found at " restore-file)))]
    (.setAppFixtureStorageLocation sandbox (StorageManager/getStorage FormInstance/STORAGE_KEY))
    (ParseUtils/parseIntoSandbox restore sandbox, false)
    (.setLoggedInUser sandbox (.read (.getUserStorage sandbox) 0))
    sandbox))

(defn install-app [ccz restore-file username password]
  (let [prototype-factory (LivePrototypeFactory.)
        engine (CommCareConfigEngine. System/out prototype-factory)
        user-sandbox (build-user-sandbox prototype-factory restore-file)]
    (App. (CLISessionWrapper. (.getPlatform engine) user-sandbox)
          (doto engine
            (.initFromArchive ccz)
            (.initEnvironment))
          user-sandbox)))

(defn get-next-screen [session]
  (let [eval-context (.getEvaluationContext session)
        needed (.getNeededData session eval-context)]
    (cond
      (nil? needed) nil
      (= needed SessionFrame/STATE_COMMAND_ID) (MenuScreen.)
      (= needed SessionFrame/STATE_DATUM_VAL) (EntityScreen.)
      (= needed SessionFrame/STATE_DATUM_COMPUTED)
      (do
        (.setComputedDatum session)
        (recur session))
      :else (throw (RuntimeException. "Unexpected frame request")))))

(defn print-stack [session]
  (let
    [frame (.getFrame session)
     steps (.getSteps frame)]
    (doall
      (map
        (fn [step] (if (= (.getType step) SessionFrame/STATE_COMMAND_ID)
                     (println "COMMAND: " (.getId step))
                     (println "DATUM: " (.getId step) " - " (.getValue step))))
        steps))))

(defn set-locale [locale]
  (if (string/blank? locale)
    (println "Command format\n:lang [langcode]")
    (let [localizer (Localization/getGlobalLocalizerAdvanced)
          available-locales (.getAvailableLocales localizer)]
      (if (nil? (some #{locale} available-locales))
        (println "Locale '" locale "' is undefined. Available locales:")
        (map (fn [l] (println "* " l)) available-locales)))))

;; String App Screen -> Action
;; where Action is one of [:quit :refresh :stay]
(defn process-command [user-input app]
  (if (= user-input ":exit")
    :quit
      (let [session (:session app)
            args (string/split user-input #" ")
            command (nth args 0)
            arg (if (> (count args) 1) (nth args 1) nil)]
        (cond
          (= command ":update")
          (do (println "TODO: app implement update")
              :stay)
          (= command ":home")
          (do (.clearAllState session)
              :refresh)
          (= command ":back")
           (do (.stepBack session (.getEvaluationContext session))
               :refresh)
          (= command ":stack")
           (do (print-stack session)
               :stay)
          (= command ":lang")
           (do (set-locale arg)
               :stay)
          (= command ":today")
           (do (reset! today-date arg)
               :stay)
          (= command ":help")
          (do
            (println HELP_MESSAGE)
            :stay)))))

(defn process-screen [screen app]
  (helpers/clear-view)
  (println (.getWrappedDisplaytitle screen (:sandbox app) (.getPlatform (:engine app))))
  (println "==================")
  (.prompt screen System/out)
  (let [user-input (read-line)]
    (if (string/starts-with? user-input ":")
      (let [action (process-command user-input app)]
        (cond (= action :quit) false
              (= action :refresh) true
              (= action :stay) (recur screen app)))
      (if
        (.handleInputAndUpdateSession screen (:session app) user-input)
        (recur screen app)
        true))))

;; Session -> Boolean
(defn finish-session [session]
  (.clearVolitiles session)
  (.finishExecuteAndPop session (.getEvaluationContext session)))

;; App -> Boolean
(defn form-entry [app]
  (let [session (:session app)
        form-xmlns (.getForm session)
        locale nil] ; TODO: pass in locale
    (if (nil? form-xmlns)
      (finish-session)
      (form-player/play
        (.loadFormByXmlns (:engine app) form-xmlns)
        session
        locale
        @today-date))))

(defn nav-loop [app]
  (let [next-screen (get-next-screen (:session app))]
    (if (nil? next-screen)
      (form-entry app)
      (do
        (.init next-screen (:session app))
        (if (.shouldBeSkipped next-screen)
          (recur app)
          (when (process-screen next-screen app)
            (recur app)))))))
