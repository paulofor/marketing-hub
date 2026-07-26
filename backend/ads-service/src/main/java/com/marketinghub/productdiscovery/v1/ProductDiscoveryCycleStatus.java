package com.marketinghub.productdiscovery.v1;

/** Define o status operacional de um ciclo de descoberta de produtos PDE. */
public enum ProductDiscoveryCycleStatus {
    DRAFT,
    READY_FOR_RESEARCH,
    RESEARCHING,
    COMPLETED,
    FAILED,
    ARCHIVED
}
