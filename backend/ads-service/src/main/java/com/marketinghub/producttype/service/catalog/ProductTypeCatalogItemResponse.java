package com.marketinghub.producttype.service.catalog;

import com.marketinghub.producttype.ProductTypeStatus;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: expor um tipo de produto e seu uso atual no catálogo administrativo. */
public record ProductTypeCatalogItemResponse(
    Long id,
    String code,
    String name,
    String internalName,
    String description,
    List<String> aliases,
    ProductTypeStatus status,
    long productCount,
    Instant createdAt,
    Instant updatedAt) {}
