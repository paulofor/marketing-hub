package com.marketinghub.mds.search;

import java.util.List;

public record SourceSearchHit(
        String source,
        String sourceDocumentId,
        String doi,
        String title,
        String abstractText,
        String url,
        String publicationYear,
        String licenseText,
        boolean openAccess,
        boolean canDownload,
        boolean canTextMine,
        List<String> authors
) {
}
