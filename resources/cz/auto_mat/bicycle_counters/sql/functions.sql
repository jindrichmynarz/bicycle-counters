-- Copy data with :column-header from the :input program to :table-name.
-- :name copy
-- :command :execute
-- :result :raw
COPY bicycle_counters.:identifier:table-name (:identifier:column-header )
FROM PROGRAM :program:input
WITH (FORMAT CSV, HEADER TRUE);

-- Copy data with :column-header from the :input program to :table-name.
-- Truncate :table-name first.
-- :name truncate-copy
-- :command :execute
-- :result :raw
TRUNCATE bicycle_counters.:identifier:table-name;
COPY bicycle_counters.:identifier:table-name (:identifier:column-header )
FROM PROGRAM :program:input
WITH (FORMAT CSV, HEADER TRUE);

-- Upsert CSV data with :column-header from the :input program to :table-name.
-- :name upsert-copy
-- :command :execute
-- :result :raw
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
ON CONFLICT DO UPDATE;

-- Maximum time of an observation from bicycle counter with :id in :table-name.
-- :name maximum-time :? :1
SELECT to_char(timezone('utc', max(measured_to)), 'YYYY-MM-DD\"T\"HH24:MI:SSZ') AS maximum_time
FROM bicycle_counters.:identifier:table-name
WHERE id = :id
