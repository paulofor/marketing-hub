package com.marketinghub.mds.dto;

import java.util.List;

public record MdsAdminRequestListResponse(
        List<MdsAdminRequestListItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
