(ns commcare-cli.app_host
  (:require [clojure.tools.cli :as cli]
            [commcare-cli.dispatch :as dispatch])
  (:import [java.io FileInputStream BufferedInputStream FileNotFoundException]
           [org.commcare.util CommCareConfigEngine]
           [org.commcare.util.cli MenuScreen EntityScreen]
           [org.commcare.core.parse ParseUtils]
           [org.javarosa.core.util.externalizable LivePrototypeFactory]
           [org.javarosa.core.services.storage StorageManager]
           [org.javarosa.core.model.instance FormInstance]
           [org.commcare.session SessionFrame]
           [org.commcare.util.mocks CLISessionWrapper MockUserDataSandbox]))

(defrecord App [session engine sandbox])

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

(defn clear-view []
  (map (fn [x] (println "\n")) (range 5)))

(defn process-screen [screen app] 
  (clear-view)
  (.getWrappedDisplaytitle screen (:sandbox app) (.getPlatform (:engine app)))
  (.prompt screen System/out)
  (let [user-input (read-line)]
    (when 
      ; TODO: handle commands
      (.handleInputAndUpdateSession screen (:session app) user-input)
      (recur screen app))))

(defn form-entry []
  (println "TODO form entry"))

(defn nav-loop [app]
  (let [next-screen (get-next-screen (:session app))]
    (if (nil? next-screen)
      (form-entry)
      (do
        (.init next-screen (:session app))
        (if (.shouldBeSkipped next-screen)
          (recur app)
          (do 
            (process-screen next-screen app)
            (recur app)))))))
