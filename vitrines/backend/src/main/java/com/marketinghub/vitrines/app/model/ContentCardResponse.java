package com.marketinghub.vitrines.app.model;

public record ContentCardResponse(
    String id,
    String title,
    String description,
    AccessType accessType,
    boolean locked,
    String coverImageUrl,
    String planId) {}
