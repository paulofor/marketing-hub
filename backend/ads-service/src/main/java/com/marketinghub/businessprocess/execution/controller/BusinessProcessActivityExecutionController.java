package com.marketinghub.businessprocess.execution.controller;

import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responsabilidade: expor a situação e as tarefas auditáveis das atividades BPM por processo e
 * produto.
 */
@Tag(
    name = "Processos — execuções das atividades",
    description =
        "Consulta auditável das tarefas por atividade e das atividades executadas por produto.")
@RestController
@RequestMapping("/api/business-processes")
public class BusinessProcessActivityExecutionController {
  private final BusinessProcessActivityExecutionService service;

  /** Configura o serviço canônico de leitura das execuções BPM. */
  public BusinessProcessActivityExecutionController(
      BusinessProcessActivityExecutionService service) {
    this.service = service;
  }

  /** Retorna as dez execuções mais recentes da atividade estável no processo canônico. */
  @Operation(summary = "Lista as dez execuções mais recentes da atividade")
  @GetMapping("/{processDefinitionId}/activities/{activityId}/executions")
  public BusinessProcessActivityExecutionHistoryResponse recentExecutions(
      @PathVariable Long processDefinitionId, @PathVariable String activityId) {
    return service.recentExecutions(processDefinitionId, activityId);
  }

  /** Retorna a situação, todas as atividades e as tarefas do produto no processo selecionado. */
  @Operation(summary = "Mostra a situação, as atividades e as tarefas do produto no processo")
  @GetMapping("/{processDefinitionId}/products/{productId}/activity-executions")
  public ProductProcessActivityExecutionHistoryResponse productProcessExecutions(
      @PathVariable Long processDefinitionId, @PathVariable Long productId) {
    return service.productProcessExecutions(processDefinitionId, productId);
  }
}
