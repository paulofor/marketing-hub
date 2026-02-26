package com.marketinghub.experiment;

import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElement;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "experiment_targeting_selection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentTargetingSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(name = "candidate_type", nullable = false, length = 32)
    private TargetingCandidateType candidateType;

    @Column(name = "term", nullable = false, length = 191)
    private String term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "targeting_element_id")
    @ToString.Exclude
    private TargetingElement targetingElement;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
