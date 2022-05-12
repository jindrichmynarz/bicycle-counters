(ns cz.auto-mat.bicycle-counters.util)

(defn lazy-cat'
  "Lazily concatenates a sequences `colls`.
  Taken from <http://stackoverflow.com/a/26595111/385505>."
  [colls]
  (lazy-seq
    (if (seq colls)
      (concat (first colls) (lazy-cat' (next colls))))))

(defn take-until
  "Returns a lazy sequence of successive items from `coll` until
  `(pred item)` returns truthy value, including that item.
  `pred` must be free of side-effects."
  ; Taken from <https://groups.google.com/d/msg/clojure-dev/NaAuBz6SpkY/_aIDyyke9b0J>.
  [pred coll]
  (lazy-seq
    (when-let [s (seq coll)]
      (let [[head & tail] s]
        (if (pred head)
          (list head)
          (cons head (take-until pred tail)))))))

