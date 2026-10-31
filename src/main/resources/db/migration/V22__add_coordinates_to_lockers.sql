-- Migration V22: Add latitude and longitude to lockers table for visual map rendering
ALTER TABLE lockers ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE lockers ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- Seed coordinates for existing sample lockers in university campus
UPDATE lockers SET latitude = 21.0049, longitude = 105.8458 WHERE code = 'LK-A1';
UPDATE lockers SET latitude = 21.0062, longitude = 105.8432 WHERE code = 'LK-B2';
UPDATE lockers SET latitude = 21.0028, longitude = 105.8475 WHERE code = 'LK-C1';

-- Fallback for any other lockers created without coordinates
UPDATE lockers SET latitude = 21.0050, longitude = 105.8450 WHERE latitude IS NULL;
