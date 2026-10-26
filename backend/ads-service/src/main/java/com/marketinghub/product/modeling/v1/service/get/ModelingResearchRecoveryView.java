package com.marketinghub.product.modeling.v1.service.get;

/** Expõe a elegibilidade comprovada para retomar a pesquisa após reconexão de Argos. */
public record ModelingResearchRecoveryView(boolean available, String reason, Long reconnectionId) {}
