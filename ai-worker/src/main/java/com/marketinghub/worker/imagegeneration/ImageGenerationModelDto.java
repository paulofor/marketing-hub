package com.marketinghub.worker.imagegeneration;

import java.util.List;

public record ImageGenerationModelDto(
        Long id,
        String code,
        String name,
        String provider,
        String apiModel,
        String description,
        List<ImageGenerationQualityDto> qualities) {}
