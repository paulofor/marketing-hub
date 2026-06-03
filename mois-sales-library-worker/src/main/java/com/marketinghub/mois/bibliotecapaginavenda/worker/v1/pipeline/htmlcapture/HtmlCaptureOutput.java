package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.htmlcapture;

import java.time.Instant;

/** Saída estruturada da captura de HTML bruto versionado. */
public record HtmlCaptureOutput(
        String rawHtml,
        String finalUrl,
        Integer httpStatus,
        String contentType,
        String sha256,
        long sizeBytes,
        Instant capturedAt
) {
}
