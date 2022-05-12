(ns cz.auto-mat.bicycle-counters.serialize)

(defn bicycle-counters
  "Serialize bicycle counters `data`."
  [data]
  (letfn [(reformat [{{[longitude latitude] :coordinates} :geometry
                      {:keys [id name route updated_at]} :properties}]
            {:id id
             :name name
             :route route
             :updated_at updated_at
             :longitude longitude
             :latitude latitude})]
    (->> data
         :features
         (map reformat))))
   
(defn bicycle-counter-directions
  "Serialize bicycle counter directions `data`."
  [data] 
  (letfn [(reformat [{{:keys [directions id]} :properties}]
            (map (fn [{direction_id :id
                       name :name}]
                   {:direction_id direction_id
                    :id id
                    :name name})
                 directions))]
    (->> data
         :features
         (filter (comp (partial some :id) :directions :properties)) ; Remove bicycle counters without directions
         (mapcat reformat))))
