package com.marketinghub.experiment.learning;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Resultado consolidado de uma leitura automática do experimento.
 */
@Entity
@Table(name = "experiment_learning")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentLearning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "niche_id", nullable = false)
    private MarketNiche niche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hypothesis_id")
    private Hypothesis hypothesis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ExperimentLearningRequest request;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ExperimentStage stage;

    @Column(name = "primary_metric", length = 128)
    private String primaryMetric;

    @Column(name = "metric_signal", length = 255)
    private String metricSignal;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String summary;

    @Lob
    @Column(name = "what_worked", columnDefinition = "LONGTEXT")
    private String whatWorked;

    @Lob
    @Column(name = "what_blocked", columnDefinition = "LONGTEXT")
    private String whatBlocked;

    @Lob
    @Column(name = "next_test", columnDefinition = "LONGTEXT")
    private String nextTest;

    @Lob
    @Column(name = "insights_json", columnDefinition = "LONGTEXT")
    private String insightsJson;

    @Lob
    @Column(name = "suggestions_json", columnDefinition = "LONGTEXT")
    private String suggestionsJson;

    @Lob
    @Column(name = "openai_request_payload_json", columnDefinition = "LONGTEXT")
    private String openAiRequestPayloadJson;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
