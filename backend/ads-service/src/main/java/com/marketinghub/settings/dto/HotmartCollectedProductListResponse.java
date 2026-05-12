package com.marketinghub.settings.dto;

import java.util.List;

public record HotmartCollectedProductListResponse(
        String workspaceId,
        List<HotmartCollectedProductDto> items
) {
}
