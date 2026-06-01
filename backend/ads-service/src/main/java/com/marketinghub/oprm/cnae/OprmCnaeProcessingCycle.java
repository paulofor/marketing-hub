package com.marketinghub.oprm.cnae;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

/**
 * Entidade responsável por persistir a rastreabilidade dos ciclos automáticos de score e enriquecimento de CNAEs.
 */
@Entity
@Data
@Table(name = "oprm_cnae_processing_cycle")
public class OprmCnaeProcessingCycle {
    @Id
    @Column(name = "cycle_id", length = 64)
    private String cycleId;

    @Column(name = "cycle_type", nullable = false, length = 32)
    private String cycleType;

    @Column(name = "cycle_number", nullable = false)
    private Long cycleNumber;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "selection_criteria", length = 1000)
    private String selectionCriteria;

    @Column(name = "processed_count", nullable = false)
    private Integer processedCount = 0;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "summary", columnDefinition = "LONGTEXT")
    private String summary;

    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;
}
