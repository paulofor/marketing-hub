package com.marketinghub.salesvideo.dto;

import java.util.List;

/** Responsabilidade: representar o catalogo operacional do Estudio de Audio e Video. */
public record SalesVideoStudioCatalogDto(
    List<SalesVideoStudioCharacterDto> characters,
    List<SalesVideoStudioCaptionPresetDto> captionPresets) {}
