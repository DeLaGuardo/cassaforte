(ns clojurewerkz.cassaforte.cql
  (:import [com.datastax.driver.core Session Query
            ResultSet ResultSetFuture]
           [com.google.common.util.concurrent Futures FutureCallback])
  (:require
   [clojurewerkz.cassaforte.conversion :as conv]
   [qbits.hayt.cql :as cql]
   [clojurewerkz.cassaforte.query :as query]
   [clojurewerkz.cassaforte.client :as client]
   [clojurewerkz.cassaforte.debug-utils :as debug-utils]))

(def ^:dynamic *debug* false)
(def ^:dynamic *async* false)

(defmacro with-debug
  "Executes query with debug output"
  [& body]
  `(binding [*debug* true]
     (debug-utils/catch-exceptions ~@body)))

(defmacro async
  "Executes query with debug output"
  [& body]
  `(binding [*async* true]
     ~@body))

;; connect! can be replaced by client/set-session!

;; Ability to turn on and off prepared statements by default? Turn on prepared statements on per-query basis
;; (macro+binding)

(defmacro prepared
  "Helper macro to execute prepared statement"
  [& body]
  `(binding [cql/*prepared-statement* true
             cql/*param-stack*        (atom [])]
     (do ~@body)))

(defn render-query
  [query-params]
  (let [renderer (if cql/*prepared-statement* query/->prepared query/->raw)]
    (renderer query-params)))

(defn compile-query
  [query-params builder]
  (apply builder (flatten query-params)))

(defn execute
  "Executes built query"
  ([query]
     (execute client/*default-session* query))
  ([session query]
     (client/with-session session
       (let [^Query statement (if cql/*prepared-statement*
                                (client/build-statement (client/prepare (first query))
                                                        (second query))
                                (client/build-statement query))
             ^ResultSetFuture future (.executeAsync session statement)]
         (if *async*
           future
           (into [] (conv/to-map (.getUninterruptibly future))))))))

(defn set-callbacks
  [^ResultSetFuture future & {:keys [success failure]}]
  {:pre [(not (nil? success))]}
  (Futures/addCallback
   future
   (reify FutureCallback
     (onSuccess [_ result]
       (success
        (into []
              (conv/to-map (deref future)))))
     (onFailure [_ result]
       (failure result)))))

(defn get-result
  ([^ResultSetFuture future ^long timeout-ms]
     (into []
           (conv/to-map (.get future timeout-ms
                              java.util.concurrent.TimeUnit/MILLISECONDS))))
  ([^ResultSetFuture future]
     (into [] (conv/to-map (deref future)))))



(defn ^:private execute-
  [query-params builder]
  (let [rendered-query (render-query (compile-query query-params builder))]
    (when *debug*
      (debug-utils/output-debug rendered-query))
    (execute rendered-query)))

;;
;; Schema operations
;;

(defn drop-keyspace
  [ks]
  (execute- [ks] query/drop-keyspace-query))

(defn create-keyspace
  [& query-params]
  (execute- query-params query/create-keyspace-query))

(defn create-table
  [& query-params]
  (execute- query-params query/create-table-query))

(def create-column-family create-table)

(defn drop-table
  [ks]
  (execute- [ks] query/drop-table-query))

(defn use-keyspace
  [ks]
  (execute- [ks] query/use-keyspace-query))

(defn alter-table
  [& query-params]
  (execute- query-params query/alter-table-query))

(defn alter-keyspace
  [& query-params]
  (execute- query-params query/alter-keyspace-query))

;;
;; DB Operations
;;

(defn insert
  [& query-params]
  (execute-
   query-params
   query/insert-query))

(defn insert-batch
  [table records]
  (->> (map #(query/insert-query table %) records)
       (apply query/queries)
       query/batch-query
       render-query
       execute))

(defn update
  [& query-params]
  (execute-
   query-params
   query/update-query))

(defn delete
  [& query-params]
  (execute-
   query-params
   query/delete-query))

(defn select
  [& query-params]
  (execute- query-params query/select-query)
  )

(defn truncate
  [table]
  (execute- [table] query/truncate-query))

;;
;; Higher level DB functions
;;

;; TBD, add Limit
(defn get-one
  [& query-params]
  (execute- query-params query/select-query))

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
            (query/where (query/token partition-key) [> (query/token last-pk)])
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
