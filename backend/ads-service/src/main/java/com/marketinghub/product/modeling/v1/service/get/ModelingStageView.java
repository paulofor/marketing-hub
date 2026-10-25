package com.marketinghub.product.modeling.v1.service.get;

/** Relaciona a entrega de uma tarefa à atividade e à identidade auditável do percurso. */
public record ModelingStageView(
    Long taskId,
    String agent,
    String activity,
    String status,
    String resultSummary,
    String activityUrl) {}
