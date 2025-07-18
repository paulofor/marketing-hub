package com.marketinghub.creative;

import com.marketinghub.experiment.Experiment;
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

    @ManyToOne
    @JoinColumn(name = "experiment_id")
    private Experiment experiment;

    private String headline;

    @Column(name = "primary_text")
    private String primaryText;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private CreativeStatus status;
}
