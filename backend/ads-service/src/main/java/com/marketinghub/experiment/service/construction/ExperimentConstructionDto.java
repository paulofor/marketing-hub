package com.marketinghub.experiment.service.construction;

import com.marketinghub.experiment.ExperimentCreationSource;
import java.time.Instant;
import java.util.List;

/** Resposta consolidada que explica como um experimento foi construído. */
public record ExperimentConstructionDto(
        Long experimentId,
        String experimentName,
        ExperimentCreationSource creationSource,
        boolean manualFlow,
        Instant createdAt,
        Instant updatedAt,
        List<ExperimentConstructionSectionDto> sections) {}
