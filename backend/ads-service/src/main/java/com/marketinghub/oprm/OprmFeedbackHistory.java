package com.marketinghub.oprm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OprmFeedbackHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occupation_name", nullable = false, length = 191)
    private String occupationName;

    @Column(name = "persona_label", nullable = false, length = 191)
    private String personaLabel;

    @Column(name = "correlation_id", nullable = false, length = 191)
    private String correlationId;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "previous_routine_confidence", precision = 5, scale = 4)
    private BigDecimal previousRoutineConfidence;

    @Column(name = "recalibrated_routine_confidence", precision = 5, scale = 4)
    private BigDecimal recalibratedRoutineConfidence;

    @Column(name = "previous_framework_confidence", precision = 5, scale = 4)
    private BigDecimal previousFrameworkConfidence;

    @Column(name = "recalibrated_framework_confidence", precision = 5, scale = 4)
    private BigDecimal recalibratedFrameworkConfidence;

    @Column(name = "average_hypothesis_impact", precision = 5, scale = 4)
    private BigDecimal averageHypothesisImpact;

    @Column(name = "notes", columnDefinition = "LONGTEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
