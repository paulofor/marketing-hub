package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Transporta uma observação pública e limitada da Biblioteca Meta executada pelo Argos. */
public record ProductDiscoveryMetaAdBrowserCollectionRequest(
    @NotBlank @Size(max = 36) String executionLeaseId,
    @NotNull @Min(1) @Max(3) Integer attemptNumber,
    @NotNull Long investigationId,
    @NotBlank @Size(max = 80) String collectorRunId,
    @NotBlank
        @Pattern(regexp = "https://((www|business)\\.)?facebook\\.com/ads/library/.*")
        @Size(max = 2048)
        String searchUrl,
    @NotBlank @Pattern(regexp = "OBSERVED|EMPTY|FALLBACK_REQUIRED") String outcome,
    @Min(100) @Max(599) Integer httpStatus,
    boolean platformFilterConfirmed,
    @Size(max = 255) String pageTitle,
    @Size(max = 1000) String errorMessage,
    @NotNull Instant startedAt,
    @NotNull Instant finishedAt,
    @NotNull @Size(max = 25) List<@Valid Observation> observations) {

  /** Representa um card público visível e o payload bruto coletado no navegador efêmero. */
  public record Observation(
      @NotBlank @Size(max = 120) String metaAdId,
      @NotBlank @Size(max = 255) String advertiserName,
      boolean active,
      @NotNull @Size(min = 1, max = 1)
          List<
                  @Pattern(
                      regexp = "INSTAGRAM",
                      message = "o navegador de Argos aceita somente Instagram")
                  String>
              publisherPlatforms,
      @NotNull @Size(max = 3) List<@Size(max = 80) String> formatTypes,
      @NotNull @Size(min = 1, max = 10) List<@NotBlank @Size(max = 5000) String> texts,
      @Size(max = 2048)
          @Pattern(regexp = "|https?://.*", message = "destinationUrl deve ser HTTP(S)")
          String destinationUrl,
      @NotBlank
          @Pattern(regexp = "https://((www|business)\\.)?facebook\\.com/ads/library/.*")
          @Size(max = 2048)
          String snapshotUrl,
      boolean pageActive,
      boolean commercialSignal,
      @NotNull JsonNode rawPayload) {}
}
