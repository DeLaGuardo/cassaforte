(ns clojurewerkz.cassaforte.query
  (:use [clojure.string :only [split join trim escape upper-case]]
        [clojurewerkz.support.string :only [maybe-append to-byte-buffer interpolate-vals interpolate-kv]]))

(defprotocol CQLValue
  (to-cql-value [value] "Converts the given value to a CQL string representation"))

(extend-protocol CQLValue
  nil
  (to-cql-value [value]
    "null")

  Number
  (to-cql-value [^Number value]
    (str value))

  Long
  (to-cql-value [^Long value]
    (str value))

  clojure.lang.BigInt
  (to-cql-value [^clojure.lang.BigInt value]
    (str value))

  clojure.lang.Named
  (to-cql-value [^clojure.lang.Named value]
    (name value))

  String
  (to-cql-value [^String value]
    (str "'" (escape value {\" "\""}) "'")))

(def primary-key-clause
  ", PRIMARY KEY (:column)")

(def create-query
  "CREATE TABLE :column-family-name (:column-definitions:primary-key-clause);")

(def drop-column-family-query
  "DROP TABLE :column-family-name;")

(defn prepare-create-column-family-query
  [column-family column-definitions & {:keys [primary-key]}]
  (interpolate-kv create-query
                  {:column-family-name column-family
                   :column-definitions (trim (join ", " (map (fn [[k v]] (str (name k) " " (name v))) column-definitions) ))
                   :primary-key-clause (when primary-key
                                         (interpolate-kv primary-key-clause {:column (name primary-key)}))}))

(defn prepare-drop-column-family-query
  [column-family]
  (interpolate-kv drop-column-family-query {:column-family-name column-family}))


(def insert-query
  "INSERT INTO :column-family-name (:names) VALUES (:values):opts;")

(def opts-clause
  " USING ?")

(defn prepare-insert-query
  [column-family m & {:keys [consistency timestamp ttl] :or {consistency "LOCAL_QUORUM"} :as opts}]
  (interpolate-kv insert-query
                  {:column-family-name column-family
                   :names (trim (join ", " (map #(name %) (keys m))))
                   :values (trim (join ", " (map #(to-cql-value %) (vals m))))
                   :opts (when opts
                           (interpolate-vals
                            opts-clause
                            [(join " AND " (map (fn [[k v]] (str (upper-case (name k)) " " v)) opts))]))}))

(def index-name-clause
  " :index-name")

(def create-index-query
  "CREATE INDEX:index-name ON :column-family-name (:column-name)")

(defn prepare-create-index-query
  ([column-family column-name]
     (prepare-create-index-query column-family column-name nil))
  ([column-family column-name index-name]
     (interpolate-kv create-index-query
                     {:index-name (when index-name
                                    (interpolate-kv index-name-clause {:index-name index-name}))
                      :column-family-name column-family
                      :column-name (name column-name)})))
