package com.marketinghub.feo.fabricacaov1.contract;

/**
 * Define uma imagem editorial que precisa ser gerada para enriquecer o produto final.
 */
public record VisualAssetSpec(
        String code,
        String title,
        String assetType,
        String placement,
        String prompt,
        String size,
        String outputFormat) {
}
