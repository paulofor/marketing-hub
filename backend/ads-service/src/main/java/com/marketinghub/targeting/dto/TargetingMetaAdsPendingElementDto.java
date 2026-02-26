package com.marketinghub.targeting.dto;

import com.marketinghub.targeting.TargetingElementType;

public record TargetingMetaAdsPendingElementDto(
        Long id,
        Long marketNicheId,
        TargetingElementType type,
        String term
) {}
