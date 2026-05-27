package com.marketinghub.worker.geralanding.presetdesign;

import com.marketinghub.worker.geralanding.presetdesign.openai.GeraLandingPresetDesignOpenAiExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa preset design usando executor da própria etapa. */
@Service
public class GeraLandingPresetDesignExecutionService {
    private final GeraLandingPresetDesignOpenAiExecutionService executionService;
    public GeraLandingPresetDesignExecutionService(GeraLandingPresetDesignOpenAiExecutionService executionService) { this.executionService = executionService; }
    /** Processa os jobs pendentes da etapa preset design. */
    public void processExecutions(List<GeraLandingStageExecutionPresetDesignDto> jobs) { executionService.processExecutions(jobs); }
}
