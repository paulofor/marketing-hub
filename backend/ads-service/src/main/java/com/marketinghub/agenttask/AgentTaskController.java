package com.marketinghub.agenttask;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor as mesas de trabalho dos agentes para a interface administrativa. */
@RestController
@RequestMapping("/api/agent-tasks")
public class AgentTaskController {
  private final AgentTaskService service;
  private final AgentTaskVisualEvidenceService visualEvidenceService;

  /** Configura o serviço canônico da caixa de entrada. */
  public AgentTaskController(
      AgentTaskService service, AgentTaskVisualEvidenceService visualEvidenceService) {
    this.service = service;
    this.visualEvidenceService = visualEvidenceService;
  }

  /** Lista a caixa de entrada de um único agente. */
  @GetMapping("/agents/{agentKey}")
  public List<AgentTaskResponse> inbox(@PathVariable String agentKey) {
    return service.inbox(agentKey);
  }

  /** Lista todo trabalho que ainda exige atuação de algum agente. */
  @GetMapping("/active")
  public List<AgentTaskResponse> activeTasks() {
    return service.activeTasks();
  }

  /** Exibe as instâncias BPM vinculadas a uma entidade operacional. */
  @GetMapping("/process-instances")
  public List<ProcessInstanceResponse> processInstances(@RequestParam String sourceReference) {
    return service.processInstances(sourceReference);
  }

  /** Permite que uma pessoa abra uma solicitação pela tela. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AgentTaskResponse create(@Valid @RequestBody CreateAgentTaskRequest request) {
    return service.createByHuman(request);
  }

  /** Permite evoluir o estado operacional da tarefa. */
  @PatchMapping("/{taskId}/status")
  public AgentTaskResponse updateStatus(
      @PathVariable Long taskId, @Valid @RequestBody UpdateAgentTaskStatusRequest request) {
    return service.updateStatus(taskId, request);
  }

  /** Vincula uma tarefa excepcional pendente a uma atividade regular publicada. */
  @PatchMapping("/{taskId}/process-binding")
  public AgentTaskResponse bindProcess(
      @PathVariable Long taskId, @Valid @RequestBody BindAgentTaskProcessRequest request) {
    return service.bindProcess(taskId, request);
  }

  /** Entrega um snapshot privado em linha sem expor bucket ou chave de storage. */
  @GetMapping("/{taskId}/visual-evidence/{evidenceId}/content")
  public ResponseEntity<byte[]> readVisualEvidence(
      @PathVariable Long taskId, @PathVariable Long evidenceId) {
    var content = visualEvidenceService.read(taskId, evidenceId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(content.contentType()))
        .header("Content-Disposition", "inline")
        .body(content.bytes());
  }
}
