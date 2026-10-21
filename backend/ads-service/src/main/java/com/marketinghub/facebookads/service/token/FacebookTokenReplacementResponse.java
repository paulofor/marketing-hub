package com.marketinghub.facebookads.service.token;

import java.time.LocalDateTime;

/** Confirma a substituição sem devolver a credencial nem configurações comerciais. */
public record FacebookTokenReplacementResponse(
    Long accountId,
    LocalDateTime tokenExpiresAt,
    LocalDateTime validatedAt,
    boolean tokenRenewalEnabled) {}
