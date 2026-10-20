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
   * Lista peças e fontes aprovadas, incluindo provas datadas do destino para a revisão de Têmis.
   */
  @GetMapping
  @Operation(
      summary = "Lista entradas visuais da tarefa criativa já reservada",
      description =
          "Têmis recebe creativeProof com fonte, recorte, autorização e relatório de continuidade datado. O endpoint de conteúdo permite somente esses arquivos da mesma tarefa e versão.")
  public List<CreativeVisualEvidenceService.VisualInput> inputs(
      @PathVariable String agentKey, @PathVariable Long taskId) {
    return service.inputs(agentKey, taskId);
  }

  /** Lista somente ilustrações auditadas da mesma identidade, sem aprovar seu uso. */
  @GetMapping("/illustrations")
  @Operation(summary = "Lista apoio ilustrativo auditado, separado da prova funcional")
  public List<CreativeIllustrationSources.Illustration> illustrations(
      @PathVariable String agentKey, @PathVariable Long taskId) {
    return service.illustrations(agentKey, taskId);
  }

  /** Lê os bytes auditados com autorização renovada na tarefa reservada. */
  @GetMapping("/illustrations/{jobId}/content")
  @Operation(summary = "Lê ilustração autorizada no contexto criativo reservado")
  public ResponseEntity<byte[]> illustration(
      @PathVariable String agentKey, @PathVariable Long taskId, @PathVariable String jobId) {
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(service.illustrationContent(agentKey, taskId, jobId));
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
