package com.marketinghub.geralanding.qualityreview.controller;

import com.marketinghub.geralanding.qualityreview.service.BackendQualityReviewService;
import com.marketinghub.geralanding.qualityreview.service.GeraLandingQualityReviewStartResponse;
import com.marketinghub.geralanding.qualityreview.service.detailStageExecution.RecordBackendQualityReviewDetalheDto;
import com.marketinghub.geralanding.qualityreview.service.listStageExecutions.GeraLandingQualityReviewExecutionSummaryResponse;
import com.marketinghub.geralanding.qualityreview.service.pending.RecordQualityReviewPending;
import com.marketinghub.geralanding.qualityreview.service.recebePrompt.RecebePromptRequest;
import com.marketinghub.geralanding.qualityreview.service.recebeResposta.RecebeRespostaRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsável pelos endpoints da etapa Quality Gate visual da landing do GeraLanding. */
@RestController
@RequestMapping("/api")
public class BackendQualityReviewController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendQualityReviewController.class);

    private final BackendQualityReviewService executionService;

    /** Inicializa o controller com o serviço da etapa de revisão visual de qualidade. */
    public BackendQualityReviewController(BackendQualityReviewService executionService) {
        this.executionService = executionService;
    }

    /** Agenda a revisão visual de qualidade da landing atual do experimento. */
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

    /** Lista os jobs pendentes iniciados da etapa quality-review para processamento pelo Worker AI. */
    @GetMapping("/internal/geralanding/quality-review/stage-executions/pending")
    public List<RecordQualityReviewPending> pending() {
        return executionService.listPending();
    }

    /** Recebe prompt, schema e request visual enviados para IA e marca a execução aguardando retorno da OpenAI. */
    @PostMapping("/internal/geralanding/quality-review/stage-executions/{idJob}/recebe-prompt")
    public ResponseEntity<Void> recebePrompt(
            @PathVariable String idJob,
            @Valid @RequestBody RecebePromptRequest payload) {
        LOGGER.info(
                "[GeraLanding][QualityReview] Recebido request visual enviado para IA idJob={} jobidopenai={} promptLength={} promptMarkdownLength={} schemaLength={} requestBodyLength={}",
                idJob,
                payload.jobidopenai(),
                payload.prompt().length(),
                payload.promptMarkdownContent() != null ? payload.promptMarkdownContent().length() : 0,
                payload.schemaJson().length(),
                payload.requestBodyJson().length());
        executionService.markWaitingOpenAiDispatch(
                idJob,
                payload.prompt(),
                payload.promptMarkdownContent(),
                payload.schemaJson(),
                payload.requestBodyJson(),
                payload.jobidopenai(),
                payload.qualityReviewAudit());
        return ResponseEntity.accepted().build();
    }

    /** Recebe a resposta da IA para a revisão visual e conclui a execução do job. */
    @PostMapping("/internal/geralanding/quality-review/stage-executions/{idJob}/recebe-resposta")
    public ResponseEntity<Void> recebeResposta(
            @PathVariable String idJob,
            @Valid @RequestBody RecebeRespostaRequest payload) {
        LOGGER.info(
                "[GeraLanding][QualityReview] Recebida resposta da IA idJob={} experimentId={} stageCode={} openAiJobId={} inputTokens={} outputTokens={} costUsd={} hasError={}",
                idJob,
                payload.experimentId(),
                payload.stageCode(),
                payload.openAiJobId(),
                payload.inputTokens(),
                payload.outputTokens(),
                payload.costUsd(),
                payload.errorMessage() != null && !payload.errorMessage().isBlank());
        executionService.markCompletedFromResponse(
                idJob,
                payload.experimentId(),
                payload.stageCode(),
                payload.modelResponse(),
                payload.inputTokens(),
                payload.outputTokens(),
                payload.costUsd(),
                payload.openAiJobId(),
                payload.errorMessage(),
                payload.errorDetail());
        return ResponseEntity.accepted().build();
    }

    /** Retorna os detalhes de uma execução específica da revisão de qualidade. */
    @GetMapping("/experiments/{experimentId}/geralanding/quality-review/stage-executions/{idJob}")
    public ResponseEntity<RecordBackendQualityReviewDetalheDto> detailStageExecution(
            @PathVariable Long experimentId,
            @PathVariable String idJob) {
        return ResponseEntity.ok(executionService.getStageExecutionDetail(experimentId, idJob));
    }
}
