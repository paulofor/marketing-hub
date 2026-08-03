package com.marketinghub.payments.dto;

import java.time.Instant;

/** Retorna a confirmação funcional do briefing recebido. */
public record AgendaCheiaBriefingResponse(Long id, String paymentId, String status, Instant submittedAt) {}
