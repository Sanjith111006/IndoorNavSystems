package com.indoornav.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "rooms")
public class Room {

    @Id
    private Long id;

    @Column(name = "floor_id", nullable = false)
    private Long floorId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "bounds_x", nullable = false, precision = 9, scale = 2)
    private BigDecimal boundsX;

    @Column(name = "bounds_y", nullable = false, precision = 9, scale = 2)
    private BigDecimal boundsY;

    @Column(name = "bounds_width", nullable = false, precision = 9, scale = 2)
    private BigDecimal boundsWidth;

    @Column(name = "bounds_height", nullable = false, precision = 9, scale = 2)
    private BigDecimal boundsHeight;

    protected Room() {
    }

    public Long getId() {
        return id;
    }

    public Long getFloorId() {
        return floorId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBoundsX() {
        return boundsX;
    }

    public BigDecimal getBoundsY() {
        return boundsY;
    }

    public BigDecimal getBoundsWidth() {
        return boundsWidth;
    }

    public BigDecimal getBoundsHeight() {
        return boundsHeight;
    }
}
