package com.marketinghub.agenttask;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Responsabilidade: expor a fila canônica de atividades BPM aos executores dos agentes. */
@RestController
@RequestMapping("/api/internal/agent-tasks/{agentKey}/stage-executions")
public class InternalAgentTaskExecutionController {
  private final AgentTaskService service;
  private final AgentTaskVisualEvidenceService visualEvidenceService;

  /** Inicializa o contrato operacional usando a fonte de verdade das tarefas. */
  public InternalAgentTaskExecutionController(
      AgentTaskService service, AgentTaskVisualEvidenceService visualEvidenceService) {
    this.service = service;
    this.visualEvidenceService = visualEvidenceService;
  }

  /** Reserva no máximo uma atividade cuja predecessora já foi concluída. */
  @GetMapping("/pending")
  public List<AgentTaskPendingResponse> pending(
      @PathVariable String agentKey,
      @RequestParam(required = false) String processCode,
      @RequestParam(required = false) String activityId,
      @RequestParam(required = false) String executionResourceCode) {
    return service
        .claimEligibleProcessTask(agentKey, processCode, activityId, executionResourceCode)
        .map(List::of)
        .orElseGet(List::of);
  }

  /** Reexpõe o snapshot da lease ativa ao executor que já reservou a tarefa. */
  @GetMapping("/{taskId}")
  public AgentTaskPendingResponse claimed(
      @PathVariable String agentKey, @PathVariable Long taskId) {
    return service.claimedProcessTask(agentKey, taskId);
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

  /** Bloqueia a atividade com causa persistida sem avançar o processo. */
  @PostMapping("/{taskId}/failure")
  public ResponseEntity<Void> fail(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @Valid @RequestBody FailAgentTaskRequest request) {
    service.failClaimedProcessTask(agentKey, taskId, request);
    return ResponseEntity.noContent().build();
  }
}
