package com.indoornav.engine.result;

import com.indoornav.engine.model.ConnectionType;
import java.util.Objects;

/**
 * A path step that moves between two floors.
 */
public record FloorTransition(
        long fromFloorId,
        long toFloorId,
        ConnectionType connectionType,
        int pathPosition) {

    public FloorTransition {
        Objects.requireNonNull(connectionType, "connectionType must not be null");
        if (fromFloorId <= 0 || toFloorId <= 0 || fromFloorId == toFloorId || pathPosition < 0) {
            throw new IllegalArgumentException("A floor transition must connect distinct floors at a valid path position.");
        }
    }
}
