package com.marketinghub.vitrines.app.model;

public record ContentDetailResponse(
    String id,
    String title,
    String description,
    AccessType accessType,
    boolean locked,
    String signedUrl,
    String coverImageUrl,
    String planId) {}
