package com.marketinghub.agenttask;

import com.marketinghub.agenttask.service.pending.AgentTaskOperatorGuidance;
import com.marketinghub.agenttask.service.pending.AgentTaskPendingWithOperatorGuidance;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Responsabilidade: expor a fila canônica de atividades BPM aos executores dos agentes. */
@RestController
@RequestMapping("/api/internal/agent-tasks/{agentKey}/stage-executions")
public class InternalAgentTaskExecutionController {
  @Autowired
  private com.marketinghub.agenttask.audioreview.v1.service.AgentAudioReviewService audioReviews;

  private final AgentTaskService service;
  private final AgentTaskVisualEvidenceService visualEvidenceService;
  private final AgentTaskOperatorGuidance operatorGuidance;

  /** Inicializa o contrato operacional e a consulta segregada das orientações do produto. */
  @Autowired
  public InternalAgentTaskExecutionController(
      AgentTaskService service,
      AgentTaskVisualEvidenceService visualEvidenceService,
      AgentTaskOperatorGuidance operatorGuidance) {
    this.service = service;
    this.visualEvidenceService = visualEvidenceService;
    this.operatorGuidance = operatorGuidance;
  }

  /** Preserva testes e consumidores do transporte anterior à orientação opcional. */
  public InternalAgentTaskExecutionController(
      AgentTaskService service, AgentTaskVisualEvidenceService visualEvidenceService) {
    this(service, visualEvidenceService, null);
  }

  /** Reserva pelo handshake da fila e entrega as notas segregadas do próprio produto. */
  @GetMapping("/pending")
  public List<AgentTaskPendingWithOperatorGuidance> pending(
      @PathVariable String agentKey,
      @RequestParam(required = false) String processCode,
      @RequestParam(required = false) String activityId,
      @RequestParam(required = false) String executionResourceCode,
      @Parameter(
              description =
                  "Contrato versionado do executor. Atena em pde-commercial-plan-offer/marketStrategy exige ATENA_PDE_MARKET_STRATEGY_V1; sem contrato essa fila não reserva tarefas.")
          @RequestParam(required = false)
          String workerContract) {
    return service
        .claimEligibleProcessTask(
            agentKey, processCode, activityId, executionResourceCode, workerContract)
        .map(this::withOperatorGuidance)
        .map(List::of)
        .orElseGet(List::of);
  }

  /** Reexpõe a lease ativa com as notas atuais, sem reservar tarefa nem repetir inferência. */
  @GetMapping("/{taskId}")
  public AgentTaskPendingWithOperatorGuidance claimed(
      @PathVariable String agentKey, @PathVariable Long taskId) {
    return withOperatorGuidance(service.claimedProcessTask(agentKey, taskId));
  }

  /** Mantém todos os campos originais e acrescenta somente a evidência do operador identificada. */
  private AgentTaskPendingWithOperatorGuidance withOperatorGuidance(AgentTaskPendingResponse task) {
    return new AgentTaskPendingWithOperatorGuidance(
        task, operatorGuidance == null ? null : operatorGuidance.read(task));
  }

  /** Preserva o prompt resolvido e a configuração antes de qualquer término da tarefa. */
  @PutMapping("/{taskId}/execution-audit")
  public ResponseEntity<Void> recordExecutionAudit(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @Valid @RequestBody AgentTaskExecutionAuditRequest request) {
    service.recordClaimedProcessTaskExecutionAudit(agentKey, taskId, request);
    return ResponseEntity.noContent().build();
  }

  /** Recebe uma captura Playwright privada antes de permitir o parecer visual do agente. */
  @PostMapping(value = "/{taskId}/visual-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public AgentTaskVisualEvidenceResponse uploadVisualEvidence(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @RequestParam String captureSessionId,
      @RequestParam String evidenceKey,
      @RequestParam String evidenceType,
      @RequestParam String deviceProfile,
      @RequestParam Integer pageNumber,
      @RequestParam(required = false) Integer foldNumber,
      @RequestParam Integer viewportWidth,
      @RequestParam Integer viewportHeight,
      @RequestParam Integer pageHeightPx,
      @RequestParam Integer scrollY,
      @RequestParam String sourceUrl,
      @RequestParam String finalUrl,
      @RequestParam Instant capturedAt,
      @RequestPart("file") MultipartFile file)
      throws java.io.IOException {
    return visualEvidenceService.store(
        agentKey,
        taskId,
        new AgentTaskVisualEvidenceRequest(
            captureSessionId,
            evidenceKey,
            evidenceType,
            deviceProfile,
            pageNumber,
            foldNumber,
            viewportWidth,
            viewportHeight,
            pageHeightPx,
            scrollY,
            sourceUrl,
            finalUrl,
            capturedAt),
        file);
  }

  /** Recebe resultado e evidências antes de liberar a atividade seguinte. */
  @PostMapping("/{taskId}/result")
  public ResponseEntity<Void> complete(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @Valid @RequestBody CompleteAgentTaskRequest request) {
    service.completeClaimedProcessTask(agentKey, taskId, request);
    return ResponseEntity.noContent().build();
  }

  /** Persiste a falha sem avançar o processo e preserva cancelamentos anteriores ao retorno. */
  @PostMapping("/{taskId}/failure")
  public ResponseEntity<Void> fail(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @Valid @RequestBody FailAgentTaskRequest request) {
    service.failClaimedProcessTask(agentKey, taskId, request);
    return ResponseEntity.noContent().build();
  }

  /** Reserva e audita a ferramenta sonora antes de qualquer consumo externo. */
  @PostMapping("/{taskId}/audio-reviews")
  public com.marketinghub.agenttask.audioreview.v1.service.receipt.AudioReviewReceipt reserveAudio(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @RequestBody
          com.marketinghub.agenttask.audioreview.v1.service.reserve.AudioReviewReserveRequest
              request) {
    return audioReviews.reserve(agentKey, taskId, request);
  }

  /** Persiste o retorno bruto e o consumo auditivo da chamada reservada. */
  @PostMapping("/{taskId}/audio-reviews/{assetId}/result")
  public com.marketinghub.agenttask.audioreview.v1.service.receipt.AudioReviewReceipt completeAudio(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @PathVariable Long assetId,
      @RequestBody
          com.marketinghub.agenttask.audioreview.v1.service.result.AudioReviewResultRequest
              request) {
    return audioReviews.complete(agentKey, taskId, assetId, request);
  }

  /** Recupera recibos auditivos sem provocar nova inferência. */
  @GetMapping("/{taskId}/audio-reviews")
  public List<com.marketinghub.agenttask.audioreview.v1.service.receipt.AudioReviewReceipt>
      listAudio(@PathVariable String agentKey, @PathVariable Long taskId) {
    return audioReviews.list(agentKey, taskId);
  }
}
