package com.marketinghub.businessprocess.independent.controller;

import com.marketinghub.businessprocess.independent.service.IndependentBusinessProcessExecutionService;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessCatalogResponse;
import com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessExecutionResponse;
import com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessExecutionSummaryResponse;
import com.marketinghub.businessprocess.independent.service.startExecution.StartIndependentBusinessProcessExecutionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o cockpit de processos executáveis sem produto. */
@RestController
@RequestMapping("/api/independent-business-process-executions")
@Tag(name = "Execuções independentes de processos")
public class IndependentBusinessProcessExecutionController {
  private final IndependentBusinessProcessExecutionService service;

  /** Configura o controller com o serviço que mantém a orquestração no backend. */
  public IndependentBusinessProcessExecutionController(
      IndependentBusinessProcessExecutionService service) {
    this.service = service;
  }

  /** Lista processos publicados, seus campos e sua disponibilidade operacional. */
  @GetMapping("/catalog")
  @Operation(summary = "Lista processos que podem ser iniciados sem produto")
  public List<IndependentBusinessProcessCatalogResponse> catalog() {
    return service.catalog();
  }

  /** Lista uma página leve do histórico sem transformar execução em venda ou receita. */
  @GetMapping
  @Operation(summary = "Lista execuções independentes recentes por cursor")
  public List<IndependentBusinessProcessExecutionSummaryResponse> list(
      @RequestParam(name = "limit", defaultValue = "10") int limit,
      @RequestParam(name = "beforeId", required = false) Long beforeId) {
    return service.list(limit, beforeId);
  }

  /** Exibe a trilha auditável de atividades e tentativas de uma execução. */
  @GetMapping("/{executionId}")
  @Operation(summary = "Detalha uma execução independente")
  public IndependentBusinessProcessExecutionResponse get(@PathVariable Long executionId) {
    return service.get(executionId);
  }

  /** Cria idempotentemente a entidade canônica e a primeira tarefa do processo. */
  @PostMapping
  @Operation(summary = "Inicia um processo sem vínculo com produto")
  public ResponseEntity<IndependentBusinessProcessExecutionResponse> start(
      @Valid @RequestBody StartIndependentBusinessProcessExecutionRequest request) {
    IndependentBusinessProcessExecutionResponse response = service.start(request);
    return ResponseEntity.created(
            URI.create("/api/independent-business-process-executions/" + response.execution().id()))
        .body(response);
  }
}
