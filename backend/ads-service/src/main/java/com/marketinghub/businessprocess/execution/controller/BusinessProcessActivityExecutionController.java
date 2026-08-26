package com.marketinghub.businessprocess.execution.controller;

import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor o histórico recente de execuções de uma atividade BPM. */
@Tag(
    name = "Processos — execuções das atividades",
    description = "Consulta auditável das dez tarefas mais recentes de cada atividade.")
@RestController
@RequestMapping("/api/business-processes/{processDefinitionId}/activities/{activityId}/executions")
public class BusinessProcessActivityExecutionController {
  private final BusinessProcessActivityExecutionService service;

  /** Configura o serviço canônico de leitura das execuções BPM. */
  public BusinessProcessActivityExecutionController(
      BusinessProcessActivityExecutionService service) {
    this.service = service;
  }

  /** Retorna as dez execuções mais recentes da atividade estável no processo canônico. */
  @Operation(summary = "Lista as dez execuções mais recentes da atividade")
  @GetMapping
  public BusinessProcessActivityExecutionHistoryResponse recentExecutions(
      @PathVariable Long processDefinitionId, @PathVariable String activityId) {
    return service.recentExecutions(processDefinitionId, activityId);
  }
}
