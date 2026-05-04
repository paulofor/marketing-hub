package com.marketinghub.worker.geralanding;

import com.marketinghub.worker.experimentpipeline.ExperimentPipelineJobDto;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GeraLandingExecutionService {
    private static final Logger log = LoggerFactory.getLogger(GeraLandingExecutionService.class);
    private static final String STAGE_WIREFRAME = "landing-page-wireframe";

    private final GeraLandingBackendClient backendClient;
    private final GeraLandingService geraLandingService;
    private final int pendingLimit;

    public GeraLandingExecutionService(GeraLandingBackendClient backendClient,
                                       GeraLandingService geraLandingService,
                                       @Value("${geralanding.execution.pending-limit:20}") int pendingLimit) {
        this.backendClient = backendClient;
        this.geraLandingService = geraLandingService;
        this.pendingLimit = Math.max(1, pendingLimit);
    }

    public void processPendingExecutions() {
        List<GeraLandingStageExecutionDto> pending = backendClient.listPendingExecutions(pendingLimit);
        log.info("GeraLanding execution worker found {} pending execution(s)", pending.size());
        for (GeraLandingStageExecutionDto execution : pending) {
            processExecution(execution);
        }
    }

    private void processExecution(GeraLandingStageExecutionDto execution) {
        if (execution == null || !StringUtils.hasText(execution.stageCode())) {
            return;
        }
        String normalizedStage = execution.stageCode().trim().toLowerCase(Locale.ROOT);
        if (!STAGE_WIREFRAME.equals(normalizedStage)) {
            return;
        }
        try {
            ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                    UUID.randomUUID(),
                    execution.experimentId(),
                    execution.stageCode(),
                    null,
                    null,
                    "{}",
                    Instant.now());
            String promptMontado = geraLandingService.montarERegistrarPromptEtapa(job, normalizedStage, execution.idJob() != null ? execution.idJob().toString() : null);
            backendClient.receivePrompt(execution.idJob(), execution.experimentId(), execution.stageCode(), promptMontado);
            log.info("Prompt de gera-landing wireframe montado para executionId={} (experimentId={})",
                    execution.idJob(), execution.experimentId());
        } catch (Exception ex) {
            log.error("Falha ao montar prompt da etapa wireframe para executionId={} (experimentId={})",
                    execution.idJob(), execution.experimentId(), ex);
        }
    }
}
