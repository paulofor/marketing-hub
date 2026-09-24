package com.marketinghub.productdiscovery.v1.service;

/** Identifica a sessão Meta imutável que Argos deve incorporar sem repetir a pesquisa ampla. */
public record ProductDiscoverySupervisedMetaReanalysisContext(
    Long investigationId, String query, String country, String publisherPlatform) {}
