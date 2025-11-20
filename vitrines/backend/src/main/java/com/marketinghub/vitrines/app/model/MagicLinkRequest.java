package com.marketinghub.vitrines.app.model;

public record MagicLinkRequest(String email, String role, String planId, Boolean firstAccess) {}
