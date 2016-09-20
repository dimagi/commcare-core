(ns commcare-cli.helpers)

(defn clear-view []
  (doall
    (map (fn [x] (println " ")) (range 5))))

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
