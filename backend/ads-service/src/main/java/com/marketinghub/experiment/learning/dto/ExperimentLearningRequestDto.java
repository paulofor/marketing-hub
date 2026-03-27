package com.marketinghub.experiment.learning.dto;

import com.marketinghub.experiment.learning.ExperimentLearningStatus;
import java.time.Instant;
import lombok.Data;

/**
 * Resumo de uma solicitação de aprendizado vinculada a um experimento específico.
 */
@Data
public class ExperimentLearningRequestDto {
    private Long id;
    private Long experimentId;
    private ExperimentLearningStatus status;
    private Instant requestedAt;
    private Instant completedAt;
    private String requestedBy;
    private String failureReason;
}
