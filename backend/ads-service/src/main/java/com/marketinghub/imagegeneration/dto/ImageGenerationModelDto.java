package com.marketinghub.imagegeneration.dto;

import com.marketinghub.imagegeneration.ImageGenerationProvider;
import java.util.List;

public record ImageGenerationModelDto(
        Long id,
        String code,
        String name,
        ImageGenerationProvider provider,
        String apiModel,
        String description,
        List<ImageGenerationQualityDto> qualities) {}
