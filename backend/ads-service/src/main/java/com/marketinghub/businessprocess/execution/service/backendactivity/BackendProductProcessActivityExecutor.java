package com.marketinghub.businessprocess.execution.service.backendactivity;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;

/** Responsabilidade: executar atividades determinísticas pertencentes ao backend dentro do BPM. */
public interface BackendProductProcessActivityExecutor {

  /** Informa se o executor é o responsável pela atividade da versão de processo recebida. */
  boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition);

  /** Verifica pré-condições persistidas sem produzir efeito externo ou alterar o processo. */
  BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference);

  /** Executa a atividade, persiste evidências e devolve o estado funcional alcançado. */
  BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference);
}
