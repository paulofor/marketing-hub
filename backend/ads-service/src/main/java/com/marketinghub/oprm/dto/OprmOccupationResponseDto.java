package com.marketinghub.oprm.dto;

import java.util.List;

public record OprmOccupationResponseDto(
        String id,
        String occupationSeedRef,
        String displayName,
        List<String> aliases,
        boolean active,
        String createdAt,
        String updatedAt
) {
}
