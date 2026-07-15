package com.marketinghub.feo.fabricacaov1.contract;

import java.util.List;

/**
 * Representa uma imagem final gerada para capa, infográfico ou figura interna do e-book.
 */
public record VisualAsset(
        String code,
        String title,
        String assetType,
        String fileName,
        String contentType,
        byte[] content,
        String prompt,
        String model,
        String providerRequest,
        String providerResponse,
        List<String> qualityNotes) {
}
