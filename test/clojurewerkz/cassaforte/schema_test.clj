(ns clojurewerkz.cassaforte.schema-test
  (:require [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.schema :as sch]
            [clojurewerkz.cassaforte.cql    :as cql])
  (:use clojure.test
        clojurewerkz.cassaforte.test-helper
        clojurewerkz.cassaforte.conversion
        clojurewerkz.cassaforte.utils))

(use-fixtures :once initialize-cql)

(deftest ^{:schema true} test-add-and-drop-keyspace
  (let [keyspace       "CassaforteTest2"
        strategy-class "org.apache.cassandra.locator.SimpleStrategy"
        strategy-opts  {:replication_factor "1"}
        cf-defs        [(build-column-family-definition keyspace "movies")]]
    (is (sch/add-keyspace keyspace strategy-class cf-defs :strategy-opts strategy-opts))
    (is (sch/drop-keyspace keyspace))))


(deftest ^{:schema true :indexes true} test-create-columnfamily-bare-cql
  (with-thrift-exception-handling
    (cql/drop-column-family "libraries")
    (cql/execute-raw "CREATE COLUMNFAMILY libraries (name     varchar,
                                                   language varchar,
                                                  PRIMARY KEY (name))")
    (is (cql/void-result? (sch/create-index "libraries" "language")))
    (is (cql/void-result? (sch/drop-index "libraries" "language")))
    (is (cql/void-result? (sch/create-index "libraries" "language" "by_language")))
    (is (cql/void-result? (sch/drop-index "by_language")))
    (cql/drop-column-family "libraries")))
