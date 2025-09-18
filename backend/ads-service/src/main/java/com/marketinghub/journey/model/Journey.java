package com.marketinghub.journey.model;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.*;

/**
 * Operational instance of a {@link JourneyTemplate} targeting a concrete audience.
 */
@Entity
@Table(name = "journey")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Journey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private JourneyTemplate template;

    @Column(nullable = false)
    private String name;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyStatus status = JourneyStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "niche_id")
    private MarketNiche marketNiche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id")
    private Experiment experiment;

    /** Optional external segment identifier or CRM list reference. */
    @Column(name = "segment_reference")
    private String segmentReference;

    @Lob
    @Column(name = "segment_filter")
    private String segmentFilter;

    private Instant startAt;

    private Instant endAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "journey_metadata", joinColumns = @JoinColumn(name = "journey_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    @Builder.Default
    private Map<String, String> metadata = new LinkedHashMap<>();

    @OneToMany(mappedBy = "journey", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JourneyAssignment> assignments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
