-- The sample graph requires a shorter stair route and a faster elevator alternative.
UPDATE connection_types
SET speed_mps = 0.25,
    fixed_seconds = 0.00
WHERE code = 'STAIRS';

UPDATE connection_types
SET speed_mps = 4.00,
    fixed_seconds = 5.00
WHERE code = 'ELEVATOR';
