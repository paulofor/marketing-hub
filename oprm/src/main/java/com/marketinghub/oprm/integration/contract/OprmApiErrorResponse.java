package com.marketinghub.oprm.integration.contract;

public record OprmApiErrorResponse(
        String code,
        String message,
        String correlationId,
        String details
) {
}
