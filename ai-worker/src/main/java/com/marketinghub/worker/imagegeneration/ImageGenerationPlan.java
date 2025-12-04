package com.marketinghub.worker.imagegeneration;

import java.math.BigDecimal;

public record ImageGenerationPlan(
        Long modelId,
        Long qualityId,
        String apiModel,
        String apiQuality,
        ImageOrientation orientation,
        Integer width,
        Integer height,
        String sizeLabel,
        BigDecimal unitPriceUsd) {}
