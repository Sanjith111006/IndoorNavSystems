package com.indoornav.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "points_of_interest")
public class PointOfInterest {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    protected PointOfInterest() {
    }

    public Long getId() {
        return id;
    }

    public Node getNode() {
        return node;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }
}
