package com.indoornav.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "floors")
public class Floor {

    @Id
    private Long id;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "display_name", nullable = false, length = 40)
    private String displayName;

    @Column(name = "plan_asset_path", nullable = false, length = 255)
    private String planAssetPath;

    @Column(name = "plan_width", nullable = false, precision = 9, scale = 2)
    private BigDecimal planWidth;

    @Column(name = "plan_height", nullable = false, precision = 9, scale = 2)
    private BigDecimal planHeight;

    @Column(name = "metres_per_unit", nullable = false, precision = 9, scale = 6)
    private BigDecimal metresPerUnit;

    protected Floor() {
    }

    public Long getId() {
        return id;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPlanAssetPath() {
        return planAssetPath;
    }

    public BigDecimal getPlanWidth() {
        return planWidth;
    }

    public BigDecimal getPlanHeight() {
        return planHeight;
    }

    public BigDecimal getMetresPerUnit() {
        return metresPerUnit;
    }
}
