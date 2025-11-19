package com.marketinghub.vitrines.app.model;

public record ContentItem(
    String id,
    String title,
    String description,
    AccessType accessType,
    String planId,
    String coverImageUrl,
    String fileStoragePath) {}
