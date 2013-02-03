(ns clojurewerkz.cassaforte.bytes-test
  (:use clojurewerkz.cassaforte.bytes
        clojure.test)
  (:import [java.nio ByteBuffer]))

(deftest t-serializer-roundtrip
  (are [type value]
       (= value (deserialize type (to-bytes (encode value))))
       "Int32Type"(Integer. 1)
       "IntegerType" (java.math.BigInteger. "123456789")
       "LongType" (Long. 100)
       "UTF8Type" "some fancy string"
       "AsciiType" "some fancy string"
       "BooleanType" true
       "BooleanType" false
       "DateType" (java.util.Date.)
       "DoubleType" (java.lang.Double. "123"))

  (is (= ["a" "b" "c"])
      (map
       #(deserialize "UTF8Type" %)
       (.fromByteBuffer composite-serializer (encode (composite "a" "b" "c")))))

  (let [cs (get-serializer ["a" "b" "c"])
        serialized (encode ["a" "b" "c"])]
    (is (= ["a" "b" "c"]) (.compose cs serialized))))
