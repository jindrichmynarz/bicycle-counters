(ns cz.auto-mat.bicycle-counters.core
  (:require [cz.auto-mat.bicycle-counters.api :as api]
            [cz.auto-mat.bicycle-counters.sql :as sql]
            [clojure.string :as string]
            [taoensso.timbre :as log])
  (:import (clojure.lang PersistentVector)
           (java.time ZonedDateTime ZoneId)
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

(defn comparison->order
  "Maps a result of `comparison` to either ascending or descending order."
  [^Integer comparison]
  (if (pos? comparison)
    :desc
    :asc))

(def offsets
  "What offsetting attributes to use for ascending and descending order?"
  ; Ascending order should be traversed using :from offset using the value of :measured_to.
  {:asc {:attribute :from
         :value :measured_to}
   ; Descending order should be traversed using :to offset using the value of :measured_from.
   :desc {:attribute :to
          :value :measured_from}})

(defn offset-fn
  "Convert offset configuration into an offsetting function."
  [{:keys [attribute value]}]
  (comp (partial hash-map attribute) value))

(defn detect-chronological-order
  "Detect chronological order of `response`."
  [^PersistentVector response]
  (when (seq response)
    (->> response
         (map :measured_from) ; Order can be detected either on :measured_from or :measured_to.
         (take 2) ; We assume the order is the same for all results.
         (apply compare)
         comparison->order)))

(defn chronological-order->offset-attribute
  "Map chronological order of events in `response` to an offset function."
  [^PersistentVector response]
  (when (seq response)
    (->> response
         detect-chronological-order
         offsets
         offset-fn)))

(defn chronological-chaining
  "Offset function that uses measured_from time of the last detection in `response`
  as the next request's offset."
  [^PersistentVector response]
  (let [offset-attribute (chronological-order->offset-attribute response)]
    (-> response
        peek
        offset-attribute)))

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
         (api/request-offsetted api-endpoint chronological-chaining)
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
