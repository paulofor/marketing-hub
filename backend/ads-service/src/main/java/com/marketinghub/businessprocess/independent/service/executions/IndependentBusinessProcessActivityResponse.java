package com.marketinghub.businessprocess.independent.service.executions;

import java.util.List;

/** Contrato de estado e tentativas de uma atividade dentro da execução independente. */
public record IndependentBusinessProcessActivityResponse(
    String activityId,
    String activityName,
    String status,
    List<IndependentBusinessProcessTaskResponse> tasks) {}
