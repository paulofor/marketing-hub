package com.marketinghub.experiment.service.construction;

import java.util.List;

/** Seção de construção com itens de negócio do experimento. */
public record ExperimentConstructionSectionDto(String title, String description, List<ExperimentConstructionItemDto> items) {}
