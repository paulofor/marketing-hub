package com.marketinghub.oprm.market.dto;

import java.time.Instant;

public record OprmCompleteImportRunRequestDto(String status,
                                              Instant finishedAt,
                                              Integer filesProcessed,
                                              Long rowsRead,
                                              Long rowsValid,
                                              Long rowsRejected,
                                              String errorMessage) {}
