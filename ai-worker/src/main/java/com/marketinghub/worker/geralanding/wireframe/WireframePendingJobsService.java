package com.marketinghub.worker.geralanding.wireframe;

import com.marketinghub.worker.geralanding.GeraLandingBackendClient;
import com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Responsável por buscar no controller de wireframe os jobs pendentes de processamento do GeraLanding.
 */
@Service
public class WireframePendingJobsService {
    private static final Logger log = LoggerFactory.getLogger(WireframePendingJobsService.class);
    private static final String WIREFRAME_STAGE_CODE = "landing-page-wireframe";

    private final GeraLandingBackendClient backendClient;

    public WireframePendingJobsService(GeraLandingBackendClient backendClient) {
        this.backendClient = backendClient;
    }

    /**
     * Busca execuções pendentes da etapa wireframe e confirma o estado via controller wireframe.web.
     */
    public List<GeraLandingStageExecutionDto> listPendingWireframeJobs(int limit) {
        List<GeraLandingStageExecutionDto> pendingExecutions = backendClient.listPendingExecutions(limit);
        List<GeraLandingStageExecutionDto> wireframeJobs = pendingExecutions.stream()
                .filter(this::isWireframeStage)
                .filter(this::isPendingOnWireframeController)
                .toList();
        log.info("Wireframe pending jobs fetched: {} (from total pending={})", wireframeJobs.size(), pendingExecutions.size());
        return wireframeJobs;
    }

    /**
     * Valida se a execução pertence à etapa canônica de wireframe.
     */
    private boolean isWireframeStage(GeraLandingStageExecutionDto execution) {
        return execution != null
                && StringUtils.hasText(execution.stageCode())
                && WIREFRAME_STAGE_CODE.equals(execution.stageCode().trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Consulta o endpoint wireframe.web para confirmar que o job segue pendente.
     */
    private boolean isPendingOnWireframeController(GeraLandingStageExecutionDto execution) {
        if (execution == null || execution.experimentId() == null || !StringUtils.hasText(execution.idJob())) {
            return false;
        }
        GeraLandingStageExecutionDetailDto detail = backendClient.fetchWireframeStageExecutionDetail(
                execution.experimentId(),
                execution.idJob());
        return detail != null && "INICIADO".equalsIgnoreCase(detail.status());
    }
}
