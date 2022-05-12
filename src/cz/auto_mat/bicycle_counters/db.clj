(ns cz.auto-mat.bicycle-counters.db
  (:require [environ.core :refer [env]]
            [mount.core :refer [defstate]]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [next.jdbc.result-set :as rs])
  (:import (com.zaxxer.hikari HikariDataSource)))

(def db-spec
  (let [{:keys [db-host db-user db-password]} env]
    {:dbtype "postgres"
     :dbname "bicycle_counters"
     :host (or db-host "localhost")
     :username db-user ; "If you are using HikariCP and next.jdbc.connection/->pool to create a connection pooled datasource, you need to provide :username for the database username (instead of, or as well as, :user)." (https://github.com/seancorfield/next-jdbc/blob/develop/doc/all-the-options.md)
     :password db-password}))

(defn connect
  "Initializes the database connection pool and perform a validation check."
  []
  (let [^HikariDataSource ds (connection/->pool HikariDataSource db-spec)]
    (.close (jdbc/get-connection ds))
    (jdbc/with-options ds {:builder-fn rs/as-unqualified-lower-maps})))

(defstate connection :start (connect)
                     :stop (.close (:connectable connection)))
