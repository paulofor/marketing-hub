package com.marketinghub.oprm.market.dto;

import java.time.Instant;
import java.util.List;

public record OprmImportFileEventRequestDto(
    String status,
    Long rowsRead,
    Long rowsValid,
    Long rowsRejected,
    String errorMessage,
    Instant finishedAt,
    List<OprmCnaeUpsertDto> cnaes,
    List<OprmMarketSizeUpsertDto> marketSizes) {}
