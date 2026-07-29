package com.marketinghub.socialdistribution.dto;

import com.marketinghub.socialdistribution.*;
import java.time.Instant;

/** Responsabilidade: agrupar contratos REST do módulo de distribuição orgânica. */
public final class SocialDistributionDtos {
  private SocialDistributionDtos() {}

  /** Dados para cadastrar ou ajustar uma conta social. */
  public record SaveSocialAccountRequest(
      SocialPlatform platform,
      String displayName,
      String handle,
      String externalAccountId,
      SocialConnectionMode connectionMode,
      SocialAccountStatus status,
      String setupNotes) {}

  /** Resposta resumida de uma conta social. */
  public record SocialAccountResponse(
      Long id,
      SocialPlatform platform,
      String displayName,
      String handle,
      String externalAccountId,
      SocialConnectionMode connectionMode,
      SocialAccountStatus status,
      String requiredScopes,
      String setupNotes,
      Instant connectedAt) {}

  /** Dados para criar uma publicação orgânica. */
  public record CreateSocialVideoPublicationRequest(
      Long productId,
      Long assetId,
      Long socialAccountId,
      SocialPlatform platform,
      SocialVideoFormat videoFormat,
      String title,
      String caption,
      String hashtags,
      String videoUrl,
      Instant scheduledAt) {}

  /** Dados para confirmar publicação feita pela plataforma ou pelo operador. */
  public record MarkSocialVideoPublishedRequest(
      String publishedUrl, String externalPostId, Instant publishedAt) {}

  /** Dados para registrar métrica posterior da publicação. */
  public record RecordSocialPublicationMetricRequest(
      Long views,
      Long likes,
      Long comments,
      Long shares,
      Long clicks,
      String rawPayloadJson,
      Instant capturedAt) {}

  /** Resposta consolidada de publicação e última métrica conhecida. */
  public record SocialVideoPublicationResponse(
      Long id,
      Long productId,
      String productName,
      String productSlug,
      Long assetId,
      Long socialAccountId,
      String socialAccountName,
      SocialPlatform platform,
      SocialVideoFormat videoFormat,
      SocialVideoPublicationStatus status,
      String title,
      String caption,
      String hashtags,
      String videoUrl,
      String publishedUrl,
      String externalPostId,
      String failureReason,
      String publishPayloadJson,
      Instant scheduledAt,
      Instant queuedAt,
      Instant publishedAt,
      SocialPublicationMetricResponse latestMetric) {}

  /** Resposta de métrica posterior da publicação. */
  public record SocialPublicationMetricResponse(
      Long id,
      Long views,
      Long likes,
      Long comments,
      Long shares,
      Long clicks,
      String rawPayloadJson,
      Instant capturedAt) {}
}
