package com.marketinghub.feo.fabricacaov1.contract;

/**
 * Descreve uma linha do manifesto do pacote final.
 */
public record ManifestItem(String fileName, String contentType, String role, String consumptionOrder, String sha256) {
}
