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

(defn ppxml [xml]
  (let [in (javax.xml.transform.stream.StreamSource.
             (java.io.StringReader. xml))
        writer (java.io.StringWriter.)
        out (javax.xml.transform.stream.StreamResult. writer)
        transformer (.newTransformer 
                      (javax.xml.transform.TransformerFactory/newInstance))]
    (.setOutputProperty transformer 
                        javax.xml.transform.OutputKeys/INDENT "yes")
    (.setOutputProperty transformer 
                        "{http://xml.apache.org/xslt}indent-amount" "2")
    (.setOutputProperty transformer 
                        javax.xml.transform.OutputKeys/METHOD "xml")
    (.transform transformer in out)
    (println (-> out .getWriter .toString))))
