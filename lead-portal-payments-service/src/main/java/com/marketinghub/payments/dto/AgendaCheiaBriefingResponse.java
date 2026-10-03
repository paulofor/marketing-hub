package com.marketinghub.payments.dto;

import java.time.Instant;
import com.marketinghub.payments.service.kit.BriefingKitProfile;

/** Retorna a confirmação funcional do briefing recebido. */
public record AgendaCheiaBriefingResponse(Long id, String paymentId, String status, Instant submittedAt,
                                        BriefingKitProfile profile) {}
