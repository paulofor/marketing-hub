package com.marketinghub.hypothesis.pain.controller;

import com.marketinghub.hypothesis.pain.service.HypothesisPainStageService;
import com.marketinghub.hypothesis.pain.service.detailStageExecution.HypothesisPainExecutionDetailResponse;
import com.marketinghub.hypothesis.pain.service.listStageExecutions.HypothesisPainExecutionSummaryResponse;
import com.marketinghub.hypothesis.pain.service.pending.HypothesisPainPendingExecution;
import com.marketinghub.hypothesis.pain.service.recebePrompt.RecebePromptRequest;
import com.marketinghub.hypothesis.pain.service.recebeResposta.RecebeRespostaRequest;
import com.marketinghub.hypothesis.pain.service.start.HypothesisPainStartResponse;
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

/** Responsável por expor os endpoints das etapas iniciais do pipeline de hipótese. */
@RestController
@RequestMapping("/api")
public class HypothesisPainStageController {
    private static final Logger log = LoggerFactory.getLogger(HypothesisPainStageController.class);
    private final HypothesisPainStageService service;

    /** Inicializa o controller com o serviço das etapas iniciais. */
    public HypothesisPainStageController(HypothesisPainStageService service) {
        this.service = service;
    }

    /** Registra uma execução inicial da etapa Dor para um nicho. */
    @PostMapping("/niches/{nicheId}/hypothesis-pipeline/pain/start")
    public ResponseEntity<HypothesisPainStartResponse> start(@PathVariable Long nicheId) {
        return ResponseEntity.accepted().body(service.start(nicheId));
    }

    /** Registra uma execução inicial da etapa Resultado para um nicho. */
    @PostMapping("/niches/{nicheId}/hypothesis-pipeline/result/start")
    public ResponseEntity<HypothesisPainStartResponse> startResult(@PathVariable Long nicheId) {
        return ResponseEntity.accepted().body(service.startResult(nicheId));
    }

    /** Lista execuções da etapa Dor para acompanhamento na tela de nova hipótese. */
    @GetMapping("/niches/{nicheId}/hypothesis-pipeline/pain/stage-executions")
    public ResponseEntity<List<HypothesisPainExecutionSummaryResponse>> listStageExecutions(
            @PathVariable Long nicheId,
            @RequestParam(defaultValue = "true") boolean includeCompleted) {
        return ResponseEntity.ok(service.listStageExecutions(nicheId, includeCompleted));
    }

    /** Lista execuções da etapa Resultado para acompanhamento na tela de nova hipótese. */
    @GetMapping("/niches/{nicheId}/hypothesis-pipeline/result/stage-executions")
    public ResponseEntity<List<HypothesisPainExecutionSummaryResponse>> listResultStageExecutions(
            @PathVariable Long nicheId,
            @RequestParam(defaultValue = "true") boolean includeCompleted) {
        return ResponseEntity.ok(service.listResultStageExecutions(nicheId, includeCompleted));
    }

    /** Consulta detalhe auditável de uma execução da etapa Dor vinculada ao nicho. */
    @GetMapping("/niches/{nicheId}/hypothesis-pipeline/pain/stage-executions/{idJob}")
    public ResponseEntity<HypothesisPainExecutionDetailResponse> detailForNiche(
            @PathVariable Long nicheId,
            @PathVariable String idJob) {
        return ResponseEntity.ok(service.detailForNiche(nicheId, idJob));
    }

    /** Consulta detalhe auditável de uma execução da etapa Resultado vinculada ao nicho. */
    @GetMapping("/niches/{nicheId}/hypothesis-pipeline/result/stage-executions/{idJob}")
    public ResponseEntity<HypothesisPainExecutionDetailResponse> detailResultForNiche(
            @PathVariable Long nicheId,
            @PathVariable String idJob) {
        return ResponseEntity.ok(service.detailForNiche(nicheId, idJob));
    }

    /** Consulta detalhe auditável de uma execução da etapa Dor para integrações internas. */
    @GetMapping("/internal/hypothesis-pipeline/pain/stage-executions/{idJob}")
    public ResponseEntity<HypothesisPainExecutionDetailResponse> detail(@PathVariable String idJob) {
        return ResponseEntity.ok(service.detail(idJob));
    }

    /** Lista jobs pendentes da etapa Dor para processamento pelo Worker AI. */
    @GetMapping("/internal/hypothesis-pipeline/pain/stage-executions/pending")
    public List<HypothesisPainPendingExecution> pending() {
        return service.listPending();
    }

    /** Lista jobs pendentes da etapa Resultado para processamento pelo Worker AI. */
    @GetMapping("/internal/hypothesis-pipeline/result/stage-executions/pending")
    public List<HypothesisPainPendingExecution> resultPending() {
        return service.listResultPending();
    }

    /** Marca uma execução de etapa como em processamento. */
    @PostMapping({"/internal/hypothesis-pipeline/pain/stage-executions/{idJob}/running", "/internal/hypothesis-pipeline/result/stage-executions/{idJob}/running"})
    public ResponseEntity<Void> running(@PathVariable String idJob) {
        service.markRunning(idJob);
        return ResponseEntity.accepted().build();
    }

    /** Recebe prompt, schema e request cru enviados para IA e marca a execução aguardando retorno. */
    @PostMapping({"/internal/hypothesis-pipeline/pain/stage-executions/{idJob}/recebe-prompt", "/internal/hypothesis-pipeline/result/stage-executions/{idJob}/recebe-prompt"})
    public ResponseEntity<Void> recebePrompt(
            @PathVariable String idJob,
            @Valid @RequestBody RecebePromptRequest payload) {
        log.info(
                "[HypothesisPipeline] Recebido request enviado para IA idJob={} jobidopenai={} promptLength={} promptMarkdownLength={} schemaLength={} requestBodyLength={}",
                idJob,
                payload.jobidopenai(),
                payload.prompt().length(),
                payload.promptMarkdownContent() != null ? payload.promptMarkdownContent().length() : 0,
                payload.schemaJson().length(),
                payload.requestBodyJson().length());
        service.markWaitingOpenAiDispatch(
                idJob,
                payload.prompt(),
                payload.promptMarkdownContent(),
                payload.schemaJson(),
                payload.requestBodyJson(),
                payload.openAiModel(),
                payload.jobidopenai());
        return ResponseEntity.accepted().build();
    }

    /** Recebe a resposta da IA para uma etapa e conclui a execução do job. */
    @PostMapping({"/internal/hypothesis-pipeline/pain/stage-executions/{idJob}/recebe-resposta", "/internal/hypothesis-pipeline/result/stage-executions/{idJob}/recebe-resposta"})
    public ResponseEntity<Void> recebeResposta(
            @PathVariable String idJob,
            @Valid @RequestBody RecebeRespostaRequest payload) {
        log.info(
                "[HypothesisPipeline] Recebida resposta da IA idJob={} marketNicheId={} stageCode={} openAiJobId={} inputTokens={} outputTokens={} costUsd={} hasError={}",
                idJob,
                payload.marketNicheId(),
                payload.stageCode(),
                payload.openAiJobId(),
                payload.inputTokens(),
                payload.outputTokens(),
                payload.costUsd(),
                payload.errorMessage() != null && !payload.errorMessage().isBlank());
        service.markCompletedFromResponse(idJob, payload);
        return ResponseEntity.accepted().build();
    }
}
