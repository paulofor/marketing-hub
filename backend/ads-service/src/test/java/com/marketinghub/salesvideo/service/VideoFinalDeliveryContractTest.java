package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.media.Asset;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.RequestSalesVideoPostProductionRequest;
import org.junit.jupiter.api.Test;

/** Protege recuperação sem IA, identidade da mídia e distinção do papel no funil. */
class VideoFinalDeliveryContractTest {
  private final ObjectMapper mapper = new ObjectMapper();

  /** Congela MP4/VTT e mantém avaliações sem promover aprovação comercial. */
  @Test
  void preservesFinalBytesAndAudit() throws Exception {
    var source = source();
    var request = request();
    var result = mapper.readTree(VideoFinalDeliveryContract.prepare(source, request, mapper));
    assertThat(result.path("deliveryOnly").asBoolean()).isTrue();
    assertThat(result.at("/delivery_source/videoSha256").asText()).isEqualTo("a".repeat(64));
    assertThat(result.at("/delivery_source/captionSha256").asText()).isEqualTo("b".repeat(64));
    assertThat(result.at("/audio/review/status").asText()).isEqualTo("APPROVED_FOR_TEST");
    assertThat(source.getMetadataJson()).doesNotContain("deliveryOnly");
  }

  /** Impede usar recuperação barata para trocar copy, fonte ou renderizar um bruto. */
  @Test
  void rejectsContentChangesAndMissingHash() {
    var request = request();
    request.setCaptionText("Outro produto");
    assertThatThrownBy(() -> VideoFinalDeliveryContract.prepare(source(), request, mapper))
        .hasMessageContaining("409");
    var source = source();
    source.getAsset().setPayload("{}");
    assertThatThrownBy(() -> VideoFinalDeliveryContract.prepare(source, request(), mapper))
        .hasMessageContaining("409");
    source.setProviderName("RUNWAY_ROUTER");
    assertThatThrownBy(() -> VideoFinalDeliveryContract.prepare(source, request(), mapper))
        .hasMessageContaining("409");
  }

  /** Separa anúncio e demonstração mesmo quando a etapa de funil é compartilhada. */
  @Test
  void resolvesCanonicalChannelsWithoutTitleOrFallback() {
    var project =
        VideoProject.builder()
            .targetChannel("SOCIAL_REELS_STORIES")
            .funnelStage("AWARENESS_TO_DIAGNOSTIC")
            .build();
    assertThat(VideoProjectFunnelRole.resolve(project)).isEqualTo(VideoProjectFunnelRole.AD);
    project.setTargetChannel("PDE_HERO_DIAGNOSTIC");
    assertThat(VideoProjectFunnelRole.resolve(project))
        .isEqualTo(VideoProjectFunnelRole.LANDING_HERO);
    project.setTargetChannel("PAYWALL_OFFER");
    assertThat(VideoProjectFunnelRole.resolve(project))
        .isEqualTo(VideoProjectFunnelRole.PRE_CHECKOUT);
    project.setTargetChannel("DESCONHECIDO");
    assertThatThrownBy(() -> VideoProjectFunnelRole.resolve(project))
        .hasMessageContaining("canal único");
  }

  /** Monta fonte sintética com identidade de upload e custo preservável. */
  static SalesVideoJob source() {
    return SalesVideoJob.builder()
        .id(91009L)
        .tenantId("default")
        .status(SalesVideoStatus.VIDEO_READY)
        .providerName("MUSA_POST_PRODUCTION")
        .jobType(SalesVideoJobType.POST_PRODUCTION)
        .profile(SalesVideoProfile.builder().id(91001L).tenantId("default").build())
        .retryAttempt(2)
        .executionMode(SalesVideoExecutionMode.TEST)
        .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
        .asset(
            Asset.builder()
                .id(92001L)
                .url("https://cdn.test/final.mp4")
                .payload("{\"metadata\":{\"sha256\":\"" + "a".repeat(64) + "\"}}")
                .build())
        .vttAsset(
            Asset.builder()
                .id(92002L)
                .url("https://cdn.test/final.vtt")
                .payload("{\"metadata\":{\"sha256\":\"" + "b".repeat(64) + "\"}}")
                .build())
        .metadataJson(
            "{\"captionText\":\"Copy preservada\",\"has_audio\":true,\"captions\":{\"burned_in\":true},\"audio\":{\"review\":{\"status\":\"APPROVED_FOR_TEST\"}}}")
        .build();
  }

  /** Solicita somente empacotamento, sem roteiro ou fonte alternativos. */
  static RequestSalesVideoPostProductionRequest request() {
    var request = new RequestSalesVideoPostProductionRequest();
    request.setRequestedBy("fixture@sandbox.local");
    request.setDeliveryOnly(true);
    request.setCaptionText("Copy preservada");
    return request;
  }
}
