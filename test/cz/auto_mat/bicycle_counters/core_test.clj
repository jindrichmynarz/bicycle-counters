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

(deftest detect-chronological-order-test
  (are [fixture expected] (= expected
                             (-> fixture
                                 json-fixture
                                 core/detect-chronological-order))
      ; Detections are sorted in the descending order.
      ; <https://gitlab.com/operator-ict/golemio/code/modules/bicycle-counters/-/blob/5aae87731c9978969ec3d0845de94e9aa1ea64bc/src/output-gateway/models/BicycleCountersDetectionsModel.ts#L67>
      "detections.json" :desc
      ; Temperatures are sorted in the ascending order.
      ; <https://gitlab.com/operator-ict/golemio/code/modules/bicycle-counters/-/blob/5aae87731c9978969ec3d0845de94e9aa1ea64bc/src/output-gateway/models/BicycleCountersTemperaturesModel.ts#L61>
      "temperatures.json" :asc)) 
