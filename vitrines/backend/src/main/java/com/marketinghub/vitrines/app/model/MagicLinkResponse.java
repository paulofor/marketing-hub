package com.marketinghub.vitrines.app.model;

import java.time.Instant;

public record MagicLinkResponse(
    String email,
    String role,
    String planId,
    boolean firstAccess,
    String token,
    String link,
    Instant expiresAt) {}
