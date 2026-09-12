package com.marketinghub.businessprocess.automation.v1.service.status;

/** Responsabilidade: explicar a intervenção humana necessária e seu destino oficial. */
public record ProcessRunUserAction(
    String code,
    String title,
    String reason,
    String responsible,
    String actionLabel,
    String actionUrl,
    String afterAction,
    String evidenceReference) {}
