package com.marketinghub.businessprocess.independent.service.executions;

import java.util.List;

/** Contrato detalhado com o resumo e a trilha de atividades da execução independente. */
public record IndependentBusinessProcessExecutionResponse(
    IndependentBusinessProcessExecutionSummaryResponse execution,
    List<IndependentBusinessProcessActivityResponse> activities) {}
