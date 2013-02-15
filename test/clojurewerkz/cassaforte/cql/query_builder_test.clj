(ns clojurewerkz.cassaforte.cql.query-builder-test
  (:use clojure.test
        clojurewerkz.cassaforte.cql.query-builder
        clojurewerkz.cassaforte.utils))

(deftest test-prepare-create-column-family-query
  (is (= "CREATE TABLE libraries (name varchar, language varchar, rating double, PRIMARY KEY (name));"
         (prepare-create-column-family-query "libraries"
                                             {:name :varchar :language :varchar :rating :double }
                                             :primary-key :name)))
  (is (= "CREATE TABLE posts (content text, posted_at timestamp, entry_title text, userid text, PRIMARY KEY (userid, posted_at));"
         (prepare-create-column-family-query "posts"
                                             {:userid :text :posted_at :timestamp :entry_title :text :content :text}
                                             :primary-key [:userid :posted_at])))

  (is (= "CREATE TABLE libraries (name varchar, language varchar, rating double);"
         (prepare-create-column-family-query "libraries"
                                             {:name :varchar :language :varchar :rating :double }))))

(deftest test-prepare-drop-column-family-query
  (is (= "DROP TABLE libraries;"
         (prepare-drop-column-family-query "libraries"))))

(deftest test-prepare-insert-query
  (is (= "INSERT INTO libraries (name, language, rating) VALUES ('name', 'language', 1.0) USING CONSISTENCY ONE AND TTL 100;"
         (prepare-insert-query "libraries" {:name "name" :language "language" :rating 1.0}
                               :consistency "ONE"
                               :ttl 100)))

  (is (= [["name" "language" 1.0] "INSERT INTO libraries (name, language, rating) VALUES (?, ?, ?) USING CONSISTENCY ONE AND TTL 100;"]
         (prepare-insert-query "libraries" {:name "name" :language "language" :rating 1.0}
                               :consistency "ONE"
                               :ttl 100
                               <:as-prepared-statement true))))

(deftest t-prepare-update-query
  (is (= "UPDATE libraries SET name = 'name', language = 'language', rating = 1.0 WHERE name = 'name'"
         (prepare-update-query "libraries" {:name "name" :language "language" :rating 1.0}
                               :where {:name "name"})))

  (is (= [["name" "language" 1.0 "name"]
          "UPDATE libraries SET name = ?, language = ?, rating = ? WHERE name = ?"]
         (prepare-update-query "libraries" {:name "name" :language "language" :rating 1.0}
                               :where {:name "name"}
                               :as-prepared-statement true)))
  )

(deftest test-prepare-create-index-query
  (is (= "CREATE INDEX column_family_name_column_name_idx ON column_family_name (column_name)"
         (prepare-create-index-query "column_family_name" "column_name"))))


;;
;; Conversion to CQL values, escaping
;;

(deftest ^{:cql true} test-conversion-to-cql-values
  (are [val cql] (is (= cql (to-cql-value val)))
    10  "10"
    nil "null"
    10N "10"
    :age "age"))


(deftest test-prepare-select-query
  (is (= "SELECT * FROM column_family_name"
         (prepare-select-query "column_family_name")))
  (is (= "SELECT column_name_1, column_name_2 FROM column_family_name"
         (prepare-select-query "column_family_name" :columns ["column_name_1" "column_name_2"])))
  (is (= "SELECT * FROM column_family_name WHERE key = 1"
         (prepare-select-query "column_family_name" :where {:key 1})))

  (is (= "SELECT * FROM column_family_name WHERE key = 1 ORDER BY second_key"
         (prepare-select-query "column_family_name" :where {:key 1} :order :second_key)))
  (is (= "SELECT * FROM column_family_name WHERE key = 1 ORDER BY second_key"
         (prepare-select-query "column_family_name" :where {:key 1} :order [:second_key])))
  (is (= "SELECT * FROM column_family_name WHERE key = 1 ORDER BY second_key ASC"
         (prepare-select-query "column_family_name" :where {:key 1} :order [:second_key :asc])))
  (is (= "SELECT * FROM column_family_name WHERE key = 1 ORDER BY second_key DESC"
         (prepare-select-query "column_family_name" :where {:key 1} :order [:second_key :desc])))

  (is (thrown? Exception
               (prepare-select-query "column_family_name" :where {:key 1} :order [:second_key :desc :third_key])))

  (is (= "SELECT * FROM column_family_name WHERE key IN (1, 2, 3)"
         (prepare-select-query "column_family_name" :where {:key [:in [1 2 3]]})))
  (is (= "SELECT * FROM column_family_name WHERE key > 1"
         (prepare-select-query "column_family_name" :where {:key [> 1]})))
  (is (= "SELECT * FROM column_family_name WHERE column_1 >= 1 AND column_1 <= 5 AND column_2 >= 1"
         (prepare-select-query "column_family_name" :where {:column_1 [>= 1 <= 5] :column_2 [>= 1]})))
  (is (= "SELECT * FROM column_family_name WHERE column_1 >= 1 AND column_2 <= 5"
         (prepare-select-query "column_family_name" :where {:column_1 [>= 1] :column_2 [<= 5]})))
  (is (= "SELECT * FROM column_family_name WHERE column_1 >= 1 AND column_2 <= 5 LIMIT 5"
         (prepare-select-query "column_family_name" :where {:column_1 [>= 1] :column_2 [<= 5]} :limit 5))))


(deftest test-prepared-statments
  (is (= [[1 5] "SELECT * FROM column_family_name WHERE column_1 >= ? AND column_2 <= ? LIMIT 5"]
         (prepare-select-query "column_family_name" :where {:column_1 [>= 1] :column_2 [<= 5]} :limit 5
                             :as-prepared-statement true))))

(deftest test-to-cql-value
  (are [expected value] (= expected (to-cql-value value))
       "1" 1
       "1.0" (float 1)
       "'str'" "str"
       "[1, 2, 3]" [1 2 3]))
