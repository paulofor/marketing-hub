package com.marketinghub.facebookads.dto;

import java.math.BigDecimal;
import lombok.Data;

/** DTO used when creating campaigns from experiments. */
@Data
public class ExperimentDTO {
    private java.util.UUID id;
    private String name;
    private BigDecimal kpiTarget;
    private Boolean splitTest;
}
