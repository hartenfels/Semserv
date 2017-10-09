(ns semserv.cache
  (:require [clojure.java.jdbc :as j]))


(def db {:classname   "org.sqlite.JDBC"
         :subprotocol "sqlite"
         :subname     (or (System/getenv "SEMSERV_CACHE") "cache.db")})


(defn- init-conn [existing]
  (or existing
      (let [conn (assoc db :connection (j/get-connection db))]
        (j/execute! conn "CREATE TABLE IF NOT EXISTS cache (
                         req    TEXT PRIMARY KEY NOT NULL,
                         res    TEXT NOT NULL,
                         digest TEXT NOT NULL)")
        conn)))

(def conn (atom nil))


(defn upsert! [table row where]
  (j/with-db-transaction [t @conn]
    (let [result (j/update! t table row where)]
      (if (zero? (first result))
        (j/insert! t table row)
        result))))

(defn- uncache [req digest]
  (first (j/query @conn ["SELECT res FROM cache WHERE req = ? AND digest = ?"
                         req digest] {:row-fn :res})))

(defn- cache [req res digest]
  (upsert! :cache {:req req :res res :digest digest} ["req = ?" req])
  res)


(defn with-cache [op kb args f]
  (swap! conn init-conn)
  (let [req    (str [(:path kb) op args])
        digest (:digest kb)
        cached (uncache req digest)]
    (if (nil? cached)
      (cache req (f op kb args) digest)
      cached)))
