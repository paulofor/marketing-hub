package com.marketinghub.experiment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registra uma mudança auditável de status de um experimento comercial.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentStatusChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    /** Status anterior do experimento antes da ação administrativa. */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 32)
    private ExperimentStatus previousStatus;

    /** Novo status aplicado ao experimento pela ação administrativa. */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 32, nullable = false)
    private ExperimentStatus newStatus;

    /** Tipo de ação que originou a mudança de status. */
    @Column(name = "action", length = 64, nullable = false)
    private String action;

    /** Motivo informado pelo usuário para justificar a mudança de status. */
    @Column(name = "reason", length = 1024, nullable = false)
    private String reason;

    /** Origem operacional que executou a ação no sistema. */
    @Column(name = "changed_by", length = 191, nullable = false)
    private String changedBy;

    /** Momento em que a mudança foi registrada pelo backend. */
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    /** Garante data e origem padrão antes de persistir o histórico. */
    @PrePersist
    void applyDefaults() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
        if (changedBy == null || changedBy.isBlank()) {
            changedBy = "SYSTEM";
        }
    }
}
