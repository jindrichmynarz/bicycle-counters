(ns cz.auto-mat.bicycle-counters.sql
  (:require [cz.auto-mat.bicycle-counters.db :refer [connection]]
            [clojure.string :as string]
            [hugsql.core :refer [def-sqlvec-fns]]
            [hugsql.parameters :as params]
            [next.jdbc :as jdbc]
            [next.jdbc.prepare :as prepare])
  (:import (java.io File)))

(def-sqlvec-fns "cz/auto_mat/bicycle_counters/sql.sql")

(defn wrap-quotes
  "Wrap string `s` with a quote character."
  ([s]
   (wrap-quotes \' s))
  ([quote-char s]
   (str quote-char s quote-char)))

(defn format-program
  "Format a shell `program` from a vector of arguments."
  [program]
  (->> program
       (string/join " ")
       wrap-quotes))

(defmethod params/apply-hugsql-param :program
  ; Format a shell program from a vector.
  [{param-name :name} data options]
  (->> param-name
       keyword
       data
       format-program
       vector))

(def table-name->sqlvec-fn
  {"bicycle_counters" upsert-copy-bicycle-counters-sqlvec
   "bicycle_counter_directions" upsert-copy-bicycle-counter-directions-sqlvec
   "detections" upsert-copy-detections-sqlvec
   "temperatures" upsert-copy-temperatures-sqlvec})

(defn upsert-copy-csv-gz!
  "COPY data from a GZip-compressed `csv-file` with column `header` into `table-name`
  overwriting existing data."
  ; Warning: Deleted data is kept. However, in case of bicycle counters, they should be deactivated
  ; rather than deleted, because deleting would cause inconsistency of historical data.
  [^String table-name
   header
   ^File csv-file]
  (let [column-header (string/join \, header)
        program ["gzip" "-cd" (.getAbsolutePath csv-file)]
        params {:column-header column-header
                :input program
                :table-name table-name}
        snippet (upsert-copy params)
        sqlvec-fn (table-name->sqlvec-fn table-name)]
    (->> {:upsert-copy snippet}
         sqlvec-fn
         (jdbc/execute-one! connection))))

(defn ^String maximum-time
  "Return the maximum value of `measured_to` from `table-name` for the given `id`
  as an xsd:dateTime-formatted string."
  [^String table-name
   ^String id]
  (->> {:id id
        :table-name table-name}
       maximum-time-sqlvec
       (jdbc/execute-one! connection)
       :maximum_time))
