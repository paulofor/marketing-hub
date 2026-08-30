package com.marketinghub.productdiscovery.v1.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Representa uma observação humana feita na Biblioteca pública da Meta para Argos. */
public record ProductDiscoverySupervisedMetaObservationRequest(
    @NotBlank @Size(max = 120) String adReference,
    @NotBlank @Size(max = 255) String advertiserName,
    @NotBlank
        @Pattern(regexp = "https://((www|business)\\.)?facebook\\.com/ads/library/.*")
        @Size(max = 2048)
        String adLibraryUrl,
    @NotBlank @Size(max = 5000) String adText,
    @NotEmpty @Size(max = 2)
        List<
                @Pattern(
                    regexp = "(?i)INSTAGRAM|FACEBOOK",
                    message = "publisherPlatforms aceita somente Instagram ou Facebook")
                String>
            publisherPlatforms,
    @Size(max = 80) String formatType,
    @Size(max = 2048) String mediaUrl,
    @Size(max = 2048) String destinationUrl,
    boolean pageActive,
    boolean commercialSignal,
    @PastOrPresent Instant observedAt) {}
