package com.marketinghub.businessprocess.execution.service.humanactivity;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.product.Product;

/** Responsabilidade: executar e auditar atividades cuja autoridade é um operador humano. */
public interface HumanProductProcessActivityExecutor {

  /** Informa se o executor reconhece a atividade como uma decisão humana. */
  boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition);

  /** Expõe a prontidão, os requisitos e a confirmação da decisão humana. */
  HumanProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference);

  /** Aplica a decisão confirmada e persiste a ocorrência BPM correspondente. */
  HumanProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference,
      ProductProcessActivityExecutionRequest request);
}
