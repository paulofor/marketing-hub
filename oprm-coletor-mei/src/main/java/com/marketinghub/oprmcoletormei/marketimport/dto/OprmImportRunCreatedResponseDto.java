package com.marketinghub.oprmcoletormei.marketimport.dto;

import java.util.List;

public record OprmImportRunCreatedResponseDto(Long runId, List<Long> fileIds) {
}
