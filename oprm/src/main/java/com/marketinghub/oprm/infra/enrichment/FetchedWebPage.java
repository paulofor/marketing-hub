package com.marketinghub.oprm.infra.enrichment;

public record FetchedWebPage(
        String url,
        int statusCode,
        String title,
        String content,
        String language,
        String captureNotes,
        boolean success) {
}
