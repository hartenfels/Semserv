(ns semserv.interpret
  (:require [clojure.data.json :as json]
            [clojure.java.io   :as io]
            [clojure.string    :as string]
            [scjsv.core        :as scjsv]
            [semserv.cache     :as sc]
            [semserv.knowbase  :as sk]))


(def ^:private scjsv-validate
  (-> "semserv.schema.json" io/resource slurp scjsv/validator))

(defn- validate [data]
  (seq (filter #(= (:level %) "error") (scjsv-validate data))))


(defn- tag-or-nil [_ x]
  (if (instance? clojure.lang.Seqable x) (-> x first keyword) nil))

(defmulti ^:private on-role tag-or-nil)

(defmethod on-role nil [_ r] (if r (sk/top-role) (sk/bottom-role)))

(defmethod on-role :r [kb [_ s]] (sk/role kb s))

(defmethod on-role :i [kb [_ r]] (sk/invert (on-role kb r)))


(defmulti ^:private on-concept tag-or-nil)

(defmethod on-concept nil [_ c] (if c (sk/everything) (sk/nothing)))

(defmethod on-concept :C [kb [_ s]] (sk/concept kb s))
(defmethod on-concept :O [kb [_ s]] (sk/one     kb s))

(defn- map-concepts [kb cs] (map (partial on-concept kb) cs))

(defmethod on-concept :U [kb [_ cs]] (sk/unify     (map-concepts kb cs)))
(defmethod on-concept :I [kb [_ cs]] (sk/intersect (map-concepts kb cs)))

(defmethod on-concept :N [kb [_ c]] (sk/negate (on-concept kb c)))

(defmethod on-concept :E [kb [_ r c]] (sk/exists (on-role kb r) (on-concept kb c)))
(defmethod on-concept :A [kb [_ r c]] (sk/forall (on-role kb r) (on-concept kb c)))


(defn- keywordize-first [s & _] (keyword s))

(defmulti ^:private on-signature keywordize-first)

(defmethod on-signature :individual [_ kb s] (sk/has-individual kb s))
(defmethod on-signature :role       [_ kb s] (sk/has-role       kb s))
(defmethod on-signature :concept    [_ kb s] (sk/has-concept    kb s))
(defmethod on-signature :property   [_ kb s] (sk/has-property   kb s))


(defn- id [i] (.toStringID i))

(defn- lit [l]
  (cond (.isBoolean l) ["b" (.parseBoolean l)]
        (.isInteger l) ["i" (.parseInteger l)]
        (.isFloat   l) ["f" (.parseFloat   l)]
        (.isDouble  l) ["d" (.parseDouble  l)]
        :else          ["s" (.getLiteral   l)]))

(defmulti ^:private on-op keywordize-first)

(defmethod on-op :individual [_ kb s]
  (id (sk/individual kb s)))

(defmethod on-op :satisfiable [_ kb c]
  (sk/satisfiable kb (on-concept kb c)))

(defmethod on-op :same [_ kb [[i j]]]
  (sk/same kb (sk/individual kb i) (sk/individual kb j)))

(defmethod on-op :query [_ kb c]
  (map id (sk/query kb (on-concept kb c))))

(defmethod on-op :project [_ kb [i r]]
  (map id (sk/project kb (sk/individual kb i) (on-role kb r))))

(defmethod on-op :appropriate [_ kb [i p]]
  (map lit (sk/appropriate kb (sk/individual kb i) (sk/property kb p))))

(defmethod on-op :subtype [_ kb [c d]]
  (sk/subtype kb (on-concept kb c) (on-concept kb d)))

(defmethod on-op :member [_ kb [c i]]
  (sk/member kb (on-concept kb c) (sk/individual kb i)))

(defmethod on-op :signature [_ kb [kind s]]
  (on-signature kind kb s))


(defn- start-op [op kb args]
  (json/write-str (locking kb (on-op op kb args))))

(defn- evaluate [[path op args]]
  (let [kb (sk/get-kb path)]
    (sc/with-cache op kb args start-op)))

(defn- respond [line]
  (try
    (let [req (json/read-str line)]
      (if-let [errors (validate req)]
        (json/write-str {:errors errors})
        (evaluate req)))
    (catch Exception e
      (json/write-str {:exception (str e)}))))

(defn interpret [line]
  (if (string/blank? line)
    ""
    (str (respond line) "\n")))
