(ns cz.auto-mat.bicycle-counters.sql
  (:require [cz.auto-mat.bicycle-counters.db :refer [connection]]
            [clojure.string :as string]
            [hugsql.core :refer [def-sqlvec-fns]]
            [hugsql.parameters :as params]
            [next.jdbc :as jdbc]
            [next.jdbc.prepare :as prepare])
  (:import (java.io File)))

(def-sqlvec-fns "cz/auto_mat/bicycle_counters/sql/functions.sql")

(defn wrap-quotes
  "Wrap string `s` with a quote character."
  ([s]
   (wrap-quotes \' s))
  ([quote-char s]
   (str quote-char s quote-char)))

(defmethod params/apply-hugsql-param :program
  ; Format a shell program from a vector.
  [{param-name :name} data options]
  (->> param-name
       keyword
       data
       (string/join " ")
       wrap-quotes
       (vector "?")))

(defn copy-csv-gz!
  ; TODO: Should we have both this function and upsert-copy-csv-gz!?
  "COPY data from a GZip-compressed `csv-file` with column `header` into `table-name`.
  Set `truncate?` = true when performing full reload."
  ([^String table-name
    header
    ^File csv-file]
   (copy-csv-gz! table-name header csv-file false))
  ([^String table-name
    header
    ^File csv-file
    ^Boolean truncate?]
   (let [column-header (string/join \, header)
         program ["zcat" (.getAbsolutePath csv-file)]
         sqlvec (if truncate? truncate-copy-sqlvec copy-sqlvec)]
     (->> {:column-header column-header
           :input program
           :table-name table-name} 
          sqlvec
          (jdbc/execute-one! connection)))))

(defn upsert-copy-csv-gz!
  "COPY data from a GZip-compressed `csv-file` with column `header` into `table-name`
  overwriting existing data."
  ; Warning: Deleted data is kept. However, in case of bicycle counters, they should be deactivated
  ; rather than deleted, because deleting would cause inconsistency of historical data.
  [^String table-name
   header
   ^File csv-file]
  (let [column-header (string/join \, header)
        program ["zcat" (.getAbsolutePath csv-file)]]
    (->> {:column-header column-header
          :input program 
          :table-name table-name}
         upsert-copy-sqlvec
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
