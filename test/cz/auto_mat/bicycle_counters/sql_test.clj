(ns cz.auto-mat.bicycle-counters.sql-test
  (:require [clojure.test :refer :all]
            [cz.auto-mat.bicycle-counters.sql :as sql]))

(deftest wrap-quotes
  (testing "That tests are setup correctly"
    (is (sql/wrap-quotes "bork") "'bork'")))
