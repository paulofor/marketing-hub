package com.marketinghub.ads.dto;

public record FacebookInstantFormPublicationDto(
        Long id,
        String facebookFormId,
        String name,
        String status,
        Long facebookPageId,
        String facebookPageExternalId,
        String facebookPageName,
        boolean approved,
        boolean published,
        String shareLink
) {
}
