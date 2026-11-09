package com.indoornav.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "nodes")
public class Node {

    @Id
    private Long id;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Column(name = "x", nullable = false, precision = 9, scale = 2)
    private BigDecimal x;

    @Column(name = "y", nullable = false, precision = 9, scale = 2)
    private BigDecimal y;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 30)
    private NodeType type;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(name = "accessible", nullable = false)
    private boolean accessible;

    protected Node() {
    }

    public Long getId() {
        return id;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public Floor getFloor() {
        return floor;
    }

    public BigDecimal getX() {
        return x;
    }

    public BigDecimal getY() {
        return y;
    }

    public NodeType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAccessible() {
        return accessible;
    }
}
