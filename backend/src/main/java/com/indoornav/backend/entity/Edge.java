package com.indoornav.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

/** One physical connection. A bidirectional edge is one row (DATABASE.md 4.5). */
@Entity
@Immutable
@Table(name = "edges")
public class Edge {

    @Id
    private Long id;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_node_id", nullable = false)
    private Node fromNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_node_id", nullable = false)
    private Node toNode;

    @Column(name = "connection_type_code", nullable = false, length = 30)
    private String connectionTypeCode;

    @Column(name = "distance_m", nullable = false, precision = 8, scale = 2)
    private BigDecimal distanceMeters;

    @Column(name = "time_seconds", precision = 8, scale = 2)
    private BigDecimal timeSeconds;

    @Column(name = "accessible", nullable = false)
    private boolean accessible;

    @Column(name = "bidirectional", nullable = false)
    private boolean bidirectional;

    protected Edge() {
    }

    public Long getId() {
        return id;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public Node getFromNode() {
        return fromNode;
    }

    public Node getToNode() {
        return toNode;
    }

    public String getConnectionTypeCode() {
        return connectionTypeCode;
    }

    public BigDecimal getDistanceMeters() {
        return distanceMeters;
    }

    public BigDecimal getTimeSeconds() {
        return timeSeconds;
    }

    public boolean isAccessible() {
        return accessible;
    }

    public boolean isBidirectional() {
        return bidirectional;
    }
}
