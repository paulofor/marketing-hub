package com.marketinghub.experiment.dto;

import java.util.List;

/** Página administrativa de experimentos com totalizadores para navegação. */
public record ExperimentListPageDto(
    List<ExperimentDto> items, long totalElements, int totalPages, int page, int size) {}
