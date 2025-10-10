package com.marketinghub.journey.model;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.EmotionalTrigger;
import com.marketinghub.creative.label.VisualProof;
import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Defines a concrete touchpoint inside a {@link JourneyTemplate}.
 */
@Entity
@Table(name = "journey_step")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JourneyStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private JourneyTemplate template;

    @Column(nullable = false)
    private Integer position;

    private String name;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "stimulus_type", nullable = false, length = 64)
    private JourneyStimulusType stimulusType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creative_id")
    private Creative creative;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "angle_id")
    private Angle angle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visual_proof_id")
    private VisualProof visualProof;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotional_trigger_id")
    private EmotionalTrigger emotionalTrigger;

    @Column(name = "entry_condition")
    private String entryCondition;

    @Column(name = "exit_condition")
    private String exitCondition;

    @Column(name = "delay_minutes")
    private Integer delayMinutes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "journey_step_metadata", joinColumns = @JoinColumn(name = "step_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    @Builder.Default
    private Map<String, String> metadata = new LinkedHashMap<>();
}
