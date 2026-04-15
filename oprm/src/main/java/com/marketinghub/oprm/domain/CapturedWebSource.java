package com.marketinghub.oprm.domain;

import java.time.Instant;
import java.util.List;

public record CapturedWebSource(
        String url,
        String sourceType,
        String title,
        Instant capturedAt,
        String language,
        String contentHash,
        List<String> extractedBlocks,
        String captureNotes,
        String captureStatus) {
}
