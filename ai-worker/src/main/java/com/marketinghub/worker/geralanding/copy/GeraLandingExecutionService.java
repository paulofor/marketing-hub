package com.marketinghub.worker.geralanding.copy;

import com.marketinghub.worker.geralanding.copy.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.copy.request.GeraLandingCopyOpenAiExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Responsabilidade: encapsular execução da etapa copy preservando isolamento de pacote. */
@Service("geraLandingCopyExecutionStageService")
public class GeraLandingExecutionService {
    private final GeraLandingCopyOpenAiExecutionService executionService;
    public GeraLandingExecutionService(GeraLandingCopyOpenAiExecutionService executionService) { this.executionService = executionService; }
    /** Processa as execuções da etapa copy. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) { executionService.processExecutions(jobs); }
}
