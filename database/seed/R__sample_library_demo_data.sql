-- A second, deliberately small environment proves that another building is added as data only.
INSERT INTO buildings (id, name, description, building_type, address) VALUES
    (2, 'Sample Library', 'A compact single-floor demonstration environment.', 'Library', '200 Example Way')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, description = EXCLUDED.description, building_type = EXCLUDED.building_type,
    address = EXCLUDED.address;

INSERT INTO floors (id, building_id, level, display_name, plan_asset_path, plan_width, plan_height, metres_per_unit) VALUES
    (200, 2, 0, 'Ground', 'sample-library/ground.svg', 600.00, 400.00, 0.100000)
ON CONFLICT (id) DO UPDATE SET
    building_id = EXCLUDED.building_id, level = EXCLUDED.level, display_name = EXCLUDED.display_name,
    plan_asset_path = EXCLUDED.plan_asset_path, plan_width = EXCLUDED.plan_width,
    plan_height = EXCLUDED.plan_height, metres_per_unit = EXCLUDED.metres_per_unit;

INSERT INTO nodes (id, building_id, floor_id, x, y, node_type, display_name, accessible) VALUES
    (1301, 2, 200, 80.00, 200.00, 'BUILDING_ENTRANCE', 'Library entrance', true),
    (1302, 2, 200, 180.00, 200.00, 'POINT_OF_INTEREST', NULL, true),
    (1303, 2, 200, 300.00, 200.00, 'ROOM_ENTRANCE', NULL, true),
    (1304, 2, 200, 420.00, 200.00, 'POINT_OF_INTEREST', NULL, true),
    (1305, 2, 200, 520.00, 200.00, 'ROOM_ENTRANCE', NULL, true),
    (1306, 2, 200, 520.00, 80.00, 'CORRIDOR_JUNCTION', NULL, true),
    (1307, 2, 200, 300.00, 80.00, 'POINT_OF_INTEREST', NULL, true),
    (1308, 2, 200, 80.00, 80.00, 'POINT_OF_INTEREST', NULL, true)
ON CONFLICT (id) DO UPDATE SET
    building_id = EXCLUDED.building_id, floor_id = EXCLUDED.floor_id, x = EXCLUDED.x, y = EXCLUDED.y,
    node_type = EXCLUDED.node_type, display_name = EXCLUDED.display_name, accessible = EXCLUDED.accessible;

INSERT INTO edges (id, building_id, from_node_id, to_node_id, connection_type_code, distance_m, time_seconds, accessible, bidirectional) VALUES
    (2401, 2, 1301, 1302, 'CORRIDOR', 10.00, NULL, true, true),
    (2402, 2, 1302, 1303, 'CORRIDOR', 12.00, NULL, true, true),
    (2403, 2, 1303, 1304, 'CORRIDOR', 12.00, NULL, true, true),
    (2404, 2, 1304, 1305, 'CORRIDOR', 10.00, NULL, true, true),
    (2405, 2, 1305, 1306, 'CORRIDOR', 12.00, NULL, true, true),
    (2406, 2, 1306, 1307, 'CORRIDOR', 22.00, NULL, true, true),
    (2407, 2, 1307, 1308, 'CORRIDOR', 22.00, NULL, true, true),
    (2408, 2, 1308, 1301, 'CORRIDOR', 12.00, NULL, true, true)
ON CONFLICT (id) DO UPDATE SET
    building_id = EXCLUDED.building_id, from_node_id = EXCLUDED.from_node_id, to_node_id = EXCLUDED.to_node_id,
    connection_type_code = EXCLUDED.connection_type_code, distance_m = EXCLUDED.distance_m,
    time_seconds = EXCLUDED.time_seconds, accessible = EXCLUDED.accessible, bidirectional = EXCLUDED.bidirectional;

INSERT INTO rooms (id, floor_id, name, category, description, bounds_x, bounds_y, bounds_width, bounds_height) VALUES
    (3301, 200, 'Welcome desk', 'information', 'Library visitor information.', 120.00, 160.00, 100.00, 80.00),
    (3302, 200, 'Reading room', 'reading', 'Shared reading space.', 240.00, 160.00, 120.00, 80.00),
    (3303, 200, 'Study room', 'study', 'Quiet study space.', 380.00, 160.00, 100.00, 80.00),
    (3304, 200, 'Public computers', 'computer', 'Public computer access.', 240.00, 20.00, 120.00, 50.00)
ON CONFLICT (id) DO UPDATE SET
    floor_id = EXCLUDED.floor_id, name = EXCLUDED.name, category = EXCLUDED.category,
    description = EXCLUDED.description, bounds_x = EXCLUDED.bounds_x, bounds_y = EXCLUDED.bounds_y,
    bounds_width = EXCLUDED.bounds_width, bounds_height = EXCLUDED.bounds_height;

INSERT INTO points_of_interest (id, node_id, room_id, name, category) VALUES
    (4301, 1302, 3301, 'Welcome desk', 'information'),
    (4302, 1304, 3302, 'Reading room', 'reading'),
    (4303, 1307, 3304, 'Public computers', 'computer'),
    (4304, 1308, 3303, 'Study room', 'study')
ON CONFLICT (id) DO UPDATE SET
    node_id = EXCLUDED.node_id, room_id = EXCLUDED.room_id, name = EXCLUDED.name, category = EXCLUDED.category;
