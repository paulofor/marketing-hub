package com.marketinghub.feo.fabricacaov1.contract;

import java.util.List;

/**
 * Representa um arquivo final gerado para entrega ao comprador.
 */
public record DigitalAssetFinal(
        String name,
        String contentType,
        byte[] content,
        String sha256,
        List<String> qualityNotes) {
}
