package com.marketinghub.worker.geralanding.imageplanning;

import com.marketinghub.worker.geralanding.imageplanning.dto.GeraLandingStageExecutionImagePlanningDto;
import com.marketinghub.worker.geralanding.imageplanning.dto.GeraLandingStageExecutionDetailDto;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsável por buscar jobs pendentes da etapa image-planning via controller imageplanning.web. */
@Service
public class ImagePlanningPendingJobsService {
    private static final String STAGE_CODE = "landing-page-image-planning";
    private final GeraLandingImagePlanningBackendClient backendClient;
    public ImagePlanningPendingJobsService(GeraLandingImagePlanningBackendClient backendClient) { this.backendClient = backendClient; }
    /** Lista jobs pendentes de image-planning validados no endpoint da etapa. */
    public List<GeraLandingStageExecutionImagePlanningDto> listPendingImagePlanningJobs(int limit) {
        return backendClient.listPendingExecutions(limit).stream().filter(this::isStage).filter(this::isPending).toList();
    }
    /** Valida se a execução pertence à etapa image-planning. */
    private boolean isStage(GeraLandingStageExecutionImagePlanningDto execution) { return execution != null && StringUtils.hasText(execution.stageCode()) && STAGE_CODE.equals(execution.stageCode().trim().toLowerCase(Locale.ROOT)); }
    /** Confirma via endpoint imageplanning.web que o job segue em INICIADO. */
    private boolean isPending(GeraLandingStageExecutionImagePlanningDto execution) {
        if (execution == null || execution.experimentId() == null || !StringUtils.hasText(execution.idJob())) return false;
        GeraLandingStageExecutionDetailDto d = backendClient.fetchStageExecutionDetail(execution.experimentId(), execution.idJob());
        return d != null && "INICIADO".equalsIgnoreCase(d.status());
    }
}
