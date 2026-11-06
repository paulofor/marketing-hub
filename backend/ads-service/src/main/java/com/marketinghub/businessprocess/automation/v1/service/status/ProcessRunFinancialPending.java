package com.marketinghub.businessprocess.automation.v1.service.status;

/** Responsabilidade: apresentar a pendência financeira comprovada junto da atividade afetada. */
public record ProcessRunFinancialPending(
    String code,
    String activityId,
    String productVersion,
    String title,
    String reason,
    String responsible,
    String actionLabel,
    String actionUrl,
    String afterAction) {}
