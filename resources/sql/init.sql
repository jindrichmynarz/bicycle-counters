CREATE SCHEMA IF NOT EXISTS bicycle_counters;

-- CREATE EXTENSION IF NOT EXISTS postgis;

DROP TABLE IF EXISTS bicycle_counters.bicycle_counters CASCADE;
CREATE TABLE IF NOT EXISTS bicycle_counters.bicycle_counters (
  id VARCHAR PRIMARY KEY,
  name VARCHAR,
  route VARCHAR,
  updated_at TIMESTAMP WITH TIME ZONE,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION
  -- coordinates GEOMETRY,
  -- CHECK (st_ndims(coordinates) = 2),
  -- CHECK (geometrytype(coordinates) = 'POINT'::text OR coordinates IS NULL),
);

-- DROP INDEX IF EXISTS bicycle_counter_coordinates CASCADE;
-- CREATE INDEX IF NOT EXISTS bicycle_counter_coordinates
-- ON bicycle_counters.bicycle_counters
-- USING gist ( coordinates );

DROP TABLE IF EXISTS bicycle_counters.bicycle_counter_directions CASCADE;
CREATE TABLE IF NOT EXISTS bicycle_counters.bicycle_counter_directions (
  direction_id VARCHAR PRIMARY KEY,
  id VARCHAR REFERENCES bicycle_counters.bicycle_counters(id) ON DELETE CASCADE,
  name VARCHAR
);

DROP TABLE IF EXISTS bicycle_counters.detections;
CREATE TABLE IF NOT EXISTS bicycle_counters.detections (
  id VARCHAR NOT NULL REFERENCES bicycle_counters.bicycle_counters(id) ON DELETE CASCADE,
  locations_id VARCHAR NOT NULL REFERENCES bicycle_counters.bicycle_counter_directions(direction_id) ON DELETE CASCADE,
  value INTEGER,
  measured_from TIMESTAMP WITH TIME ZONE NOT NULL,
  measured_to TIMESTAMP WITH TIME ZONE,
  value_pedestrians INTEGER,
  PRIMARY KEY(id, measured_from)
);

DROP TABLE IF EXISTS bicycle_counters.temperatures;
CREATE TABLE IF NOT EXISTS bicycle_counters.temperatures (
  id VARCHAR NOT NULL REFERENCES bicycle_counters.bicycle_counters(id) ON DELETE CASCADE,
  value INTEGER,
  measured_from TIMESTAMP WITH TIME ZONE NOT NULL,
  measured_to TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY(id, measured_from)
);

-- Consider <https://dba.stackexchange.com/a/39599/61433>

CREATE EXTENSION IF NOT EXISTS btree_gist;

DROP INDEX IF EXISTS bicycle_counter_detections_time;
CREATE INDEX IF NOT EXISTS bicycle_counter_detections_time
ON bicycle_counters.detections
USING gist (
  id, measured_from, tstzrange(measured_from, measured_to, '[]')
);

DROP INDEX IF EXISTS bicycle_counter_temperatures_time;
CREATE INDEX IF NOT EXISTS bicycle_counter_temperatures_time
ON bicycle_counters.temperatures
USING gist (
  id, measured_from, tstzrange(measured_from, measured_to, '[]')
);
