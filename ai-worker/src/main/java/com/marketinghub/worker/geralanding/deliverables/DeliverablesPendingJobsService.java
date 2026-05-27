package com.marketinghub.worker.geralanding.deliverables;

import com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingStageExecutionDeliverablesDto;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsável por buscar jobs pendentes da etapa deliverables via controller deliverables.web. */
@Service
public class DeliverablesPendingJobsService {
    private static final String STAGE_CODE = "landing-page-deliverables";
    private final GeraLandingDeliverablesBackendClient backendClient;
    public DeliverablesPendingJobsService(GeraLandingDeliverablesBackendClient backendClient) { this.backendClient = backendClient; }
    /** Lista jobs pendentes de deliverables validados no endpoint da etapa. */
    public List<GeraLandingStageExecutionDeliverablesDto> listPendingDeliverablesJobs(int limit) {
        return backendClient.listPendingExecutions(limit).stream().filter(this::isStage).filter(this::isPending).toList();
    }
    /** Valida se a execução pertence à etapa deliverables. */
    private boolean isStage(GeraLandingStageExecutionDeliverablesDto execution) { return execution != null && StringUtils.hasText(execution.stageCode()) && STAGE_CODE.equals(execution.stageCode().trim().toLowerCase(Locale.ROOT)); }
    /** Confirma via endpoint deliverables.web que o job segue em INICIADO. */
    private boolean isPending(GeraLandingStageExecutionDeliverablesDto execution) {
        if (execution == null || execution.experimentId() == null || !StringUtils.hasText(execution.idJob())) return false;
        GeraLandingStageExecutionDetailDto d = backendClient.fetchStageExecutionDetail(execution.experimentId(), execution.idJob());
        return d != null && "INICIADO".equalsIgnoreCase(d.status());
    }
}
