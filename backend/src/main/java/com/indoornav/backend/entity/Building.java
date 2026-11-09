package com.indoornav.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Read-only at runtime: graph data changes only through migrations (PRD FR-39). */
@Entity
@Immutable
@Table(name = "buildings")
public class Building {

    @Id
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "building_type", nullable = false, length = 50)
    private String buildingType;

    @Column(name = "address", length = 255)
    private String address;

    protected Building() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public String getAddress() {
        return address;
    }
}
