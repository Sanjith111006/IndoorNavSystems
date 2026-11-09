package com.indoornav.backend.dto;

import com.indoornav.backend.entity.NodeType;
import java.math.BigDecimal;

public record LabelledNodeResponse(
        Long id,
        String label,
        NodeType type,
        Long floorId,
        String floorDisplayName,
        BigDecimal x,
        BigDecimal y) {
}
