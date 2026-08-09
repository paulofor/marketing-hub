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

/** Responsabilidade: expor o catálogo de modelos visuais homologados para novas gerações. */
@Service
public class ImageGenerationCatalogService {

  private static final String OBSOLETE_OPENAI_IMAGE_PREFIX = "gpt-image-1";

  private final ImageGenerationModelRepository modelRepository;

  /** Inicializa o catálogo com o repositório canônico de modelos. */
  public ImageGenerationCatalogService(ImageGenerationModelRepository modelRepository) {
    this.modelRepository = modelRepository;
  }

  /** Lista somente modelos disponíveis para novas gerações, com qualidades e preços. */
  public List<ImageGenerationModelDto> listModels() {
    return modelRepository.findAll(Sort.by(Sort.Direction.ASC, "displayName")).stream()
        .filter(model -> !isObsolete(model))
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  /**
   * Oculta modelos OpenAI aposentados, preservando seus registros apenas para auditoria histórica.
   */
  private boolean isObsolete(ImageGenerationModel model) {
    return model.getApiModel() != null
        && model
            .getApiModel()
            .toLowerCase(java.util.Locale.ROOT)
            .startsWith(OBSOLETE_OPENAI_IMAGE_PREFIX);
  }

  /** Converte um modelo persistido para o contrato público do catálogo. */
  private ImageGenerationModelDto toDto(ImageGenerationModel model) {
    List<ImageGenerationQualityDto> qualities =
        model.getQualities().stream()
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

  /** Converte uma qualidade e seus preços para o contrato público. */
  private ImageGenerationQualityDto toDto(ImageGenerationQuality quality) {
    List<ImageGenerationPriceDto> prices =
        quality.getPrices().stream()
            .sorted(
                Comparator.comparing(ImageGenerationPrice::getOrientation)
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

  /** Converte um preço persistido para o contrato público. */
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
