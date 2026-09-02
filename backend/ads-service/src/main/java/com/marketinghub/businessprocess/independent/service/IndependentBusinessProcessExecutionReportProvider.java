package com.marketinghub.businessprocess.independent.service;

import com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessFlowReportResponse;
import java.util.Map;

/** Responsabilidade: produzir o relatório funcional especializado de uma execução independente. */
public interface IndependentBusinessProcessExecutionReportProvider {
  /** Informa o processo publicado atendido pelo provedor. */
  String processCode();

  /** Consolida a verdade persistida da cadeia originada pela referência informada. */
  IndependentBusinessProcessFlowReportResponse report(String sourceReference);

  /** Resolve em lote somente os estados funcionais da listagem, sem montar relatórios extensos. */
  default Map<String, String> summaryStatuses(Map<String, String> technicalStatuses) {
    return Map.copyOf(technicalStatuses);
  }
}
