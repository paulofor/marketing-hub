package com.marketinghub.geralanding.agent.v1;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor os contratos internos exclusivos do Agente Gerador de Landing. */
@RestController
@RequestMapping("/api/internal/geralanding/agent/v1/stage-executions")
public class LandingGenerationAgentController {
  private final LandingGenerationAgentExecutionService service;

  /** Inicializa o controller com o serviço canônico da fila. */
  public LandingGenerationAgentController(LandingGenerationAgentExecutionService service) {
    this.service = service;
  }

  /** Reserva pendências para o executor independente. */
  @GetMapping("/pending")
  public List<LandingAgentPendingResponse> pending(
      @RequestParam(defaultValue = "1") int limit,
      @RequestHeader(value = "X-Agent-Build-Reference", required = false) String buildReference) {
    return service.claimPending(limit, buildReference);
  }

  /** Expõe somente o snapshot da execução segregada ao MCP. */
  @GetMapping("/{executionId}/context")
  public LandingAgentPendingResponse context(@PathVariable String executionId) {
    return service.context(executionId);
  }

  /** Recebe o resultado auditável sem conceder autoridade de publicação ao worker. */
  @PostMapping("/{executionId}/result")
  public ResponseEntity<Void> result(
      @PathVariable String executionId, @Valid @RequestBody LandingAgentResultRequest request) {
    service.complete(executionId, request);
    return ResponseEntity.noContent().build();
  }
}
