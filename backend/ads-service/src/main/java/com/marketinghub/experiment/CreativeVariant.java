package com.marketinghub.experiment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Creative variant linked to an experiment.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreativeVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "experiment_id")
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    private CreativeType type;

    private String assetUrl;

    @Lob
    private String titles;

    @Lob
    private String descriptions;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
