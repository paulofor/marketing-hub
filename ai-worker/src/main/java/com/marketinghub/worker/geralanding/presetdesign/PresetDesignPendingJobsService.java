package com.marketinghub.worker.geralanding.presetdesign;

import com.marketinghub.worker.geralanding.GeraLandingBackendClient;
import com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Responsável por buscar jobs pendentes da etapa preset-design via controller designpreset.web. */
@Service
public class PresetDesignPendingJobsService {
    private static final String STAGE_CODE = "landing-page-design-preset";
    private final GeraLandingBackendClient backendClient;
    public PresetDesignPendingJobsService(GeraLandingBackendClient backendClient) { this.backendClient = backendClient; }
    /** Lista jobs pendentes de preset-design validados no endpoint da etapa. */
    public List<GeraLandingStageExecutionDto> listPendingPresetDesignJobs(int limit) {
        return backendClient.listPendingExecutions(limit).stream().filter(this::isStage).filter(this::isPending).toList();
    }
    /** Valida se a execução pertence à etapa preset-design. */
    private boolean isStage(GeraLandingStageExecutionDto execution) { return execution != null && StringUtils.hasText(execution.stageCode()) && STAGE_CODE.equals(execution.stageCode().trim().toLowerCase(Locale.ROOT)); }
    /** Confirma via endpoint designpreset.web que o job segue em INICIADO. */
    private boolean isPending(GeraLandingStageExecutionDto execution) {
        if (execution == null || execution.experimentId() == null || !StringUtils.hasText(execution.idJob())) return false;
        GeraLandingStageExecutionDetailDto d = backendClient.fetchDesignPresetStageExecutionDetail(execution.experimentId(), execution.idJob());
        return d != null && "INICIADO".equalsIgnoreCase(d.status());
    }
}
