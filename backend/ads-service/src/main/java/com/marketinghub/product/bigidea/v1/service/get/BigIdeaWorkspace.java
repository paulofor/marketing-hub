package com.marketinghub.product.bigidea.v1.service.get;

import java.util.List;

/** Expõe situação, referência vigente, histórico e orientação da Big Idea. */
public record BigIdeaWorkspace(
    Long productId,
    String status,
    BigIdeaView current,
    List<BigIdeaView> history,
    String guidance) {}
