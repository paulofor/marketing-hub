package com.marketinghub.geralanding.internal.web;

import com.marketinghub.geralanding.GeraLandingDispatchReceiveRequest;
import com.marketinghub.geralanding.GeraLandingPendingExecutionResponse;
import com.marketinghub.geralanding.GeraLandingPromptReceiveDirectRequest;
import com.marketinghub.geralanding.GeraLandingPromptReceiveRequest;
import com.marketinghub.geralanding.GeraLandingResultReceiveRequest;
import com.marketinghub.geralanding.GeraLandingStageExecutionService;
import com.marketinghub.geralanding.GeraLandingWorkerPromptRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável pelos endpoints internos de orquestração das etapas do GeraLanding. */
@RestController
@RequestMapping("/api/internal/geralanding")
public class GeraLandingInternalController {

  private final GeraLandingStageExecutionService executionService;

  public GeraLandingInternalController(GeraLandingStageExecutionService executionService) {
    this.executionService = executionService;
  }

  /** Registra a execução enviada pelo worker antes do processamento da etapa. */
  @PostMapping("/stage-executions")
  public ResponseEntity<Void> registerWorkerPrompt(
      @Valid @RequestBody GeraLandingWorkerPromptRequest request) {
    executionService.registerWorkerPromptExecution(request);
    return ResponseEntity.accepted().build();
  }

  /** Recebe o prompt gerado para uma execução específica. */
  @PostMapping("/stage-executions/{idJob}/receive-prompt")
  public ResponseEntity<Void> receivePrompt(
      @PathVariable String idJob, @Valid @RequestBody GeraLandingPromptReceiveRequest request) {
    executionService.receivePrompt(idJob, request);
    return ResponseEntity.accepted().build();
  }

  /** Recebe prompt via payload direto contendo idJob no corpo da requisição. */
  @PostMapping("/stage-executions/receive-prompt")
  public ResponseEntity<Void> receivePromptDirect(
      @Valid @RequestBody GeraLandingPromptReceiveDirectRequest request) {
    executionService.receivePrompt(
        request.idJob(),
        new GeraLandingPromptReceiveRequest(
            request.experimentId(), request.stageCode(), request.prompt(), null, null, null, null,
            null));
    return ResponseEntity.accepted().build();
  }

  /** Recebe o resultado final de uma execução de etapa. */
  @PostMapping("/stage-executions/{idJob}/receive-result")
  public ResponseEntity<Void> receiveResult(
      @PathVariable String idJob, @Valid @RequestBody GeraLandingResultReceiveRequest request) {
    executionService.receiveResult(idJob, request);
    return ResponseEntity.accepted().build();
  }

  /** Marca a execução como despachada para OpenAI. */
  @PostMapping("/stage-executions/{idJob}/receive-dispatch")
  public ResponseEntity<Void> receiveDispatch(
      @PathVariable String idJob, @Valid @RequestBody GeraLandingDispatchReceiveRequest request) {
    executionService.markAsSentToOpenAi(idJob, request);
    return ResponseEntity.accepted().build();
  }

  /** Lista execuções pendentes de retorno da OpenAI. */
  @GetMapping("/stage-executions/pending")
  public ResponseEntity<List<GeraLandingPendingExecutionResponse>> listPendingExecutions() {
    return ResponseEntity.ok(executionService.listPendingExecutions());
  }
}
