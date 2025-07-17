package com.marketinghub.experiment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

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

    @ManyToOne
    @JoinColumn(name = "experiment_id")
    private Experiment experiment;

    private String location;

    @Lob
    private String interests;

    @Lob
    private String lookalikes;

    private java.math.BigDecimal budget;
    private Integer durationDays;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
