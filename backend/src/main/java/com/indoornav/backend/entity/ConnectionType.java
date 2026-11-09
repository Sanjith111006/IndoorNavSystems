package com.indoornav.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "connection_types")
public class ConnectionType {

    /** Reserved codes with routing meaning (DATABASE.md 4.3). Other codes are plain data. */
    public static final String STAIRS = "STAIRS";

    public static final String ELEVATOR = "ELEVATOR";

    @Id
    @Column(name = "code", length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "speed_mps", nullable = false, precision = 5, scale = 2)
    private BigDecimal speedMps;

    @Column(name = "fixed_seconds", nullable = false, precision = 6, scale = 2)
    private BigDecimal fixedSeconds;

    protected ConnectionType() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSpeedMps() {
        return speedMps;
    }

    public BigDecimal getFixedSeconds() {
        return fixedSeconds;
    }
}
