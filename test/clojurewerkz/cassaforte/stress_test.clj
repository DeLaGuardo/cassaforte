(ns clojurewerkz.cassaforte.stress-test
  (:require [clojurewerkz.cassaforte.test-helper :as th]
            [clojurewerkz.cassaforte.client :as client]
            [clojurewerkz.cassaforte.cql :refer :all]
            [clojure.test :refer :all]
            [clojurewerkz.cassaforte.query :refer :all]))

(use-fixtures :each th/initialize!)

(deftest insert-test
  (doseq [n [10 1000 10000]]
    (let [table  :users
          docs   (map (fn [i]
                        {:name (str "user_" i) :city (str "city_" i) :age (int i)})
                      (take n (iterate inc 1)))]
      (truncate table)
      (println "Inserting " n " documents...")
      (time (insert-batch table docs))
      (is (= n (perform-count table))))))

(deftest insert-test
  (doseq [n [10 1000 3000]]
    (let [table  :users
          docs   (map (fn [i]
                        {:name (str "user_" i) :city (str "city_" i) :age (int i)})
                      (take n (iterate inc 1)))]
      (truncate table)
      (println "Inserting " n " documents with prepared statement...")
      (time (client/prepared (insert-batch table docs)))
      (is (= n (perform-count table))))))
