package com.marketinghub.product.service.adlibrary;

import java.util.List;

/** Responsabilidade: consolidar anúncios reutilizáveis de um produto para a tela comercial. */
public record ProductAdLibraryResponse(
    Long productId,
    String productName,
    String productSlug,
    String commercialStatus,
    String mainRecommendation,
    List<ProductAdLibraryItemResponse> ads) {}
