(ns commcare-cli.app_host
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as string]
            [clojure.stacktrace :as st]
            [commcare-cli.form_player :as form-player]
            [commcare-cli.helpers :as helpers])
  (:import [java.io ByteArrayInputStream BufferedInputStream FileInputStream FileNotFoundException]
           [org.commcare.core.parse CommCareTransactionParserFactory ParseUtils]
           [org.commcare.data.xml DataModelPullParser]
           [org.commcare.session SessionFrame]
           [org.commcare.util CommCareConfigEngine]
           [org.commcare.util.cli ApplicationHost MenuScreen EntityScreen]
           [org.commcare.util.mocks CLISessionWrapper MockUserDataSandbox]
           [org.javarosa.core.util.externalizable LivePrototypeFactory]
           [org.javarosa.core.services.locale Localization]
           [org.javarosa.core.services.storage StorageManager]
           [org.javarosa.core.model.instance FormInstance]))

(defrecord App [session engine sandbox])

(def today-date (atom nil))

(def ^:const
  HELP_MESSAGE
  (helpers/long-str
    ":exit / :quit - Terminate the session and close the CLI app"
    ":update (-f/-p) - Update the application live against the newest version on the server. --latest/-f optional flag to grab the newest build instead of the newest starred build. --preview/-p is for latest saved version of app"
    ":home - Navigate to the home menu of the app"
    ":lang <lang> - change the language to <lang> (e.g. :lang en)"
    ":today <date> - change the date returned by today()/now() (e.g. :today 2015-07-25). ':today' resets to today's date"
    ":stack - Show the stack of session frames"
    ":frame - Show the current session frame"
    ":help - Show this message"))

(defn build-restore-user-sandbox [prototype-factory restore-file]
  (let [sandbox (MockUserDataSandbox. prototype-factory)
        restore (try (BufferedInputStream. (FileInputStream. restore-file))
                     (catch FileNotFoundException e
                       (println "No restore file found at " restore-file)))]
    (.setAppFixtureStorageLocation sandbox (StorageManager/getStorage FormInstance/STORAGE_KEY))
    (ParseUtils/parseIntoSandbox restore sandbox, false)
    (.setLoggedInUser sandbox (.read (.getUserStorage sandbox) 0))
    sandbox))

(defn build-remote-user-sandbox [prototype-factory username password]
  (let [sandbox (MockUserDataSandbox. prototype-factory)]
    (.setAppFixtureStorageLocation sandbox (StorageManager/getStorage FormInstance/STORAGE_KEY))
    (ApplicationHost/restoreUserToSandbox sandbox username password)
    sandbox))

(defn install-app-with-restore [ccz restore-file]
  (let [prototype-factory (LivePrototypeFactory.)
        engine (CommCareConfigEngine. System/out prototype-factory)
        user-sandbox (build-restore-user-sandbox prototype-factory restore-file)]
    (App. (CLISessionWrapper. (.getPlatform engine) user-sandbox)
          (doto engine
            (.initFromArchive ccz)
            (.initEnvironment))
          user-sandbox)))

(defn install-app-with-creds [ccz username password]
  (let [prototype-factory (LivePrototypeFactory.)
        engine (CommCareConfigEngine. System/out prototype-factory)]
  (doto engine
              (.initFromArchive ccz)
              (.initEnvironment))
  (let [user-sandbox (build-remote-user-sandbox prototype-factory username password)]
      (App. (CLISessionWrapper. (.getPlatform engine) user-sandbox)
            engine
            user-sandbox))))

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

(defn print-frame-inner [steps]
  (doall
    (map
      (fn [step] (if (= (.getType step) SessionFrame/STATE_COMMAND_ID)
                   (println "COMMAND: " (.getId step))
                   (println "DATUM: " (.getId step) " - " (.getValue step))))
      steps)))

(defn print-frame [session]
  (let
    [frame (.getFrame session)
     steps (.getSteps frame)]
    (print-frame-inner steps)))

(defn print-stack [session]
  (let
    [frame-stack (.getFrameStack session)]
    (println "------- current frame -------")
    (print-frame session)
    (println "-----------------------------")
    (doall
      (map (fn [frame] (println "-------")
             (print-frame-inner (.getSteps frame))
             (println "-------")))
      frame-stack)))

;; String -> BuildType
;; where BuildType is one of
;; - :save, latest saved version of app
;; - :build, latest built version of app
;; - :release, latests starred version of app
(defn parse-build-type [arg]
  (cond
    (nil? arg) :release
    (or (string/includes? arg "--latest") (string/includes? arg "-f")) :build
    (or (string/includes? arg "--preview") (string/includes? arg "-p")) :save
    :else :release))

;; App BuildType -> None
(defn update-app [app build-type]
  (.clearAllState (:session app))
  (.attemptAppUpdate (:engine app) (name build-type)))

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
          [command arg] (map string/trim (string/split user-input #" " 2))]
      (cond
        (= command ":update")
        (do (update-app app (parse-build-type arg))
            :refresh)
        (= command ":home")
        (do (.clearAllState session)
            :refresh)
        (= command ":back")
        (do (.stepBack session (.getEvaluationContext session))
            :refresh)
        (= command ":stack")
        (do (print-stack session)
            :stay)
        (= command ":frame")
        (do (print-frame session)
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

;; Screen App -> Boolean
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

;; Session -> None
(defn finish-session [session]
  (.clearVolitiles session)
  (when (not (.finishExecuteAndPop session (.getEvaluationContext session)))
    (.clearAllState session)))

;; Sandbox Byte[] -> Boolean
(defn process-form-result [sandbox form-instance]
  (try
    (let [stream (ByteArrayInputStream. form-instance)]
      (.parse (DataModelPullParser. stream (CommCareTransactionParserFactory. sandbox) true true)))
    (catch Exception e (doall
                         (println "Error processing the form result: " (.getMessage e))
                         (st/print-stack-trace e)))))

;; App -> Boolean
(defn form-entry [app]
  (let [session (:session app)
        form-xmlns (.getForm session)
        locale nil] ; TODO: pass in locale
    (println "Starting form entry with the following stack frame")
    (print-frame session)
    (if (nil? form-xmlns)
      (do (finish-session session) true)
      (let [form-result (form-player/play
                          (.loadFormByXmlns (:engine app) form-xmlns)
                          session
                          locale
                          @today-date)
            form-not-cancelled? (not (nil? form-result))]
        (when (and form-not-cancelled?
                   (process-form-result (:sandbox app) form-result))
          (finish-session session))
        form-not-cancelled?))))

(defn nav-loop [app]
  (let [session (:session app)
        next-screen (get-next-screen session)]
    (if (nil? next-screen)
      (do (when (not (form-entry app))
            ;; form cancelled or errored out
            (.stepBack session (.getEvaluationContext session)))
          (recur app))
      (do
        (.init next-screen (:session app))
        (if (.shouldBeSkipped next-screen)
          (recur app)
          (when (process-screen next-screen app)
            (recur app)))))))
