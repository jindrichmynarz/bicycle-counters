(ns cz.auto-mat.bicycle-counters.api
  (:require [cz.auto-mat.bicycle-counters.util :refer [lazy-cat' take-until]]
            [clj-http.client :as client]
            ;[clj-http.conn-mgr :as conn-mgr]
            ;[clj-http.core :as http]))
            [environ.core :refer [env]]
            [slingshot.slingshot :refer [throw+ try+]]
            [taoensso.timbre :as log])
  (:import (java.time ZonedDateTime)
           (java.time.format DateTimeFormatter)))

(def api-endpoint "https://api.golemio.cz/v2")

(def maximum-backoff
  "1 hour maximum backoff"
  3600000)

(defn request
  "Request the Golemio API using URL `path` with HTTP GET `query-params`."
  ([path]
   (request path {}))
  ([path
    query-params
    & {:keys [backoff]
       :or {backoff 5000}}]
   (let [url (str api-endpoint path)
         params {:as :json
                 :headers {:accept "application/json"
                           :x-access-token (:golemio-api-key env)}
                 :query-params query-params
                 :throw-entire-message? true}]
     (try+
       (:body (client/get url params))
       (catch [:status 403] error ; Handle HTTP 403 Not Authorized
         (log/error "Not authorized."
                    "You might need to generate a new API key at"
                    "https://api.golemio.cz/api-keys/dashboard.")
         (throw+ error))
       (catch [:status 429] error ; Handle HTTP 429 Too Many Requests
         (if (< backoff maximum-backoff)
           (do (log/warnf "Rate limited. Retrying in %.0f seconds." (/ backoff 1000))
               (Thread/sleep backoff)
               (request path params :backoff (* backoff 2)))
           (do (log/errorf "Maximum backoff %.0f seconds exceeded." (/ maximum-backoff 1000))
               (throw+ error))))))))

(defn request-paged
  "Request the Golemio API using URL `path` with HTTP GET `query-params`
  that uses pagination via limit and offset."
  [path
   query-params
   & {:keys [limit]
      :or {limit 10000}}] ; Maximum limit is 10000
  (letfn [(request-fn [offset]
            (->> {:limit limit :offset offset}
                 (merge query-params)
                 (request path)))]
    (->> 0
         (iterate (partial + limit))
         (map request-fn)
         (take-until (comp (partial > limit) count))
         lazy-cat')))

(defn request-offsetted
  "Paged API request.
  `offset-fn` is applied to a page of results to get query parameters for offsetting the next page."
  [^String path
   offset-fn
   {:keys [limit]
    :or {limit 10000} ; The maximum limit is 10000
    :as query-params}]
  (let [response (request path query-params)]
    (->> response
         offset-fn
         (merge query-params)
         (request-offsetted path offset-fn)
         (lazy-cat response))))

(comment)
  ; (def cache-config
  ;   (http/build-cache-config {}))
  ; (def connection-manager
  ;   (conn-mgr/make-reusable-conn-manager {}))
