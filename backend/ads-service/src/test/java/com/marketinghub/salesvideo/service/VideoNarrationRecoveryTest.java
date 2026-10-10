package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.RequestSalesVideoPostProductionRequest;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

/** Protege recuperação da mesma fala sem novo consumo ou transferência entre contextos. */
class VideoNarrationRecoveryTest {
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Reutiliza cinco trechos íntegros em contextos distintos sem declarar custo antigo como zero.
   */
  @ParameterizedTest
  @ValueSource(longs = {21252, 91009})
  void restoresOnlyVerifiedAudio(long id) throws Exception {
    var fixture = fixture(id, mapper);
    var result =
        mapper.valueToTree(
            VideoNarrationRecovery.prepare(
                fixture.source(), fixture.previous(), fixture.request(), mapper, fixture.assets()));
    assertThat(result.at("/preservedNarration/sourceJobId").asLong()).isEqualTo(id + 1);
    assertThat(result.at("/preservedNarration/segments")).hasSize(5);
    assertThat(result.at("/preservedNarration/measuredDurationSeconds").asDouble())
        .isCloseTo(15.72, within(.001));
    assertThat(result.at("/preservedNarration/newTtsAuthorized").asBoolean()).isFalse();
    assertThat(
            result
                .at("/preservedNarration/segments/0/sourcePricing/reconciliation_status")
                .asText())
        .isEqualTo("PENDING_PROVIDER_RECONCILIATION");
    assertThat(result.at("/preservedNarration/segments/0/sourcePricing/known_cost_usd").isNull())
        .isTrue();
  }

  /** Recusa falhas de identidade, texto, recibo e tempo antes de enfileirar outro job. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "tenant",
        "source",
        "caption",
        "voice",
        "duration",
        "missing-duration",
        "receipt",
        "asset",
        "text",
        "hash"
      })
  void blocksUnsafeRecovery(String scenario) throws Exception {
    var f = fixture(91009, mapper);
    switch (scenario) {
      case "tenant" -> f.previous().setTenantId("other");
      case "source" -> f.previous().setRetryOfJob(SalesVideoJob.builder().id(1L).build());
      case "caption" -> f.request().setCaptionText("Outra copy");
      case "voice" -> f.request().setVoiceOverScript("Outra voz");
      case "duration" -> f.request().setTargetDurationSeconds(15);
      case "missing-duration" -> f.request().setTargetDurationSeconds(null);
      case "receipt" -> f.audit().remove("persisted_audit_assets");
      case "asset" -> when(f.assets().findById(anyLong())).thenReturn(Optional.empty());
      case "text" ->
          ((ObjectNode) f.audit().at("/tts_interactions/0/raw_request"))
              .put("input", "Outro texto");
      case "hash" ->
          ((ObjectNode) f.audit().at("/tts_interactions/0/raw_response")).put("sha256", "changed");
    }
    f.previous().getEvents().getFirst().setDetailsJson(mapper.writeValueAsString(f.audit()));
    assertThatThrownBy(
            () ->
                VideoNarrationRecovery.prepare(
                    f.source(), f.previous(), f.request(), mapper, f.assets()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409");
  }

  /** Capacidade exata aceita a recuperação; jobs normais preservam compatibilidade. */
  @Test
  void acceptsCapableWorkerAndPreviousNormalJobs() {
    var job =
        SalesVideoJob.builder().id(91009L).metadataJson("{\"preservedNarration\":{}}").build();
    VideoNarrationRecovery.requireCompatible(job, VideoNarrationRecovery.CONTRACT, mapper);
    job.setMetadataJson("");
    VideoNarrationRecovery.requireCompatible(job, null, mapper);
  }

  /** Preserva produção normal e outra classe de falha sem criar contrato de voz. */
  @Test
  void preservesPreviousValidPaths() throws Exception {
    var f = fixture(91009, mapper);
    assertThat(VideoNarrationRecovery.prepare(f.source(), null, f.request(), mapper, f.assets()))
        .isEmpty();
    f.previous().setFailureCode("PDE_PRODUCT_PROOF_INVALID");
    assertThat(
            VideoNarrationRecovery.prepare(
                f.source(), f.previous(), f.request(), mapper, f.assets()))
        .isEmpty();
  }

  /** Mantém fonte, tentativa e auditoria juntas no teste da integração do service. */
  record Fixture(
      SalesVideoJob source,
      SalesVideoJob previous,
      RequestSalesVideoPostProductionRequest request,
      AssetRepository assets,
      ObjectNode audit) {}

  /** Cria auditoria segregada com a mesma soma temporal observada no caso original. */
  static Fixture fixture(long id, ObjectMapper mapper) throws Exception {
    var source = VideoFinalDeliveryContractTest.source();
    source.setId(id);
    source.setJobType(SalesVideoJobType.RENDER);
    source.setProviderName("EDITORIAL_MOTION");
    var request = new RequestSalesVideoPostProductionRequest();
    request.setCaptionText(
        "Desejo reconhecido | Resultado com limites | Sem diagnóstico | Sem cobrança | Experimentar acesso privado");
    request.setVoiceOverScript(request.getCaptionText().replace(" | ", ". "));
    request.setRequestedBy("fixture@sandbox.local");
    request.setTargetDurationSeconds(20);
    var previous =
        SalesVideoJob.builder()
            .id(id + 1)
            .tenantId("default")
            .retryOfJob(source)
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .status(SalesVideoStatus.VIDEO_FAILED)
            .failureCode("APOLLO_NARRATION_DURATION_EXCEEDED")
            .metadataJson(
                mapper.writeValueAsString(
                    Map.of(
                        "captionText",
                        request.getCaptionText(),
                        "voiceOverScript",
                        request.getVoiceOverScript())))
            .build();
    var audit = mapper.createObjectNode();
    var interactions = audit.putArray("tts_interactions");
    var receipts = audit.putArray("persisted_audit_assets");
    AssetRepository assets = mock(AssetRepository.class);
    String[] texts = request.getCaptionText().split(" \\| ");
    double[] durations = {3.72, 2.808, 2.76, 3.168, 3.264};
    for (int i = 0; i < texts.length; i++) {
      String file = "received-segment-" + (i + 1) + ".mp3";
      String hash = Integer.toHexString(i + 1).repeat(64);
      var interaction = interactions.addObject();
      interaction
          .put("segment_index", i + 1)
          .put("status", "COMPLETED")
          .put("model", "tts-model")
          .put("output_duration_seconds", durations[i]);
      interaction.putObject("raw_request").put("input", texts[i]).put("voice", "marin");
      interaction.putObject("raw_response").put("sha256", hash).put("asset_file_name", file);
      interaction
          .putObject("pricing")
          .put("reconciliation_status", "PENDING_PROVIDER_RECONCILIATION")
          .putNull("known_cost_usd");
      receipts
          .addObject()
          .put("asset_id", id + 10 + i)
          .put("file_name", file)
          .put("sha256", hash)
          .put("role", "AUDIO_AUDIT");
      when(assets.findById(id + 10 + i))
          .thenReturn(
              Optional.of(
                  Asset.builder()
                      .id(id + 10 + i)
                      .type(AssetType.AUDIO)
                      .status(AssetStatus.READY)
                      .url("https://fixture.invalid/" + file)
                      .build()));
    }
    previous
        .getEvents()
        .add(
            SalesVideoJobEvent.builder()
                .id(id + 2)
                .job(previous)
                .detailsJson(mapper.writeValueAsString(audit))
                .build());
    return new Fixture(source, previous, request, assets, audit);
  }
}
