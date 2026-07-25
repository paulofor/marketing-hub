package com.marketinghub.product.service.videoimage;

import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.product.ProductVideoSeedImageReviewStatus;
import java.time.Instant;

/** Responsabilidade: expor uma imagem da galeria de vídeos do produto para o frontend. */
public record ProductVideoImageDto(
    Long id,
    Long productId,
    Long assetId,
    AssetType assetType,
    MediaProvider provider,
    AssetStatus assetStatus,
    String url,
    String model,
    String purpose,
    String prompt,
    ProductVideoSeedImageReviewStatus reviewStatus,
    String reviewNotes,
    Instant createdAt) {}
