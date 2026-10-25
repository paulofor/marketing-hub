package com.marketinghub.product.modeling.v1.service.get;

/** Identifica preparação elegível sem confundir sua escolha com autorização de gasto. */
public record ModelingCycleOption(
    Long cycleId, Long experimentId, String productVersion, String url) {}
