package com.marketinghub.experiment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Advertising set configuration for an experiment.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    private String location;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String interests;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String jobTitles;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String behaviors;


    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String targetingJson;

    @Column(name = "targeting_request_id", columnDefinition = "BINARY(16)")
    private UUID targetingRequestId;

    private java.math.BigDecimal budget;
    private Integer durationDays;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String prompt;

    private String model;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
