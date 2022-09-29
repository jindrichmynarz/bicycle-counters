(ns cz.auto-mat.bicycle-counters.core
  (:require [cz.auto-mat.bicycle-counters.api :as api]
            [cz.auto-mat.bicycle-counters.sql :as sql]
            [clojure.string :as string]
            [taoensso.timbre :as log])
  (:import (java.time ZonedDateTime ZoneId)
           (java.time.temporal ChronoUnit)
           (java.time.format DateTimeFormatter)))

(def maximum-time
  ; TODO: How long does the validation take? Currently, we expect that if the validation starts at 5 AM, it is complete by 6 AM.
  "Data from bicycle counters are validated every day as 5:00 AM UTC.
  Since data can change during validation, we want to fetch only the data
  that was already validated.
  Source:
  https://golemioapi.docs.apiary.io/#reference/traffic/bicycle-counters/get-bicycle-counters-detections"
  (let [now (ZonedDateTime/now (ZoneId/of "UTC"))
        today-start (.truncatedTo now ChronoUnit/DAYS)
        threshold (if (.isAfter now (.plusHours today-start 6)) ; When data for yesterday was already validated
                    today-start
                    (.minusDays today-start 1))]
    (.format threshold DateTimeFormatter/ISO_OFFSET_DATE_TIME)))

(defn bicycle-counters
  "Get bicycle counters data"
  []
  (api/request "/bicyclecounters"))

(defn bicycle-counter-ids
  "Extract bicycle counter IDs from `data`."
  [data]
  (->> data
       :features
       (map (comp :id :properties))))

(defn bicycle-counter-direction-ids
  "Extract bicycle counter direction IDs from `data`."
  [data]
  (->> data
       :features
       (mapcat (comp :directions :properties))
       (keep :id))) ; Ignore bicycle counters that don't have directional cameras

(defn reverse-cmp
  "Reverse comparator"
  [a b]
  (compare b a))

(defn reverse-chronological-chaining
  [response]
  "Offset function that uses measured_from time of the oldest detection in `response`
  as the measured_to offset."
  (when (seq response)
    (->> response
         ; Not all API responses are sorted in the descending order, such as:
         ; <https://gitlab.com/operator-ict/golemio/code/modules/bicycle-counters/-/blob/5aae87731c9978969ec3d0845de94e9aa1ea64bc/src/output-gateway/models/BicycleCountersTemperaturesModel.ts#L61>
         ; So we re-sort them to make sure.
         (sort-by :measured_from reverse-cmp)
         last
         :measured_from
         (hash-map :to))))

(defn- bicycle-counter-events
  "`events` is also an SQL table name."
  [^String events
   ^String id]
  (let [measured-from (sql/maximum-time events id)
        query-params (cond-> {:id id :to maximum-time}
                       measured-from (assoc :from measured-from))
        api-endpoint (str "/bicyclecounters/" events)]
    (log/infof "Getting bicycle counter %s from %s from %s."
               events
               id
               (or measured-from "all time"))
    (->> query-params
         (api/request-offsetted api-endpoint reverse-chronological-chaining)
         (filter :value)))) ; Filter out null values

(defn bicycle-counter-detections
  "Get detections of the bicycle counter in the direction
  identified as `bicycle-counter-direction-id`."
  [bicycle-counter-direction-id]
  (log/infof "Bicycle counter direction ID = %s" bicycle-counter-direction-id)
  (bicycle-counter-events "detections" bicycle-counter-direction-id))

(defn bicycle-counter-temperatures
  "Get temperature measurements from a bicycle counter
  identifier as `bicycle-counter-id`."
  [bicycle-counter-id]
  (bicycle-counter-events "temperatures" bicycle-counter-id))

(defn bicycle-counters-detections
  "Get detections for bicycle counters in `data`."
  [data]
  (->> data
       bicycle-counter-direction-ids
       (mapcat bicycle-counter-detections)))

(defn- camea?
  "Is the bicycle counter with `bicycle-counter-id` a product of Camea?"
  [bicycle-counter-id]
  (string/starts-with? bicycle-counter-id "camea-"))

(defn bicycle-counters-temperatures
  "Get temperatures for bicycle counters in `data`."
  [data]
  (->> data
       bicycle-counter-ids
       (filter camea?) ; Temperatures are available only from Camea bicycle counters
       (mapcat bicycle-counter-temperatures)))
