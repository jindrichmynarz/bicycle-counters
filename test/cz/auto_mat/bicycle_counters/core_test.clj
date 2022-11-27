(ns cz.auto-mat.bicycle-counters.core-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [cz.auto-mat.bicycle-counters.core :as core]))

(defn- json-fixture
  "Load a `json-file` from test resources."
  [^String json-file]
  (with-open [reader (-> json-file
                         io/resource
                         io/reader)]
    (json/parse-stream-strict reader true))) 

(deftest chronological-order->offset-attribute-test
  (are [fixture expected] (= expected
                             (-> fixture
                                 json-fixture
                                 core/chronological-order->offset-attribute))
      ; Detections are sorted in the descending order.
      "detections.json" {:attribute :to
                         :value :measured_from}
      ; Temperatures are sorted in the ascending order.
      "temperatures.json" {:attribute :from
                           :value :measured_to}))
