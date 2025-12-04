package com.marketinghub.imagegeneration.dto;

import java.util.List;

public record ImageGenerationQualityDto(
        Long id,
        Long modelId,
        String code,
        String name,
        String apiQuality,
        boolean defaultQuality,
        List<ImageGenerationPriceDto> prices) {}
