package com.marketinghub.geralanding.qualityreview.controller;

import com.marketinghub.geralanding.qualityreview.service.BackendQualityReviewService;
import com.marketinghub.geralanding.qualityreview.service.GeraLandingQualityReviewStartResponse;
import com.marketinghub.geralanding.qualityreview.service.detailStageExecution.RecordBackendQualityReviewDetalheDto;
import com.marketinghub.geralanding.qualityreview.service.listStageExecutions.GeraLandingQualityReviewExecutionSummaryResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsável pelos endpoints da etapa Quality Gate da landing do GeraLanding. */
@RestController
@RequestMapping("/api")
public class BackendQualityReviewController {

    private final BackendQualityReviewService executionService;

    /** Inicializa o controller com o serviço da etapa de revisão de qualidade. */
    public BackendQualityReviewController(BackendQualityReviewService executionService) {
        this.executionService = executionService;
    }

    /** Executa a revisão de qualidade da landing atual do experimento. */
    @PostMapping("/experiments/{experimentId}/geralanding/quality-review/start")
    public ResponseEntity<GeraLandingQualityReviewStartResponse> start(@PathVariable Long experimentId) {
        return ResponseEntity.accepted().body(executionService.start(experimentId));
    }

    /** Lista as execuções da etapa de revisão de qualidade para o experimento. */
    @GetMapping("/experiments/{experimentId}/geralanding/quality-review/stage-executions")
    public ResponseEntity<List<GeraLandingQualityReviewExecutionSummaryResponse>> listStageExecutions(
            @PathVariable Long experimentId,
            @RequestParam(defaultValue = "true") boolean includeCompleted) {
        return ResponseEntity.ok(executionService.listExperimentStageExecutions(experimentId, includeCompleted));
    }

    /** Retorna os detalhes de uma execução específica da revisão de qualidade. */
    @GetMapping("/experiments/{experimentId}/geralanding/quality-review/stage-executions/{idJob}")
    public ResponseEntity<RecordBackendQualityReviewDetalheDto> detailStageExecution(
            @PathVariable Long experimentId,
            @PathVariable String idJob) {
        return ResponseEntity.ok(executionService.getStageExecutionDetail(experimentId, idJob));
    }
}
