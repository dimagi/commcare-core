(ns commcare-cli.helpers)

(defn clear-view []
  (doall
    (map (fn [x] (println " ")) (range 5))))

