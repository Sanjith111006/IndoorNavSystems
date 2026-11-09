package com.indoornav.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** The only table the running application writes to. Targets exactly one edge or one node. */
@Entity
@Table(name = "obstacles")
public class Obstacle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_id", nullable = false, updatable = false)
    private Long buildingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edge_id", updatable = false)
    private Edge edge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", updatable = false)
    private Node node;

    @Enumerated(EnumType.STRING)
    @Column(name = "obstacle_type", nullable = false, length = 30)
    private ObstacleType type;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ObstacleStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Obstacle() {
    }

    private Obstacle(Long buildingId, Edge edge, Node node, ObstacleType type, String description, Instant now) {
        this.buildingId = buildingId;
        this.edge = edge;
        this.node = node;
        this.type = type;
        this.description = description;
        this.status = ObstacleStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Obstacle onEdge(Edge edge, ObstacleType type, String description, Instant now) {
        return new Obstacle(edge.getBuildingId(), edge, null, type, description, now);
    }

    public static Obstacle onNode(Node node, ObstacleType type, String description, Instant now) {
        return new Obstacle(node.getBuildingId(), null, node, type, description, now);
    }

    public void update(ObstacleType newType, String newDescription, ObstacleStatus newStatus, Instant now) {
        this.type = newType;
        this.description = newDescription;
        this.status = newStatus;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public Edge getEdge() {
        return edge;
    }

    public Node getNode() {
        return node;
    }

    public ObstacleType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public ObstacleStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
