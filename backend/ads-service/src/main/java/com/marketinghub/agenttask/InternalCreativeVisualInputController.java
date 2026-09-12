package com.marketinghub.agenttask;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: entregar ao executor reservado somente as imagens governadas da atividade
 * criativa.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/agent-tasks/{agentKey}/stage-executions/{taskId}/visual-inputs")
public class InternalCreativeVisualInputController {
  private final CreativeVisualEvidenceService service;

  /**
   * Lista imagens aprovadas e sua versão para produção ou revisão, sem reservar trabalho adicional.
   */
  @GetMapping
  @Operation(summary = "Lista entradas visuais da tarefa criativa já reservada")
  public List<CreativeVisualEvidenceService.VisualInput> inputs(
      @PathVariable String agentKey, @PathVariable Long taskId) {
    return service.inputs(agentKey, taskId);
  }

  /** Entrega os pixels privados após conferir novamente tarefa, versão e origem aprovada. */
  @GetMapping("/{sourceTaskId}/{evidenceId}/content")
  @Operation(summary = "Lê PNG autorizado pela origem da tarefa criativa")
  public ResponseEntity<byte[]> content(
      @PathVariable String agentKey,
      @PathVariable Long taskId,
      @PathVariable Long sourceTaskId,
      @PathVariable Long evidenceId) {
    var content = service.read(agentKey, taskId, sourceTaskId, evidenceId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.contentType()))
        .body(content.bytes());
  }
}
