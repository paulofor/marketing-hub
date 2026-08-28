package com.marketinghub.planning.imagestudio.v1.service;

import java.util.List;

/** Responsabilidade: identificar um exemplo aprovado entregue ao Estúdio de Íris. */
public record TemisVisualPlaybookExampleDto(
    Long assetId, String label, String assetUrl, String format, List<String> purposes) {}
