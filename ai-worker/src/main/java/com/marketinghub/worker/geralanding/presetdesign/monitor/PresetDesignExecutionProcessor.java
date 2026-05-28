package com.marketinghub.worker.geralanding.presetdesign.monitor;

import com.marketinghub.worker.geralanding.presetdesign.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.presetdesign.request.GeraLandingPresetDesignOpenAiExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Processa os jobs pendentes da etapa design-preset com o executor OpenAI da própria etapa. */
@Service
public class PresetDesignExecutionProcessor {
    private final GeraLandingPresetDesignOpenAiExecutionService executionService;

    public PresetDesignExecutionProcessor(GeraLandingPresetDesignOpenAiExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa a lista de jobs pendentes retornada pelo polling da etapa design-preset. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) {
        executionService.processExecutions(jobs);
    }
}
