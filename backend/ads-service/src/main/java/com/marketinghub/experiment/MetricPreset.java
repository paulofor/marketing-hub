package com.marketinghub.experiment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * Preset of structural metrics for experiments.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "metric_preset")
public class MetricPreset {
    @Id
    private String id;

    private String name;

    private Integer sampleSize;

    @Column(precision = 5, scale = 2)
    private BigDecimal stopLossFactor;

    @Column(name = "default_mde_pp", precision = 5, scale = 2)
    private BigDecimal defaultMdePp;
}
