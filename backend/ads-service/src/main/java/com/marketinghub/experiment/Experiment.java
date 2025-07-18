package com.marketinghub.experiment;

import jakarta.persistence.*;
import lombok.*;
import com.marketinghub.niche.MarketNiche;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Experiment grouping ad sets and creatives.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"niche_id", "name"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experiment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY,
            cascade = CascadeType.PERSIST)
    @JoinColumn(name = "niche_id", nullable = false)
    private MarketNiche niche;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String hypothesis;

    private java.math.BigDecimal kpiTarget;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ExperimentStatus status;

    @Enumerated(EnumType.STRING)
    private ExperimentPlatform platform;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
