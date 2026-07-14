package com.marketinghub.feo.fabricacao.v1;

import com.marketinghub.experiment.Experiment;
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
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** Responsabilidade: persistir uma execução auditável de etapa da FEO vinculada a um experimento. */
@Entity
@Table(name = "feo_fabricacao_v1_stage_execution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeoFabricacaoV1StageExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Experimento que originou a fabricação dos entregáveis. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    /** Identificador estável do job de fabricação. */
    @Column(name = "job_id", nullable = false, length = 64)
    private String jobId;

    /** Código canônico da etapa consumida pelo worker FEO. */
    @Column(name = "stage_code", nullable = false, length = 80)
    private String stageCode;

    /** Status operacional da execução. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FeoFabricacaoV1StageStatus status;

    /** Payload de entrada entregue ao worker. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "input_payload", nullable = false, columnDefinition = "LONGTEXT")
    private String inputPayload;

    /** Payload funcional retornado pelo worker. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "output_payload", columnDefinition = "LONGTEXT")
    private String outputPayload;

    /** Artefatos auditáveis retornados pelo worker. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "artifacts_payload", columnDefinition = "LONGTEXT")
    private String artifactsPayload;

    /** Métricas de execução retornadas pelo worker. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "metrics_payload", columnDefinition = "LONGTEXT")
    private String metricsPayload;

    /** Motivo funcional para bloqueio da etapa. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "block_reason", columnDefinition = "LONGTEXT")
    private String blockReason;

    /** Próxima etapa solicitada pelo worker quando o contrato permitir avanço automático. */
    @Column(name = "next_stage_code", length = 80)
    private String nextStageCode;

    /** Worker que reportou o resultado. */
    @Column(name = "worker_id", length = 191)
    private String workerId;

    /** Erro técnico registrado quando a execução falha. */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    /** Momento em que o worker assumiu a execução. */
    @Column(name = "started_at")
    private Instant startedAt;

    /** Momento em que a execução terminou ou foi bloqueada. */
    @Column(name = "finished_at")
    private Instant finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
