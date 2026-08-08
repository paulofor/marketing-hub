package com.marketinghub.hypothesis.dto;

import java.util.List;

/** Página administrativa de hipóteses com totalizadores para navegação. */
public record HypothesisListPageDto(
    List<HypothesisDto> items, long totalElements, int totalPages, int page, int size) {}
