(ns semserv.knowbase
  (:require [clojure.java.io :as io]
            [digest :refer [md5]])
  (:import [org.semanticweb.HermiT Reasoner]
           [org.semanticweb.owlapi.reasoner InferenceType]
           [org.semanticweb.owlapi.apibinding OWLManager]
           [org.semanticweb.owlapi.model IRI OWLClassExpression OWLIndividual
                                         OWLObjectPropertyExpression]
           [org.semanticweb.owlapi.search.EntitySearcher]))


(def ^:private mgr (OWLManager/createOWLOntologyManager))
(def ^:private df  (.getOWLDataFactory mgr))

(defmacro df-get [suffix & args]
  (let [method (symbol (str ".getOWL" (name suffix)))]
    `(~method df ~@args)))


(def ^:private kb-cache (atom {}))

(defn- build-kb [path]
  (let [file   (io/file path)
        onto   (.loadOntologyFromOntologyDocument mgr file)
        digest (md5 file)
        hermit (Reasoner. onto)]
    (.precomputeInferences hermit (into-array InferenceType ()))
    {:onto onto :hermit hermit :path path :digest digest}))

(defn get-kb [path]
  (let [abs-path (-> path io/file .getCanonicalPath)]
    (if-let [kb (get @kb-cache abs-path)]
      kb
      (-> (swap! kb-cache assoc abs-path (build-kb abs-path)) first second))))


(defn- iri [{:keys [hermit]} s]
  (IRI/create (try (.expandAbbreviatedIRI (.getPrefixes hermit) s)
                   (catch Exception _ s))))

(defn individual [kb s] (df-get NamedIndividual (iri kb s)))
(defn role       [kb s] (df-get ObjectProperty  (iri kb s)))
(defn concept    [kb s] (df-get Class           (iri kb s)))
(defn property   [kb s] (df-get DataProperty    (iri kb s)))


(defn top-role    [] (df-get TopObjectProperty))
(defn bottom-role [] (df-get BottomObjectProperty))

(defn invert [r] (df-get ObjectInverseOf r))

(defn everything [] (df-get Thing))
(defn nothing    [] (df-get Nothing))

(defn one [kb s]
  (df-get ObjectOneOf (into-array OWLIndividual [(individual kb s)])))

(defn unify [cs]
  (df-get ObjectUnionOf (into-array OWLClassExpression cs)))

(defn intersect [cs]
  (df-get ObjectIntersectionOf (into-array OWLClassExpression cs)))

(defn negate [c] (df-get ObjectComplementOf c))

(defn exists [r c] (df-get ObjectSomeValuesFrom r c))
(defn forall [r c] (df-get ObjectAllValuesFrom  r c))


(defn satisfiable [{:keys [hermit]} c] (.isSatisfiable hermit c))

(defn same [{:keys [hermit]} i j] (.isSameIndividual hermit i j))


(defn- flat [node-set] (-> node-set .getFlattened seq))

(defn query [{:keys [hermit]} c]
  (flat (.getInstances hermit c false)))

(defn project [{:keys [hermit]} i r]
  (flat (.getObjectPropertyValues hermit i r)))


(defn- entail [{:keys [hermit]} a b]
  (and (.isEntailed hermit a) (not (.isEntailed hermit b))))

(defn subtype [kb c d]
  (entail kb (df-get SubClassOfAxiom c d)
             (df-get SubClassOfAxiom c (negate d))))

(defn member [kb c i]
  (entail kb (df-get ClassAssertionAxiom c i)
             (df-get ClassAssertionAxiom (negate c) i)))


(defmacro defn-sig [sym infix]
  (let [method (symbol (str ".contains" (name infix) "InSignature"))]
    `(defn ~sym [kb# s#]
       (~method (:onto kb#) (iri kb# s#)))))

(defn-sig has-individual Individual)
(defn-sig has-role       ObjectProperty)
(defn-sig has-concept    Class)
(defn-sig has-property   DataProperty)


(def ^:private java-to-owl
  {"b" ["boolean"]
   "d" ["float" "double" "decimal"]
   "i" ["int" "long" "short" "byte" "integer" "positiveInteger"
        "negativeInteger" "nonPositiveInteger" "nonNegativeInteger"
        "unsignedLong" "unsignedShort" "unsignedInt" "unsignedByte"]
   "s" ["string" "normalizedString"]})

(def ^:private owl-to-java
  (letfn [(o->j [j os]
            (map #(vector (str "http://www.w3.org/2001/XMLSchema#" %) j) os))]
    (into {} (apply concat (map (partial apply o->j) java-to-owl)))))

(def ^:private type-parsers
  {"b" #(.parseBoolean %)
   "i" #(.parseInteger %)
   "f" #(.parseFloat   %)
   "d" #(.parseDouble  %)})

(defn- naturalize-literal [l]
  (let [type-key (get owl-to-java (-> l .getDatatype .toStringID))
        parse-fn (get type-parsers type-key #(.getLiteral %))]
    [(or type-key "s") (parse-fn l)]))

(defn appropriate [{:keys [hermit] :as kb} i p]
  (map naturalize-literal (.getDataPropertyValues hermit i p)))


(defn- extract-types [axiom]
  (try (str (.getIRI (.getRange axiom)))
       (catch Exception _ nil)))

(defn- merge-types [iris]
  (if (= (-> iris distinct count) 1)
    (first iris)
    nil))

(defn property-type [{:keys [onto]} p]
  (->> (.getDataPropertyRangeAxioms onto p)
       (map extract-types)
       (map (partial get owl-to-java))
       merge-types))
