package com.marketinghub.mois.biblioteca.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

public final class MoisSalesLibraryDtos {

    private MoisSalesLibraryDtos() {
    }

    public record SalesLibraryIngestRequest(
            @NotBlank String workspaceId,
            @NotBlank String source,
            @NotEmpty List<@Valid SalesLibraryUrlItem> urls
    ) {
    }

    public record SalesLibraryUrlItem(
            @NotBlank String url,
            String title,
            Instant capturedAt
    ) {
    }

    public record SalesLibraryIngestResponse(
            String workspaceId,
            String source,
            int received,
            int persisted
    ) {
    }


    public record SalesLibraryEntryResponse(
            long id,
            String workspaceId,
            String source,
            String urlOriginal,
            String urlCanonical,
            String title,
            int ingestCount,
            Instant firstCapturedAt,
            Instant lastCapturedAt,
            Instant updatedAt
    ) {
    }

    public record SalesLibraryEntryPageResponse(
            int page,
            int pageSize,
            long total,
            List<SalesLibraryEntryResponse> items
    ) {
    }

}
