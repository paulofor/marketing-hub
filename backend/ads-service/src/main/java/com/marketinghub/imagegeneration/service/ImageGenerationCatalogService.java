package com.marketinghub.imagegeneration.service;

import com.marketinghub.imagegeneration.ImageGenerationModel;
import com.marketinghub.imagegeneration.ImageGenerationPrice;
import com.marketinghub.imagegeneration.ImageGenerationQuality;
import com.marketinghub.imagegeneration.dto.ImageGenerationModelDto;
import com.marketinghub.imagegeneration.dto.ImageGenerationPriceDto;
import com.marketinghub.imagegeneration.dto.ImageGenerationQualityDto;
import com.marketinghub.repository.jpa.imagegeneration.ImageGenerationModelRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ImageGenerationCatalogService {

    private final ImageGenerationModelRepository modelRepository;

    public ImageGenerationCatalogService(ImageGenerationModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    public List<ImageGenerationModelDto> listModels() {
        return modelRepository.findAll(Sort.by(Sort.Direction.ASC, "displayName")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ImageGenerationModelDto toDto(ImageGenerationModel model) {
        List<ImageGenerationQualityDto> qualities = model.getQualities().stream()
                .sorted(Comparator.comparingInt(ImageGenerationQuality::getPosition))
                .map(this::toDto)
                .toList();
        return new ImageGenerationModelDto(
                model.getId(),
                model.getCode(),
                model.getDisplayName(),
                model.getProvider(),
                model.getApiModel(),
                model.getDescription(),
                qualities);
    }

    private ImageGenerationQualityDto toDto(ImageGenerationQuality quality) {
        List<ImageGenerationPriceDto> prices = quality.getPrices().stream()
                .sorted(Comparator
                        .comparing(ImageGenerationPrice::getOrientation)
                        .thenComparing(ImageGenerationPrice::getWidth)
                        .thenComparing(ImageGenerationPrice::getHeight))
                .map(this::toDto)
                .toList();
        return new ImageGenerationQualityDto(
                quality.getId(),
                quality.getModel() != null ? quality.getModel().getId() : null,
                quality.getCode(),
                quality.getDisplayName(),
                quality.getApiQuality(),
                quality.isDefaultQuality(),
                prices);
    }

    private ImageGenerationPriceDto toDto(ImageGenerationPrice price) {
        return new ImageGenerationPriceDto(
                price.getId(),
                price.getOrientation(),
                price.getWidth(),
                price.getHeight(),
                price.getSizeLabel(),
                price.getUnitPriceUsd(),
                price.isPreferred());
    }
}
