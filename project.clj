(defproject semserv "1.1.0-dev"
  :description "Semserv - Server for Semantics4J"
  :url         "https://github.com/hartenfels/Semserv"

  :license {:name "Apache License, Version 2.0"
            :url  "https://www.apache.org/licenses/LICENSE-2.0"}

  :dependencies [[org.clojure/clojure                        "1.8.0"]
                 [org.clojure/data.json                      "0.2.6"]
                 [com.hermit-reasoner/org.semanticweb.hermit "1.3.8.4"]
                 [metosin/scjsv                              "0.4.0"]
                 [digest                                     "1.4.6"]
                 [org.clojure/java.jdbc                      "0.7.3"]
                 [org.xerial/sqlite-jdbc                     "3.20.0"]]

  :main ^:skip-aot semserv.core
  :source-paths   ["src/main/clojure"]
  :resource-paths ["src/main/resources"]
  :target-path    "target/%s"
  :profiles {:uberjar {:aot :all}})
