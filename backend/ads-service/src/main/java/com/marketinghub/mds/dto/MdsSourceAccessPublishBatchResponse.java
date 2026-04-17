package com.marketinghub.mds.dto;

import java.util.List;

public record MdsSourceAccessPublishBatchResponse(
        int savedCount,
        List<Long> ids
) {
}
