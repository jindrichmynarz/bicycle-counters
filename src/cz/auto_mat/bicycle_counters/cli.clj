(ns cz.auto-mat.bicycle-counters.cli
  (:gen-class)
  (:require [cz.auto-mat.bicycle-counters.core :as core]
            [cz.auto-mat.bicycle-counters.io :as io]
            [cz.auto-mat.bicycle-counters.serialize :as serialize]
            [clojure.java.io :as jio]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [environ.core :refer [env]]
            [mount.core :as mount]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]))

; ----- Private functions -----

(defn- long-str
  [& lines]
  (string/join \newline lines))

(defn- error-message
  [errors]
  (long-str "The following errors occurred while parsing your command:"
            \newline
            (apply long-str errors)))

(defn- die
  "Fail and exit with `message`."
  [message]
  (binding [*out* *err*] (println message))
  (System/exit 1))

(defn- info
  "Exit with `message`."
  [message]
  (println message)
  (System/exit 0))

(defn- usage
  [summary]
  (long-str "Harvests data from Prague bicycle counters via the Golemio API."
            "Options:"
            summary))

(defn- remove-measurement-count
  "measurement_count is relevant only when aggregation is used."
  [result]
  (dissoc result :measurement_count))

(defn- main
  []
  (log/info "Downloading data from bicycle counters.")
  (let [data (core/bicycle-counters)]
    (->> data
         serialize/bicycle-counters
         (io/upsert-data! "bicycle_counters"))
    (->> data
         serialize/bicycle-counter-directions
         (io/upsert-data! "bicycle_counter_directions"))
    (->> data
         core/bicycle-counters-detections
         (map remove-measurement-count)
         (io/upsert-data! "bicycle_counter_detections"))
    (->> data
         core/bicycle-counters-temperatures
         (io/upsert-data! "bicycle_counter_temperatures"))))

; ----- Public functions -----

(defn -main
  [& args]
  (when-not (:golemio-api-key env)
    (die "Please provide the Golemio API key using the GOLEMIO_API_KEY environment variable!"))
  (when-not (:db-password env)
    (die "Please provide the password for the database using the DB_PASSWORD environment variable!"))
  ; Initialize logging to standard error stream
  (log/merge-config! {:appenders {:println (appenders/println-appender {:stream :std-err})}})
  (mount/start)
  (main))
