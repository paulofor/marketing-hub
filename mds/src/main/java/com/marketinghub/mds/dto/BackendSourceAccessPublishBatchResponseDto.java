package com.marketinghub.mds.dto;

import java.util.List;

public record BackendSourceAccessPublishBatchResponseDto(
        int savedCount,
        List<Long> ids
) {
}
