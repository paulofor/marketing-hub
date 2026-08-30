package com.marketinghub.businessprocess.execution.service.humanactivity;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.product.Product;

/** Responsabilidade: aplicar regras e efeitos de domínio de uma aprovação humana especializada. */
public interface HumanProductProcessActivityHandler {

  /** Informa se o handler governa a decisão humana recebida. */
  boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition);

  /** Expõe pré-requisitos e confirmação usando somente a verdade persistida do domínio. */
  HumanProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference);

  /** Aplica o efeito aprovado antes de a instância BPM ser concluída. */
  void approve(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference,
      ProductProcessActivityExecutionRequest request);
}
