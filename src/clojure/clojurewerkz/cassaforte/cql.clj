(ns clojurewerkz.cassaforte.cql
  (:require [clojurewerkz.cassaforte.client :as cc])
  (:use [clojure.string :only [split join]]
        [clojurewerkz.support.string :only [maybe-append to-byte-buffer]]
        [clojurewerkz.support.fn :only [fpartial]]
        [clojurewerkz.cassaforte.conversion :only [from-cql-prepared-result]])
  (:import clojurewerkz.cassaforte.CassandraClient
           java.util.List
           [org.apache.cassandra.thrift Compression CqlResult CqlRow CqlResultType]))


;;
;; Implementation
;;

(declare escape)

(def ^{:cons true :doc "Default compression level that is used for CQL queries"}
  default-compression (Compression/NONE))

(defprotocol CQLValue
  (to-cql-value [value] "Converts the given value to a CQL string representation"))

(extend-protocol CQLValue
  nil
  (to-cql-value [value]
    "null")

  Integer
  (to-cql-value [^Integer value]
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
    (escape value)))

(defn quote-identifier
  "Quotes the provided identifier"
  [identifier]
  (str "\"" (name identifier) "\""))

(defn quote
  "Quotes the provided string"
  [identifier]
  (str "'" (name identifier) "'"))


(def ^{:private true
       :const true}
  placeholder-pattern #"\?")

(defn interpolate-vals
  [^String query & vals]
  (let [segments (split query placeholder-pattern)]
    ))

(def ^{:private true}
  maybe-append-semicolon (fpartial maybe-append ";"))

(defn- clean-up
  "Cleans up the provided query string by trimming it and appending the semicolon if needed"
  [^String query]
  (-> query .trim maybe-append-semicolon))


(defn prepare-cql-query
  "Prepares a CQL query for execution. Cassandra 1.1+ only."
  ([^String query]
     (from-cql-prepared-result (.prepare_cql_query ^CassandraClient cc/*cassandra-client* (to-byte-buffer query) default-compression)))
  ([^String query ^Compression compression]
     (from-cql-prepared-result (.prepare_cql_query ^CassandraClient cc/*cassandra-client* (to-byte-buffer query) compression))))

(defn execute-prepared-query
  "Executes a CQL query previously prepared using the prepare-cql-query function
   by providing the actual values for placeholders"
  [^long prepared-statement-id ^List values]
  (.execute_prepared_cql_query ^CassandraClient cc/*cassandra-client* prepared-statement-id values))



;;
;; API
;;

(defn escape
  [^String value]
  ;; TODO
  value)

(defn ^org.apache.cassandra.thrift.CqlResult
  execute-raw
  "Executes a CQL query given as a string. No argument replacement (a la JDBC) is performed."
  ([^String query]
     (.executeCqlQuery ^CassandraClient cc/*cassandra-client* (-> query clean-up)))
  ([^String query ^Compression compression]
     (.executeCqlQuery ^CassandraClient cc/*cassandra-client* (-> query clean-up) compression)))

(defn execute
  ([^String query]
     (comment TODO))
  ([^String query & args]
     (let [{:keys [id] :as stmt} (prepare-cql-query query)
           result                (execute-prepared-query id args)]
       result)))




(defn void-result?
  "Returns true if the provided CQL query result is of type void (carries no result set)"
  [^CqlResult result]
  (= (.getType result) CqlResultType/VOID))

(defn int-result?
  "Returns true if the provided CQL query result is of type int (carries a single numerical value)"
  [^CqlResult result]
  (= (.getType result) CqlResultType/INT))

(defn rows-result?
  "Returns true if the provided CQL query result carries a result set"
  [^CqlResult result]
  (= (.getType result) CqlResultType/ROWS))