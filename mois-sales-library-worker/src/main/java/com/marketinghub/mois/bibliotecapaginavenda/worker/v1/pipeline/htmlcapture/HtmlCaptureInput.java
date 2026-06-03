package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.htmlcapture;

/** Entrada da primeira etapa do pipeline: URL normalizada da biblioteca para captura de HTML bruto. */
public record HtmlCaptureInput(
        long pageId,
        String urlCanonical,
        String title
) {
}
