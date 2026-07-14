package com.marketinghub.feo.fabricacaov1.contract;

import java.util.List;

/**
 * Lista os arquivos finais e a ordem recomendada de consumo pelo cliente.
 */
public record OfferDeliveryManifest(String requestId, String packageTitle, List<ManifestItem> items) {
}
