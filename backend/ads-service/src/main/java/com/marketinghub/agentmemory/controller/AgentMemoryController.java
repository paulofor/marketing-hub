package com.marketinghub.agentmemory.controller;

import com.marketinghub.agentmemory.service.AgentMemoryService;
import com.marketinghub.agentmemory.service.registerFeedback.RegisterMemoryFeedbackRequest;
import com.marketinghub.agentmemory.service.registerMemory.RegisterMemoryRequest;
import com.marketinghub.agentmemory.service.retrieveMemory.MemoryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor a memória governada aos MCPs exclusivos dos agentes premium. */
@RestController
@RequestMapping("/api/internal/agent-memory/v1/agents/{agentKey}")
public class AgentMemoryController {
  private final AgentMemoryService service;

  /** Inicializa o controller com a governança central de memória. */
  public AgentMemoryController(AgentMemoryService service) {
    this.service = service;
  }

  /** Recupera memórias relevantes sem expor histórico ilimitado. */
  @GetMapping
  public List<MemoryResponse> retrieve(
      @PathVariable String agentKey,
      @RequestParam(required = false) String tenantKey,
      @RequestParam String scopeType,
      @RequestParam String scopeId,
      @RequestParam(defaultValue = "8") int limit) {
    return service.retrieve(agentKey, tenantKey, scopeType, scopeId, limit);
  }

  /** Registra uma lembrança candidata que aguarda confirmação independente. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MemoryResponse register(
      @PathVariable String agentKey, @Valid @RequestBody RegisterMemoryRequest request) {
    return service.register(agentKey, request);
  }

  /** Recebe resultado oficial posterior sem permitir que o MCP se auto-confirme. */
  @PostMapping("/{memoryId}/feedback")
  public MemoryResponse feedback(
      @PathVariable String agentKey,
      @PathVariable Long memoryId,
      @Valid @RequestBody RegisterMemoryFeedbackRequest request) {
    return service.feedback(agentKey, memoryId, request);
  }
}
