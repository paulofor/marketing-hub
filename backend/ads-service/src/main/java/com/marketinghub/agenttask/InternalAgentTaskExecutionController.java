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
                  "Contrato versionado do executor. Atena em"
                      + " pde-commercial-plan-offer/marketStrategy exige"
                      + " ATENA_PDE_MARKET_STRATEGY_REPLAY_V2; sem contrato essa fila não reserva"
                      + " tarefas.")
          @RequestParam(required = false)
          String workerContract) {
    // O rollout fecha o consumo antigo de Dédalo antes de disponibilizar o novo executor.
    if ("landing-generator".equals(agentKey)) return List.of();
    return service
        .claimEligibleProcessTask(
            agentKey, processCode, activityId, executionResourceCode, workerContract)
        .map(this::withOperatorGuidance)
        .map(List::of)
        .orElseGet(List::of);
  }

  /** Reserva apenas o recibo persistente; contexto é carregado depois pelo mesmo titular. */
  @GetMapping("/pending/receipt")
  public List<com.marketinghub.agenttask.service.receipt.DedaloClaimReceipt> pendingReceipt(
      @PathVariable String agentKey,
      @RequestParam String processCode,
      @RequestParam String activityId,
      @RequestParam String claimId) {
    requireDedalo(agentKey);
    return service
        .claimDedaloReceipt(processCode, activityId, claimId)
        .map(List::of)
        .orElseGet(List::of);
  }

  /** Carrega o contexto após o recibo, preservando identidade mesmo se o HTTP falhar. */
  @GetMapping("/{taskId}/claim-context")
  public AgentTaskPendingWithOperatorGuidance claimContext(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @RequestHeader("X-Dedalo-Claim") String claimId) {
    requireDedalo(agentKey);
    service.requireDedaloClaim(taskId, claimId);
    return withOperatorGuidance(service.claimedProcessTask(agentKey, taskId));
  }

  /** Confirma atividade do titular durante preparação ou execução sem registrar custo. */
  @PutMapping("/{taskId}/claim-heartbeat")
  public ResponseEntity<Void> claimHeartbeat(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @RequestHeader("X-Dedalo-Claim") String claimId) {
    requireDedalo(agentKey);
    service.heartbeatDedaloClaim(taskId, claimId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Concede início único após preparação; uma resposta perdida exige conferência, não repetição.
   */
  @PutMapping("/{taskId}/claim-start")
  public ResponseEntity<Void> claimStart(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @RequestHeader("X-Dedalo-Claim") String claimId,
      @Valid @RequestBody AgentTaskExecutionAuditRequest audit) {
    requireDedalo(agentKey);
    service.startDedaloClaim(taskId, claimId, audit);
    return ResponseEntity.noContent().build();
  }

  /** Impede uso do contrato dedicado por outro executor. */
  private void requireDedalo(String agentKey) {
    if (!"landing-generator".equals(agentKey))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND);
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

  /** Mantém chamada direta legada, exigindo identidade quando houver recibo novo. */
  public ResponseEntity<Void> recordExecutionAudit(
      String agentKey, Long taskId, AgentTaskExecutionAuditRequest request) {
    return recordExecutionAudit(agentKey, taskId, request, null);
  }

  /** Preserva o prompt resolvido e a configuração antes de qualquer término da tarefa. */
  @PutMapping("/{taskId}/execution-audit")
  public ResponseEntity<Void> recordExecutionAudit(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @Valid @RequestBody AgentTaskExecutionAuditRequest request,
      @RequestHeader(value = "X-Dedalo-Claim", required = false) String claimId) {
    service.requireCallbackClaim(agentKey, taskId, claimId);
    service.recordClaimedProcessTaskExecutionAudit(agentKey, taskId, request);
    return ResponseEntity.noContent().build();
  }

  /** Confirma o aceite dos bytes preservados sem conceder nova reserva ou inferência. */
  @GetMapping("/{taskId}/result-receipt")
  public com.marketinghub.agenttask.service.receipt.AgentTaskResultReceipt resultReceipt(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @RequestHeader(value = "X-Dedalo-Claim", required = false) String claimId) {
    service.requireCallbackClaim(agentKey, taskId, claimId);
    return service.resultReceipt(agentKey, taskId);
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

  /** Preserva chamada direta de consumidores legados, sem contornar fencing de novas reservas. */
  public ResponseEntity<Void> complete(
      String agentKey, Long taskId, CompleteAgentTaskRequest request) {
    return complete(agentKey, taskId, request, null);
  }

  /** Preserva chamada direta de consumidores legados, sem contornar fencing de novas reservas. */
  public ResponseEntity<Void> fail(String agentKey, Long taskId, FailAgentTaskRequest request) {
    return fail(agentKey, taskId, request, null);
  }

  /** Recebe resultado e evidências antes de liberar a atividade seguinte. */
  @PostMapping("/{taskId}/result")
  public ResponseEntity<Void> complete(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @Valid @RequestBody CompleteAgentTaskRequest request,
      @RequestHeader(value = "X-Dedalo-Claim", required = false) String claimId) {
    service.requireCallbackClaim(agentKey, taskId, claimId, request.executionAudit());
    service.completeClaimedProcessTask(agentKey, taskId, request);
    return ResponseEntity.noContent().build();
  }

  /** Persiste a falha sem avançar o processo e preserva cancelamentos anteriores ao retorno. */
  @PostMapping("/{taskId}/failure")
  public ResponseEntity<Void> fail(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @Valid @RequestBody FailAgentTaskRequest request,
      @RequestHeader(value = "X-Dedalo-Claim", required = false) String claimId) {
    service.requireCallbackClaim(agentKey, taskId, claimId, request.executionAudit());
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
