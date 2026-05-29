package com.marketinghub.geralanding.wireframe.web;

import com.marketinghub.geralanding.wireframe.service.BackendWireframeService;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeExecutionSummaryResponse;
import com.marketinghub.geralanding.wireframe.service.GeraLandingWireframeStartResponse;
import com.marketinghub.geralanding.wireframe.service.RecordBackendWireframeDetalheDto;
import com.marketinghub.geralanding.wireframe.service.pending.RecordWireframePending;
import com.marketinghub.geralanding.wireframe.service.recebeprompt.RecebePromptRequest;
import com.marketinghub.geralanding.wireframe.service.receberesposta.RecebeRespostaRequest;
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

/** Responsável por expor os endpoints de backend da etapa landing-page-wireframe no GeraLanding. */
@RestController
@RequestMapping("/api")
public class BackendWireframeController {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendWireframeController.class);
  private static final String STAGE_CODE = "landing-page-wireframe";

  private final BackendWireframeService executionService;

  /** Inicializa o controller com o serviço backend da etapa wireframe. */
  public BackendWireframeController(BackendWireframeService executionService) {
    this.executionService = executionService;
  }

  /** Registra uma execução inicial da etapa landing-page-wireframe. */
  @PostMapping("/experiments/{experimentId}/geralanding/wireframe/start")
  public ResponseEntity<GeraLandingWireframeStartResponse> start(@PathVariable Long experimentId) {
    GeraLandingWireframeStartResponse response = executionService.start(experimentId);
    return ResponseEntity.accepted().body(response);
  }

  /** Lista as execuções da etapa para o experimento. */
  @GetMapping("/experiments/{experimentId}/geralanding/wireframe/stage-executions")
  public ResponseEntity<List<GeraLandingWireframeExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long experimentId,
      @RequestParam(defaultValue = "true") boolean includeCompleted) {
    List<GeraLandingWireframeExecutionSummaryResponse> response =
        executionService.listExperimentStageExecutions(experimentId, STAGE_CODE, includeCompleted);
    return ResponseEntity.ok(response);
  }

  /** Lista os jobs pendentes iniciados da etapa wireframe para processamento pelo Worker AI. */
  @GetMapping("/internal/geralanding/wireframe/stage-executions/pending")
  public List<RecordWireframePending> pending() {
    return executionService.listPending(STAGE_CODE);
  }

  /** Recebe o prompt enviado para IA, registra o prompt e marca a execução como aguardando retorno da OpenAI. */
  @PostMapping("/internal/geralanding/wireframe/stage-executions/{idJob}/recebe-prompt")
  public ResponseEntity<Void> recebePrompt(
      @PathVariable String idJob, @Valid @RequestBody RecebePromptRequest payload) {
    LOGGER.info(
        "[GeraLanding][Wireframe] Recebido prompt enviado para IA idJob={} jobidopenai={} prompt={}",
        idJob,
        payload.jobidopenai(),
        payload.prompt());
    executionService.markWaitingOpenAiDispatch(idJob, payload.prompt(), payload.jobidopenai());
    return ResponseEntity.accepted().build();
  }

  /** Recebe a resposta da IA para a etapa wireframe e conclui a execução do job. */
  @PostMapping("/internal/geralanding/wireframe/stage-executions/{idJob}/recebe-resposta")
  public ResponseEntity<Void> recebeResposta(
      @PathVariable String idJob, @Valid @RequestBody RecebeRespostaRequest payload) {
    LOGGER.info(
        "[GeraLanding][Wireframe] Recebida resposta da IA idJob={} experimentId={} stageCode={} openAiJobId={} inputTokens={} outputTokens={} costUsd={}",
        idJob,
        payload.experimentId(),
        payload.stageCode(),
        payload.openAiJobId(),
        payload.inputTokens(),
        payload.outputTokens(),
        payload.costUsd());
    executionService.markCompletedFromResponse(
        idJob,
        payload.experimentId(),
        payload.stageCode(),
        payload.modelResponse(),
        payload.inputTokens(),
        payload.outputTokens(),
        payload.costUsd(),
        payload.openAiJobId());
    return ResponseEntity.accepted().build();
  }

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/experiments/{experimentId}/geralanding/wireframe/stage-executions/{idJob}")
  public ResponseEntity<RecordBackendWireframeDetalheDto> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    RecordBackendWireframeDetalheDto response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
