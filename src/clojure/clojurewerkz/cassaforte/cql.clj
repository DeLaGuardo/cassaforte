(ns clojurewerkz.cassaforte.cql
  (:import [com.datastax.driver.core Session])
  (:use clojurewerkz.cassaforte.conversion)
  (:require [clojurewerkz.cassaforte.query :as query]
            [qbits.hayt.cql :as cql]
            [qbits.hayt.fns :as cql-fn]
            [clojurewerkz.cassaforte.client :as client]
            [clojurewerkz.cassaforte.debug-utils :as debug-utils]))

(def ^:dynamic *client*)
(def ^:dynamic *debug* false)

(defmacro with-client
  [^Session client & body]
  `(binding [*client* ~client]
     (do ~@body)))

(defmacro with-debug
  "Executes query with debug output"
  [& body]
  `(binding [*debug* true]
     (debug-utils/catch-exceptions ~@body)))

(defn connect
  "Connects to the C* client, returns Session"
  [h]
  (cond
   (vector? h) (client/connect h)))

(defn connect!
  "Connects to C* client, sets *client* as a default client"
  [h]
  (let [c (connect h)]
    (alter-var-root (var *client*) (constantly c))
    c))

;; Ability to turn on and off prepared statements by default? Turn on prepared statements on per-query basis
;; (macro+binding)

(defmacro prepared
  "Helper macro to execute prepared statement"
  [& body]
  `(binding [cql/*prepared-statement* true
             cql/*param-stack*        (atom [])]
     (do ~@body)))

(defn execute
  [query-params builder]
  (let [executor (if cql/*prepared-statement* client/execute-prepared client/execute)
        renderer (if cql/*prepared-statement* query/->prepared query/->raw)
        query    (->> query-params
                      flatten
                      (apply builder)
                      renderer)]
    (when *debug*
      (debug-utils/output-debug query))
    (executor *client* query)))

;;
;; Schema operations
;;

(defn drop-keyspace
  [ks]
  (execute [ks] query/drop-keyspace-query))

(defn create-keyspace
  [& query-params]
  (execute query-params query/create-keyspace-query))

(defn create-table
  [& query-params]
  (execute query-params query/create-table-query))

(def create-column-family create-table)

(defn drop-table
  [ks]
  (execute [ks] query/drop-table-query))

(defn use-keyspace
  [ks]
  (execute [ks] query/use-keyspace-query))

(defn alter-table
  [& query-params]
  (execute query-params query/alter-table-query))

(defn alter-keyspace
  [& query-params]
  (execute query-params query/alter-keyspace-query))

;;
;; DB Operations
;;

(defn insert
  [& query-params]
  (execute
   query-params
   query/insert-query))

(defn update
  [& query-params]
  (execute
   query-params
   query/update-query))

(defn delete
  [& query-params]
  (execute
   query-params
   query/delete-query))

(defn select
  [& query-params]
  (execute query-params query/select-query))

(defn truncate
  [table]
  (execute [table] query/truncate-query))

;;
;; Higher level DB functions
;;

;; TBD, add Limit
(defn get-one
  [& query-params]
  (execute query-params query/select-query))

(defn perform-count
  [table & query-params]
  (:count
   (first
    (select table
            (cons
             (query/columns (query/count*))
             query-params)))))

;;
;; Higher-level helper functions for schema
;;

(defn describe-keyspace
  [ks]
  (first
   (select :system.schema_keyspaces
           (query/where :keyspace_name ks))))

(defn describe-table
  [ks table]
  (first
   (select :system.schema_columnfamilies
           (query/where :keyspace_name ks
                        :columnfamily_name table))))

(defn describe-columns
  [ks table]
  (select :system.schema_columns
          (query/where :keyspace_name ks
                       :columnfamily_name table)))

;;
;; Higher-level collection manipulation
;;

(defn- get-chunk
  "Returns next chunk for the lazy world iteration"
  [table partition-key chunk-size last-pk]
  (if (nil? last-pk)
    (select table
            (query/limit chunk-size))
    (select table
            (query/where (cql-fn/token partition-key) [> (cql-fn/token last-pk)])
            (query/limit chunk-size))))

(defn iterate-world
  "Lazily iterates through the collection, returning chunks of chunk-size."
  ([table partition-key chunk-size]
     (iterate-world table partition-key chunk-size []))
  ([table partition-key chunk-size c]
     (lazy-cat c
               (let [last-pk    (get (last c) partition-key)
                     next-chunk (get-chunk table partition-key chunk-size last-pk)]
                 (iterate-world table partition-key chunk-size next-chunk)))))

(defn indexes-of [f coll] (keep-indexed #(if (f %2) %1) coll))

(defn- update-where
  [query-params f]


  )
(defn next-page-for
  (println (indexes-of #(= :where (ffirst %)) k))
  (update-in
   (vec k)
   [(indexes-of #(= :where (ffirst %)) k)]
   #(conj % '(:asd :bsd))
   )
  ;; (execute query-params query/select-query)
  )
