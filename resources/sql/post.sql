UPDATE bicycle_counters.bicycle_counters
SET coordinates = ST_GeomFromText('POINT(' || longitude || ' ' || latitude || ')', 4326);
