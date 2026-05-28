package com.marketinghub.worker.geralanding.copy.monitor;

import com.marketinghub.worker.geralanding.copy.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.copy.GeraLandingCopyBackendClient;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsável por buscar jobs pendentes da etapa copy via controller copy.web. */
@Service
public class CopyPendingJobsService {
    private static final String STAGE_CODE = "landing-page-copy";
    private final GeraLandingCopyBackendClient backendClient;
    public CopyPendingJobsService(GeraLandingCopyBackendClient backendClient) { this.backendClient = backendClient; }
    /** Lista jobs pendentes de copy validados no endpoint da etapa. */
    public List<GeraLandingStageExecutionDetailDto> listPendingCopyJobs(int limit) {
        return backendClient.listPendingExecutions(limit).stream().filter(this::isCopyStage).filter(this::isPending).toList();
    }
    /** Valida se a execução pertence à etapa copy. */
    private boolean isCopyStage(GeraLandingStageExecutionDetailDto execution) {
        return execution != null && StringUtils.hasText(execution.stageCode()) && STAGE_CODE.equals(execution.stageCode().trim().toLowerCase(Locale.ROOT));
    }
    /** Confirma via endpoint copy.web que o job segue em INICIADO. */
    private boolean isPending(GeraLandingStageExecutionDetailDto execution) {
        if (execution == null || execution.experimentId() == null || !StringUtils.hasText(execution.idJob())) return false;
        GeraLandingStageExecutionDetailDto d = backendClient.fetchCopyStageExecutionDetail(execution.experimentId(), execution.idJob());
        return d != null && "INICIADO".equalsIgnoreCase(d.status());
    }
}
