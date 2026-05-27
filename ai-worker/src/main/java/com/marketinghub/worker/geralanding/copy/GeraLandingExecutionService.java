package com.marketinghub.worker.geralanding.copy;

import com.marketinghub.worker.geralanding.comum.GeraLandingStageExecutionRef;
import java.util.List;
import org.springframework.stereotype.Service;

/** Responsabilidade: encapsular execução da etapa copy preservando isolamento de pacote. */
@Service("geraLandingCopyExecutionStageService")
public class GeraLandingExecutionService {
    private final com.marketinghub.worker.geralanding.comum.GeraLandingExecutionService executionService;

    public GeraLandingExecutionService(com.marketinghub.worker.geralanding.comum.GeraLandingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa as execuções da etapa copy convertendo para DTO base do pipeline. */
    public void processExecutions(List<GeraLandingStageExecutionDto> jobs) {
        executionService.processExecutions(jobs.stream()
                .map(item -> new GeraLandingStageExecutionRef(item.experimentId(), item.idJob(), item.stageCode()))
                .toList());
    }
}
