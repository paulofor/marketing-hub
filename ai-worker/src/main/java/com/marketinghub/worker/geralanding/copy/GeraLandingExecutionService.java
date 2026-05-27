package com.marketinghub.worker.geralanding.copy;

import java.util.List;
import org.springframework.stereotype.Service;

/** Responsabilidade: encapsular execução da etapa copy preservando isolamento de pacote. */
@Service("geraLandingCopyExecutionStageService")
public class GeraLandingExecutionService {
    private final com.marketinghub.worker.geralanding.GeraLandingExecutionService delegate;

    public GeraLandingExecutionService(com.marketinghub.worker.geralanding.GeraLandingExecutionService delegate) {
        this.delegate = delegate;
    }

    /** Processa as execuções da etapa copy convertendo para DTO base do pipeline. */
    public void processExecutions(List<GeraLandingStageExecutionDto> jobs) {
        List<com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto> mapped = jobs.stream()
                .map(item -> new com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto(
                        item.experimentId(), item.idJob(), item.stageCode()))
                .toList();
        delegate.processExecutions(mapped);
    }
}
