package com.marketinghub.planning.imagestudio.v1.service;

import java.util.List;

/** Responsabilidade: identificar um exemplo premium aprovado entregue ao Estúdio de Têmis. */
public record TemisVisualPlaybookExampleDto(
    Long assetId, String label, String assetUrl, String format, List<String> purposes) {}
