(ns cz.auto-mat.bicycle-counters.io
  (:require [cz.auto-mat.bicycle-counters.sql :as sql]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.io File)
           (java.util.zip GZIPOutputStream)))

(defn- csv-head
  "Use sorted keys of the first map in `data` as a CSV header."
  [data]
  (-> data first keys sort))

(defn ->csv
  "Transform a collection of maps with keyword keys in `data` to
  a collection of seqs, starting with the header row followed by data
  rows. The header can be provided as the optional `head` argument
  containing a collection of column keywords. This can be used to specify
  the order of columns, or if there are optional keys in the maps.
  The header is made from the keys of the first map by default."
  ([data]
   (->> data
        csv-head
        (->csv data))) 
  ([data head]
   (->csv data head (map name head)))
  ([data head header]
   (let [->row (apply juxt head)]
     (->> data
          (map ->row)
          (cons header)))))

(defmacro with-delete
  "bindings => [name init ...]

  Evaluates body in a try expression with names bound to the values
  of the inits, and a finally clause that calls (.delete name) on each
  name in reverse order."
  [bindings & body]
  (cond
    (= (count bindings) 0) `(do ~@body)
    (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                              (try
                                (with-delete ~(subvec bindings 2) ~@body)
                                (finally
                                  (. ~(bindings 0) delete))))
    :else (throw (IllegalArgumentException. "with-delete only allows symbols in bindings"))))

(defn write-csv-gz!
  "Write sequence of maps `csv-data` to `file` as GZip-compressed CSV."
  [^File file
   csv-data]
  {:pre [(string/ends-with? (.getName file) ".csv.gz") ".csv.gz file suffix must be used!"]}
  (with-open [writer (-> file
                         io/output-stream
                         GZIPOutputStream.
                         io/writer)]
    (csv/write-csv writer csv-data)))

(defn copy-data!
  "Copy `data` to an SQL `table-name`, optionally truncating the table before load."
  [^String table-name
   ^Boolean truncate!
   data]
  (with-delete [tmp-file (File/createTempFile "bicycle-counters" ".csv.gz")]
    (let [csv-data (->csv data)
          csv-header (first csv-data)]
      (write-csv-gz! tmp-file csv-data)
      (sql/copy-csv-gz! table-name csv-header tmp-file truncate!))))
