package com.marketinghub.experiment.promise;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: persistir uma solicitação de geração de contrato de promessa para execução assíncrona. */
@Entity
@Table(name = "experiment_promise_generation_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentPromiseGenerationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "niche_id", nullable = false)
    private MarketNiche niche;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hypothesis_id", nullable = false)
    private Hypothesis hypothesis;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExperimentPromiseGenerationRequestStatus status;

    @Column(name = "model", nullable = false, length = 191)
    private String model;

    @Column(name = "worker_id", length = 191)
    private String workerId;

    @Column(name = "current_single_pain", length = 512)
    private String currentSinglePain;

    @Column(name = "current_free_reward", length = 512)
    private String currentFreeReward;

    @Column(name = "current_funnel_promise", length = 512)
    private String currentFunnelPromise;

    @Column(name = "current_primary_cta", length = 191)
    private String currentPrimaryCta;

    @Lob
    @Column(name = "prompt", nullable = false, columnDefinition = "LONGTEXT")
    private String prompt;

    @Lob
    @Column(name = "options_json", columnDefinition = "LONGTEXT")
    private String optionsJson;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
