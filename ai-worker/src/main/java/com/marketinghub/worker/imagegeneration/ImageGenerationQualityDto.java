package com.marketinghub.worker.imagegeneration;

import java.util.List;

public record ImageGenerationQualityDto(
        Long id,
        Long modelId,
        String code,
        String name,
        String apiQuality,
        boolean defaultQuality,
        List<ImageGenerationPriceDto> prices) {}
