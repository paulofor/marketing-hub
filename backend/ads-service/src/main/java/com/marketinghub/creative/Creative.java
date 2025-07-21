package com.marketinghub.creative;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.VisualProof;
import com.marketinghub.creative.label.EmotionalTrigger;
import jakarta.persistence.*;
import lombok.*;

/**
 * Creative linked to an experiment.
 */
@Entity
@Table(name = "creative_variants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Creative {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    private String headline;

    @Column(name = "primary_text")
    private String primaryText;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private CreativeStatus status;

    @ManyToMany
    @JoinTable(name = "creative_angle",
            joinColumns = @JoinColumn(name = "creative_id"),
            inverseJoinColumns = @JoinColumn(name = "angle_id"))
    private java.util.Set<Angle> angles = new java.util.HashSet<>();

    @ManyToMany
    @JoinTable(name = "creative_visual_proof",
            joinColumns = @JoinColumn(name = "creative_id"),
            inverseJoinColumns = @JoinColumn(name = "proof_id"))
    private java.util.Set<VisualProof> visualProofs = new java.util.HashSet<>();

    @ManyToMany
    @JoinTable(name = "creative_emotional_trigger",
            joinColumns = @JoinColumn(name = "creative_id"),
            inverseJoinColumns = @JoinColumn(name = "trigger_id"))
    private java.util.Set<EmotionalTrigger> emotionalTriggers = new java.util.HashSet<>();
}
