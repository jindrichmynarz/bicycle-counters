-- Upsert CSV data with :column-header from the :input program to :table-name.
-- :snip upsert-copy
CREATE TEMPORARY TABLE tmp_table
ON COMMIT DROP
AS
SELECT *
FROM bicycle_counters.:identifier:table-name
WITH NO DATA;
COPY tmp_table ( :identifier:column-header )
FROM PROGRAM :program:input
WITH (FORMAT CSV, HEADER TRUE);
INSERT INTO bicycle_counters.:identifier:table-name
SELECT *
FROM tmp_table

-- :name upsert-copy-bicycle-counters
-- :command :execute
-- :result :raw
:snip:upsert-copy
ON CONFLICT (id)
DO UPDATE
SET (
  name,
  route,
  updated_at,
  latitude,
  longitude
) = (
  EXCLUDED.name,
  EXCLUDED.route,
  EXCLUDED.updated_at,
  EXCLUDED.latitude,
  EXCLUDED.longitude
)

-- :name upsert-copy-bicycle-counter-directions
-- :command :execute
-- :result :raw
:snip:upsert-copy
ON CONFLICT (direction_id)
DO UPDATE
SET (
  id,
  name
) = (
  EXCLUDED.id,
  EXCLUDED.name
)

-- :name upsert-copy-detections
-- :command :execute
-- :result :raw
:snip:upsert-copy
ON CONFLICT (id, locations_id, measured_from)
DO UPDATE
SET (
  value,
  measured_to,
  value_pedestrians
) = (
  EXCLUDED.value,
  EXCLUDED.measured_to,
  EXCLUDED.value_pedestrians
)

-- :name upsert-copy-temperatures
-- :command :execute
-- :result :raw
:snip:upsert-copy
ON CONFLICT (id, measured_from)
DO UPDATE
SET (
  value,
  measured_to
) = (
  EXCLUDED.value,
  EXCLUDED.measured_to
)

-- Maximum time of an observation from bicycle counter with :id in :table-name.
-- :name maximum-time :? :1
SELECT to_char(timezone('utc', max(measured_to)), 'YYYY-MM-DD"T"HH24:MI:SSZ') AS maximum_time
FROM bicycle_counters.:identifier:table-name
WHERE id = :id
