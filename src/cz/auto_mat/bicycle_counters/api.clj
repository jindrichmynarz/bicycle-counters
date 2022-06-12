(ns cz.auto-mat.bicycle-counters.api
  (:require [clj-http.client :as client]
            [environ.core :refer [env]]
            [slingshot.slingshot :refer [throw+ try+]]
            [taoensso.timbre :as log])
  (:import (java.time ZonedDateTime)
           (java.time.format DateTimeFormatter)))

(def maximum-backoff
  "1 hour maximum backoff"
  3600000)

(def maximum-limit
  "Maximum number of results the API allows per response"
  10000)

(defn request
  "Request the Golemio API using URL `path` with HTTP GET `query-params`."
  ([path]
   (request path {}))
  ([path
    query-params
    & {:keys [backoff]
       :or {backoff 5000}}]
   (let [url (-> env :api-endpoint (str path))
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

(defn request-offsetted
  "Paged API request.
  `offset-fn` is applied to a page of results to get query parameters for offsetting the next page
  if the page shorter than `limit`."
  [^String path
   offset-fn
   {:keys [limit]
    :or {limit maximum-limit}
    :as query-params}]
  (let [response (->> limit
                      (assoc query-params :limit)
                      (request path))]
    (if (< (count response) limit)
      response
      (some->> response
               offset-fn
               (merge query-params)
               (request-offsetted path offset-fn)
               (lazy-cat response)))))
