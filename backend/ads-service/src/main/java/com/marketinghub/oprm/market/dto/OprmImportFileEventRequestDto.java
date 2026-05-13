package com.marketinghub.oprm.market.dto;

import java.util.List;
import java.time.Instant;

public record OprmImportFileEventRequestDto(String status, Long rowsRead, Long rowsValid, Long rowsRejected, String errorMessage,
                                            Instant finishedAt,
                                            List<OprmCnaeUpsertDto> cnaes) {}
