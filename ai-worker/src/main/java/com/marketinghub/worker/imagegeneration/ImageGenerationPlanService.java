package com.marketinghub.worker.imagegeneration;

import com.marketinghub.worker.leadportal.image.LeadPortalImagePackageClient;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class ImageGenerationPlanService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationPlanService.class);

    private final ImageGenerationCatalogService catalogService;

    public ImageGenerationPlanService(ImageGenerationCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public ImageGenerationPlan resolvePlan(LeadPortalImagePackageClient.ImagePackage imagePackage, ImageOrientation orientation) {
        Long requestedModelId = imagePackage.imageModelId();
        Long requestedQualityId = imagePackage.imageModelQualityId();

        ImageGenerationModelDto model = null;
        ImageGenerationQualityDto quality = null;

        if (requestedQualityId != null) {
            quality = catalogService.findQuality(requestedQualityId).orElse(null);
            if (quality != null) {
                model = catalogService.findModel(quality.modelId()).orElse(null);
            }
        }
        if (model == null && requestedModelId != null) {
            model = catalogService.findModel(requestedModelId).orElse(null);
        }
        if (quality == null && model != null) {
            quality = selectQualityForModel(model, requestedQualityId);
        }
        if (model == null && quality != null) {
            model = catalogService.findModel(quality.modelId()).orElse(null);
        }
        if (model == null) {
            model = catalogService.getCatalog().stream().findFirst().orElse(null);
        }
        if (quality == null && model != null) {
            quality = selectQualityForModel(model, null);
        }
        if (model == null || quality == null) {
            return null;
        }

        ImageGenerationPriceDto price = selectPrice(quality, orientation);
        ImageOrientation resolvedOrientation = price != null ? price.orientation() : orientation;
        Integer width = price != null ? price.width() : null;
        Integer height = price != null ? price.height() : null;
        String sizeLabel = price != null ? price.sizeLabel() : null;
        BigDecimal unitPrice = price != null ? price.unitPriceUsd() : null;

        return new ImageGenerationPlan(
                model.id(),
                quality.id(),
                model.apiModel(),
                quality.apiQuality(),
                resolvedOrientation,
                width,
                height,
                sizeLabel,
                unitPrice);
    }

    private ImageGenerationQualityDto selectQualityForModel(ImageGenerationModelDto model, Long preferredQualityId) {
        if (CollectionUtils.isEmpty(model.qualities())) {
            return null;
        }
        if (preferredQualityId != null) {
            Optional<ImageGenerationQualityDto> direct = model.qualities().stream()
                    .filter(q -> preferredQualityId.equals(q.id()))
                    .findFirst();
            if (direct.isPresent()) {
                return direct.get();
            }
        }
        return model.qualities().stream()
                .filter(ImageGenerationQualityDto::defaultQuality)
                .findFirst()
                .orElse(model.qualities().get(0));
    }

    private ImageGenerationPriceDto selectPrice(ImageGenerationQualityDto quality, ImageOrientation orientation) {
        if (quality.prices() == null || quality.prices().isEmpty()) {
            return null;
        }
        if (orientation != null) {
            Optional<ImageGenerationPriceDto> match = quality.prices().stream()
                    .filter(price -> price.orientation() == orientation)
                    .findFirst();
            if (match.isPresent()) {
                return match.get();
            }
        }
        return quality.prices().stream()
                .sorted(Comparator
                        .comparing(ImageGenerationPriceDto::preferred).reversed()
                        .thenComparing(ImageGenerationPriceDto::width, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ImageGenerationPriceDto::height, Comparator.nullsLast(Integer::compareTo)))
                .findFirst()
                .orElse(null);
    }

    public ImageOrientation detectOrientation(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return ImageOrientation.SQUARE;
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                return ImageOrientation.SQUARE;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            if (width == height) {
                return ImageOrientation.SQUARE;
            }
            return width > height ? ImageOrientation.LANDSCAPE : ImageOrientation.PORTRAIT;
        } catch (IOException ex) {
            log.warn("Failed to detect orientation from base image", ex);
            return ImageOrientation.SQUARE;
        }
    }
}
