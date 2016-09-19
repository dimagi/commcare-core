(defproject commcare-cli "0.1.0-SNAPSHOT"
  :description "CLI wrapper for CommCare"
  :url "http://www.dimagi.com/products/"
  :license {:name "Apache License V2"
            :url "http://www.apache.org/licenses/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [jline "2.14.2"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.commcare/commcare "2.31.0"]]
  :aot [commcare-cli.reader.jline.JlineInputReader]
  :plugins [[lein-localrepo "0.5.3"]]
  :main ^{:skip-aot true} commcare-cli.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
