package com.marketinghub.oprm.dto;

public record OprmApiErrorResponseDto(
        String code,
        String message,
        String correlationId,
        String details
) {
}
