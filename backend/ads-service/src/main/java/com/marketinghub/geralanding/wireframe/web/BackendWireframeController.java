package com.marketinghub.geralanding.wireframe.web;

import com.marketinghub.geralanding.wireframe.service.BackendWireframeService;
import com.marketinghub.geralanding.wireframe.service.start.GeraLandingWireframeExecutionSummaryResponse;
import com.marketinghub.geralanding.wireframe.service.start.GeraLandingWireframeStartResponse;
import com.marketinghub.geralanding.wireframe.service.start.RecordBackendWireframeDetalheDto;
import com.marketinghub.geralanding.wireframe.service.start.RecordWireframePending;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
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

  /** Recebe o prompt enviado para IA sem alterar estado persistido nesta primeira versão. */
  @PostMapping("/internal/geralanding/wireframe/stage-executions/{idJob}/recebe-prompt")
  public ResponseEntity<Void> recebePrompt(
      @PathVariable String idJob, @Valid @RequestBody RecebePromptRequest payload) {
    return ResponseEntity.accepted().build();
  }

  /** Payload interno com o prompt enviado ao provedor de IA e o job aberto no OpenAI. */
  public record RecebePromptRequest(@NotBlank String prompt, @NotBlank String jobidopenai) {}

  /** Retorna os detalhes de uma execução específica da etapa. */
  @GetMapping("/experiments/{experimentId}/geralanding/wireframe/stage-executions/{idJob}")
  public ResponseEntity<RecordBackendWireframeDetalheDto> detailStageExecution(
      @PathVariable Long experimentId, @PathVariable String idJob) {
    RecordBackendWireframeDetalheDto response =
        executionService.getStageExecutionDetail(experimentId, idJob);
    return ResponseEntity.ok(response);
  }
}
