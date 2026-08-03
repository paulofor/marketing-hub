package com.marketinghub.socialdistribution.dto;

import com.marketinghub.socialdistribution.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
      Long growthContentId,
      Long assetId,
      Long socialAccountId,
      SocialPlatform platform,
      SocialVideoFormat videoFormat,
      String title,
      String caption,
      String hashtags,
      String videoUrl,
      Instant scheduledAt) {
    /** Mantém compatibilidade com clientes anteriores ao vínculo de pauta. */
    public CreateSocialVideoPublicationRequest(
        Long productId,
        Long assetId,
        Long socialAccountId,
        SocialPlatform platform,
        SocialVideoFormat videoFormat,
        String title,
        String caption,
        String hashtags,
        String videoUrl,
        Instant scheduledAt) {
      this(
          productId,
          null,
          assetId,
          socialAccountId,
          platform,
          videoFormat,
          title,
          caption,
          hashtags,
          videoUrl,
          scheduledAt);
    }
  }

  /** Dados para confirmar publicação feita pela plataforma ou pelo operador. */
  public record MarkSocialVideoPublishedRequest(
      String publishedUrl, String externalPostId, Instant publishedAt) {}

  /** Dados para registrar falha retornada pelo executor de publicação. */
  public record MarkSocialVideoFailedRequest(String errorCategory, String errorMessage) {}

  /** Dados para registrar métrica posterior da publicação. */
  public record RecordSocialPublicationMetricRequest(
      Long views,
      Long engagedViews,
      BigDecimal averageViewDurationSeconds,
      Long recurringViewers,
      Long subscribersGained,
      Long likes,
      Long comments,
      Long shares,
      Long clicks,
      Long landingSessions,
      Long leads,
      Long checkoutsStarted,
      Long salesApproved,
      BigDecimal revenue,
      String rawPayloadJson,
      Instant capturedAt) {
    /** Mantém compatibilidade com a leitura básica de métricas já usada pelo worker. */
    public RecordSocialPublicationMetricRequest(
        Long views,
        Long likes,
        Long comments,
        Long shares,
        Long clicks,
        String rawPayloadJson,
        Instant capturedAt) {
      this(
          views,
          null,
          null,
          null,
          null,
          likes,
          comments,
          shares,
          clicks,
          null,
          null,
          null,
          null,
          null,
          rawPayloadJson,
          capturedAt);
    }
  }

  /** Resposta consolidada de publicação e última métrica conhecida. */
  public record SocialVideoPublicationResponse(
      Long id,
      Long productId,
      String productName,
      String productSlug,
      Long assetId,
      Long growthContentId,
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
      SocialPublicationMetricResponse latestMetric,
      String socialAccountExternalAccountId) {}

  /** Resposta de métrica posterior da publicação. */
  public record SocialPublicationMetricResponse(
      Long id,
      Long views,
      Long engagedViews,
      BigDecimal averageViewDurationSeconds,
      Long recurringViewers,
      Long subscribersGained,
      Long likes,
      Long comments,
      Long shares,
      Long clicks,
      Long landingSessions,
      Long leads,
      Long checkoutsStarted,
      Long salesApproved,
      BigDecimal revenue,
      String rawPayloadJson,
      Instant capturedAt) {}

  /** Dados para iniciar um ciclo mensurável de crescimento orgânico. */
  public record CreateSocialGrowthPlanRequest(
      Long productId,
      String name,
      String audience,
      String commercialHypothesis,
      String commercialObjective,
      String primaryCta,
      String destinationUrl,
      String utmCampaign,
      LocalDate startsOn,
      LocalDate endsOn) {}

  /** Dados para adicionar uma pauta ao calendário do plano. */
  public record CreateSocialGrowthContentRequest(
      SocialGrowthContentType contentType,
      String pillar,
      String topic,
      String funnelStage,
      String cta,
      Instant plannedAt) {}

  /** Resposta de uma pauta com rastreamento e aprovação explícitos. */
  public record SocialGrowthContentResponse(
      Long id,
      SocialGrowthContentType contentType,
      String pillar,
      String topic,
      String funnelStage,
      String cta,
      String trackingCode,
      String trackingUrl,
      SocialGrowthContentStatus status,
      Instant plannedAt,
      Long publicationId) {}

  /** Totais comerciais e orientação derivada pelo backend para o plano. */
  public record SocialGrowthPlanPerformanceResponse(
      Long views,
      Long engagedViews,
      Long recurringViewers,
      Long landingSessions,
      Long leads,
      Long checkoutsStarted,
      Long salesApproved,
      BigDecimal revenue,
      String decision,
      String decisionReason) {}

  /** Resposta consolidada do plano, calendário e leitura comercial. */
  public record SocialGrowthPlanResponse(
      Long id,
      Long productId,
      String productName,
      String name,
      String audience,
      String commercialHypothesis,
      String commercialObjective,
      String primaryCta,
      String destinationUrl,
      String utmCampaign,
      SocialGrowthPlanStatus status,
      LocalDate startsOn,
      LocalDate endsOn,
      List<SocialGrowthContentResponse> contents,
      SocialGrowthPlanPerformanceResponse performance) {}
}
