package com.marketinghub.payments.dto;

import java.time.Instant;

/** Expõe o andamento funcional da produção sem dados técnicos internos. */
public record AgendaCheiaDeliveryResponse(
        String status,
        String stage,
        Integer qualityScore,
        String downloadUrl,
        Instant finishedAt,
        Instant deliveredAt) {}
