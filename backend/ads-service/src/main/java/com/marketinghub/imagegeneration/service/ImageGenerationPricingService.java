package com.marketinghub.imagegeneration.service;

import com.marketinghub.imagegeneration.ImageGenerationModel;
import com.marketinghub.imagegeneration.ImageGenerationPrice;
import com.marketinghub.imagegeneration.ImageGenerationQuality;
import com.marketinghub.imagegeneration.ImageOrientation;
import com.marketinghub.imagegeneration.repository.ImageGenerationModelRepository;
import com.marketinghub.imagegeneration.repository.ImageGenerationPriceRepository;
import com.marketinghub.imagegeneration.repository.ImageGenerationQualityRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageGenerationPricingService {

    private final ImageGenerationModelRepository modelRepository;
    private final ImageGenerationQualityRepository qualityRepository;
    private final ImageGenerationPriceRepository priceRepository;

    public ImageGenerationPricingService(
            ImageGenerationModelRepository modelRepository,
            ImageGenerationQualityRepository qualityRepository,
            ImageGenerationPriceRepository priceRepository) {
        this.modelRepository = modelRepository;
        this.qualityRepository = qualityRepository;
        this.priceRepository = priceRepository;
    }

    public Optional<ImageGenerationModel> findModel(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return modelRepository.findById(id);
    }

    public Optional<ImageGenerationQuality> findQuality(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return qualityRepository.findById(id);
    }

    public Optional<ImageGenerationQuality> findQuality(Long modelId, Long qualityId) {
        if (modelId == null || qualityId == null) {
            return Optional.empty();
        }
        return qualityRepository.findByModelIdAndId(modelId, qualityId);
    }

    public Optional<ImageGenerationPrice> resolvePrice(Long qualityId, ImageOrientation orientation) {
        if (qualityId == null) {
            return Optional.empty();
        }
        Optional<ImageGenerationPrice> directMatch = Optional.empty();
        if (orientation != null) {
            directMatch = priceRepository.findFirstByQualityIdAndOrientationOrderByPreferredDescWidthDesc(
                    qualityId, orientation);
        }
        if (directMatch.isPresent()) {
            return directMatch;
        }
        return priceRepository.findFirstByQualityIdOrderByPreferredDescWidthDesc(qualityId);
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> estimateTotalCost(
            Long qualityId, ImageOrientation orientation, int imageCount) {
        if (imageCount <= 0) {
            return Optional.empty();
        }
        return resolvePrice(qualityId, orientation).map(price ->
                price.getUnitPriceUsd().multiply(BigDecimal.valueOf(imageCount))
                        .setScale(5, RoundingMode.HALF_UP));
    }
}
