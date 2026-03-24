package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.model.Lead;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Evento que representa uma interação registrada em alguma etapa do funil de vendas do experimento.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentFunnelEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private ExperimentFunnelStage stage;

    @Column(length = 50)
    private String source;

    @Column(name = "campaign_code", length = 190)
    private String campaignCode;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String payload;

    @CreationTimestamp
    @Column(name = "occurred_at")
    private Instant occurredAt;
}
