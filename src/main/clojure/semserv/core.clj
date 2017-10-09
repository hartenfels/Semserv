(ns semserv.core
  (:gen-class)
  (:require [clojure.string :as string]
            [semserv.cache  :as sc]
            [semserv.interpret :refer [interpret]])
  (:import [java.io BufferedReader BufferedWriter InputStreamReader
                    OutputStreamWriter]
           [java.net ServerSocket]
           [java.nio.charset StandardCharsets]))


(def ^:private utf-8 StandardCharsets/UTF_8)

(defn- handle-line [out line]
  (.write out (interpret line))
  (.flush out))

(defn- server-thread [socket]
  (let [in  (-> socket .getInputStream  (InputStreamReader.  utf-8) BufferedReader.)
        out (-> socket .getOutputStream (OutputStreamWriter. utf-8) BufferedWriter.)]
    (try (run! (partial handle-line out) (line-seq in))
         (finally
           (.close socket)
           (println (str "# disconnect " (.getInetAddress socket)))))))


(defn- listen [port]
  (let [server (ServerSocket. port)]
    (println "# listen" (.getLocalPort server))
    (loop []
      (let [socket (.accept server)]
        (println (str "# connect " (.getInetAddress socket)))
        (-> (partial server-thread socket) Thread. .start)
        (recur)))))


(defn strip-margin [s]
  (-> s (string/replace #"(?m)^\s+\|" "") string/trim))

(defn -main [& args]
  (when (seq args)
    (.println *err* (strip-margin "
      |Semserv Clojure 1.0.0
      |
      |I don't take any arguments.
      |
      |Set the SEMSERV_PORT environment variable
      |if you want to use a different port than
      |the default port 53115.
      |
      |Set the SEMSERV_CACHE environment variable
      |to set the cache database file. This may be
      |a relative or absolute path, or \":memory:\"
      |for an in-memory database."))
    (System/exit 2))
  (listen (Integer/parseInt (or (System/getenv "SEMSERV_PORT") "53115"))))
