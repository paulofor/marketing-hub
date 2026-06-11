package com.marketinghub.niche.dto;

import java.util.List;

/** Responsabilidade: transportar uma página da listagem administrativa de nichos. */
public record MarketNicheListPageDto(
        List<MarketNicheListItemDto> items,
        long totalElements,
        int totalPages,
        int page,
        int size) {
}
