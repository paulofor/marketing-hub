package com.marketinghub.oprm.market.dto;

import java.util.List;

public record OprmImportRunCreatedResponseDto(Long runId, List<Long> fileIds) {}
