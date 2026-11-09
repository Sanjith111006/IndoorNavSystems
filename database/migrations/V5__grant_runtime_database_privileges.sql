DO $$
BEGIN
    -- Hosted environments provision indoornav_app before Flyway runs. Local development
    -- commonly uses one role, so this migration intentionally does nothing when absent.
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'indoornav_app') THEN
        GRANT USAGE ON SCHEMA public TO indoornav_app;
        GRANT SELECT ON TABLE buildings, floors, connection_types, nodes, edges, rooms, points_of_interest
            TO indoornav_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE obstacles TO indoornav_app;
        GRANT USAGE, SELECT ON SEQUENCE obstacles_id_seq TO indoornav_app;
    END IF;
END $$;
