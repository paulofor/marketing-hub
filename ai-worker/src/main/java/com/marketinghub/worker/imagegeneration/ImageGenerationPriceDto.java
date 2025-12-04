package com.marketinghub.worker.imagegeneration;

import java.math.BigDecimal;

public record ImageGenerationPriceDto(
        Long id,
        ImageOrientation orientation,
        Integer width,
        Integer height,
        String sizeLabel,
        BigDecimal unitPriceUsd,
        boolean preferred) {}
