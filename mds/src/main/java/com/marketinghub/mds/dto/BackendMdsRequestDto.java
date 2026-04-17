package com.marketinghub.mds.dto;

public record BackendMdsRequestDto(
        Long id,
        String status,
        String market,
        String problem,
        String desiredOutcome,
        String correlationId
) {
}
