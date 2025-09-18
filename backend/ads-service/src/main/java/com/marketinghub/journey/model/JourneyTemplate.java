package com.marketinghub.journey.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.*;

/**
 * Blueprint describing the phases and messaging cadence for a journey.
 */
@Entity
@Table(name = "journey_template")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JourneyTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob
    private String description;

    private String objective;

    @Column(name = "preferred_channel")
    private String preferredChannel;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "journey_template_phase", joinColumns = @JoinColumn(name = "template_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    @OrderColumn(name = "phase_order")
    @Builder.Default
    private List<JourneyPhase> phases = new ArrayList<>(List.of(JourneyPhase.ATTENTION,
            JourneyPhase.INTEREST,
            JourneyPhase.DESIRE,
            JourneyPhase.ACTION));

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "journey_template_tag", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "journey_template_metadata", joinColumns = @JoinColumn(name = "template_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    @Builder.Default
    private Map<String, String> metadata = new LinkedHashMap<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<JourneyStep> steps = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
