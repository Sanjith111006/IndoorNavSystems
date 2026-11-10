package com.indoornav.engine.model;

import java.util.Locale;
import java.util.Objects;

/**
 * A connection-type code sourced from reference data. Unknown codes remain valid ordinary connections.
 */
public record ConnectionType(String code) {
    public static final ConnectionType CORRIDOR = new ConnectionType("CORRIDOR");
    public static final ConnectionType DOORWAY = new ConnectionType("DOORWAY");
    public static final ConnectionType RAMP = new ConnectionType("RAMP");
    public static final ConnectionType STAIRS = new ConnectionType("STAIRS");
    public static final ConnectionType ELEVATOR = new ConnectionType("ELEVATOR");

    public ConnectionType {
        code = Objects.requireNonNull(code, "code must not be null").trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Connection type code must not be blank.");
        }
    }

    public boolean isStairs() {
        return this.equals(STAIRS);
    }

    public boolean isElevator() {
        return this.equals(ELEVATOR);
    }
}
