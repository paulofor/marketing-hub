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
  /** Impede consumo da nova montagem por worker antigo e mantém capturas e voz compatíveis. */
  @org.junit.jupiter.api.Test
  void requiresDeliveryCapabilityWithoutBreakingExistingContracts() {
    var job = new com.marketinghub.salesvideo.SalesVideoJob();
    job.setId(91999L);
    job.setMetadataJson(
        "{\"post_production\":{\"deliveryExamples\":{}},\"preservedNarration\":{}}");
    var parser = new com.fasterxml.jackson.databind.ObjectMapper();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                VideoNarrationRecovery.requireCompatible(
                    job, VideoNarrationRecovery.SEQUENCE_CONTRACT, parser))
        .hasMessageContaining("montagem de amostras");
    org.assertj.core.api.Assertions.assertThatCode(
            () ->
                VideoNarrationRecovery.requireCompatible(
                    job, VideoNarrationRecovery.DELIVERY_CONTRACT, parser))
        .doesNotThrowAnyException();
    job.setMetadataJson("{\"post_production\":{\"proofShots\":[]},\"preservedNarration\":{}}");
    org.assertj.core.api.Assertions.assertThatCode(
            () ->
                VideoNarrationRecovery.requireCompatible(
                    job, VideoNarrationRecovery.DELIVERY_CONTRACT, parser))
        .doesNotThrowAnyException();
  }

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

  /** A correção visual de uma finalização pronta conserva todos os áudios sem outra síntese. */
  @ParameterizedTest
  @ValueSource(longs = {21266, 93166})
  void restoresCompletedFinalizationForVisualCorrection(long id) throws Exception {
    var f = fixture(id, mapper);
    var metadata = (ObjectNode) mapper.readTree(f.previous().getMetadataJson());
    metadata.setAll(f.audit());
    f.previous().setStatus(SalesVideoStatus.VIDEO_READY);
    f.previous().setFailureCode(null);
    f.previous().setMetadataJson(metadata.toString());
    f.previous().getEvents().clear();
    var result =
        mapper.valueToTree(
            VideoNarrationRecovery.prepare(
                f.source(), f.previous(), f.request(), mapper, f.assets()));
    assertThat(result.at("/preservedNarration/segments")).hasSize(5);
    assertThat(result.at("/preservedNarration/newTtsAuthorized").asBoolean()).isFalse();
    f.request().setCaptionText("Copy alterada");
    assertThatThrownBy(
            () ->
                VideoNarrationRecovery.prepare(
                    f.source(), f.previous(), f.request(), mapper, f.assets()))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** Reaproveita repetidamente a mesma fala em dois produtos, com sucesso ou falha de montagem. */
  @ParameterizedTest
  @ValueSource(longs = {21267, 94167})
  void restoresRepeatedRecoveryWithoutLosingOriginalCost(long id) throws Exception {
    for (boolean failed : new boolean[] {false, true}) {
      var f = recoveredFixture(id, failed);
      for (int attempt = 0; attempt < 3; attempt++) {
        var result =
            mapper.valueToTree(
                VideoNarrationRecovery.prepare(
                    f.source(), f.previous(), f.request(), mapper, f.assets()));
        assertThat(result.at("/preservedNarration/segments")).hasSize(5);
        assertThat(result.at("/preservedNarration/newTtsAuthorized").asBoolean()).isFalse();
        assertThat(result.at("/preservedNarration/segments/0/assetId").asLong()).isEqualTo(id + 10);
        assertThat(
                result.at("/preservedNarration/segments/0/sourcePricing/known_cost_usd").isNull())
            .isTrue();
        recordReusedResult(f, result.path("preservedNarration"), failed);
      }
    }
  }

  /** Uma recuperação repetida recusa recibos sem vínculo, conteúdo ou custo íntegros. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "source",
        "tenant",
        "copy",
        "new-tts",
        "asset",
        "hash",
        "index",
        "text",
        "duration",
        "cost",
        "missing-audit",
        "model"
      })
  void rejectsUnprovenRepeatedRecovery(String scenario) throws Exception {
    var f = recoveredFixture(94167, false);
    var metadata = (ObjectNode) mapper.readTree(f.previous().getMetadataJson());
    var contract = (ObjectNode) metadata.path("preservedNarration");
    var segment = (ObjectNode) contract.path("segments").get(0);
    var receipt = (ObjectNode) metadata.path("tts_interactions").get(0);
    switch (scenario) {
      case "source" -> receipt.put("source_job_id", 1);
      case "tenant" -> contract.put("tenantId", "other");
      case "copy" -> contract.put("captionText", "Outra copy");
      case "new-tts" -> contract.put("newTtsAuthorized", true);
      case "asset" -> receipt.put("source_asset_id", 1);
      case "hash" -> receipt.put("source_sha256", "f".repeat(64));
      case "index" -> receipt.put("segment_index", 2);
      case "text" -> segment.put("text", "Outro texto");
      case "duration" -> receipt.put("output_duration_seconds", 1);
      case "cost" -> receipt.put("incremental_cost_usd", 1);
      case "missing-audit" -> metadata.remove("tts_interactions");
      case "model" -> receipt.put("model", "other");
    }
    f.previous().setMetadataJson(metadata.toString());
    assertThatThrownBy(
            () ->
                VideoNarrationRecovery.prepare(
                    f.source(), f.previous(), f.request(), mapper, f.assets()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409");
  }

  /** Cria o contrato e o recibo de uma fala já recuperada, como o executor publicado. */
  private Fixture recoveredFixture(long id, boolean failed) throws Exception {
    var f = fixture(id, mapper);
    var first =
        mapper.valueToTree(
            VideoNarrationRecovery.prepare(
                f.source(), f.previous(), f.request(), mapper, f.assets()));
    recordReusedResult(f, first.path("preservedNarration"), failed);
    return f;
  }

  /**
   * Simula o recibo REUSED sem novo upload nem request externo, inclusive após falha da montagem.
   */
  private void recordReusedResult(
      Fixture f, com.fasterxml.jackson.databind.JsonNode contract, boolean failed)
      throws Exception {
    var metadata = mapper.createObjectNode();
    metadata.put("captionText", f.request().getCaptionText());
    metadata.put("voiceOverScript", f.request().getVoiceOverScript());
    metadata.set("preservedNarration", contract.deepCopy());
    f.previous().setId(f.previous().getId() + 1);
    var audit = mapper.createObjectNode();
    var interactions = audit.putArray("tts_interactions");
    for (var segment : contract.path("segments")) {
      var interaction = interactions.addObject();
      interaction
          .put("interaction_type", "TEXT_TO_SPEECH_REUSE")
          .put("provider", "OPENAI")
          .put("status", "REUSED")
          .put("job_id", f.previous().getId())
          .put("source_job_id", contract.path("sourceJobId").asLong())
          .put("source_asset_id", segment.path("assetId").asLong())
          .put("source_sha256", segment.path("sha256").asText())
          .put("segment_index", segment.path("segmentIndex").asInt())
          .put("model", segment.path("model").asText())
          .put("output_duration_seconds", segment.path("durationSeconds").asDouble())
          .put("incremental_cost_usd", 0);
      interaction.set("source_pricing", segment.path("sourcePricing").deepCopy());
    }
    f.previous().getEvents().clear();
    if (failed) {
      f.previous().setStatus(SalesVideoStatus.VIDEO_FAILED);
      f.previous().setFailureCode("PDE_PROOF_CAPTION_TIMING_INVALID");
      f.previous()
          .getEvents()
          .add(
              SalesVideoJobEvent.builder()
                  .id(f.previous().getId() + 1)
                  .job(f.previous())
                  .detailsJson(audit.toString())
                  .build());
    } else {
      f.previous().setStatus(SalesVideoStatus.VIDEO_READY);
      f.previous().setFailureCode(null);
      metadata.setAll(audit);
    }
    f.previous().setMetadataJson(metadata.toString());
  }

  /** Nova copy exige confirmação; o texto idêntico continua recuperando a voz sem nova síntese. */
  @Test
  void separatesConfirmedNarrationRevisionFromRecovery() throws Exception {
    var f = fixture(91009, mapper);
    f.request().setCaptionText("Novo texto.");
    f.request().setVoiceOverScript("Novo texto.");
    f.request().setNarrationRevisionConfirmed(true);
    var result =
        mapper.valueToTree(
            VideoNarrationRecovery.prepare(
                f.source(), f.previous(), f.request(), mapper, f.assets()));
    assertThat(result.at("/narrationRevision/newNarrationRequested").asBoolean()).isTrue();
    assertThat(result.has("preservedNarration")).isFalse();
    verifyNoInteractions(f.assets());
  }

  /** Falha de montagem após receber a fala reutiliza o recibo íntegro, sem pagar novamente. */
  @ParameterizedTest
  @ValueSource(strings = {"PDE_PRODUCT_PROOF_INVALID", "PDE_PROOF_CAPTION_TIMING_INVALID"})
  void recoversAuditedVoiceAfterProofCompositionFailure(String code) throws Exception {
    var f = fixture(91009, mapper);
    f.previous().setFailureCode(code);
    var result =
        mapper.valueToTree(
            VideoNarrationRecovery.prepare(
                f.source(), f.previous(), f.request(), mapper, f.assets()));
    assertThat(result.at("/preservedNarration/segments")).hasSize(5);
    assertThat(result.at("/preservedNarration/newTtsAuthorized").asBoolean()).isFalse();
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

  /** Um worker antigo não pode ignorar a sequência e entregar outra montagem como sucesso. */
  @Test
  void requiresSequenceCapabilityBeforeClaiming() {
    var job =
        SalesVideoJob.builder()
            .id(91009L)
            .metadataJson("{\"post_production\":{\"proofShots\":[{}]}}")
            .build();
    assertThatThrownBy(
            () ->
                VideoNarrationRecovery.requireCompatible(
                    job, VideoNarrationRecovery.CONTRACT, mapper))
        .hasMessageContaining("sequência");
    assertThatCode(
            () ->
                VideoNarrationRecovery.requireCompatible(
                    job, VideoNarrationRecovery.SEQUENCE_CONTRACT, mapper))
        .doesNotThrowAnyException();
    job.setMetadataJson("{\"preservedNarration\":{}}");
    assertThatCode(
            () ->
                VideoNarrationRecovery.requireCompatible(
                    job, VideoNarrationRecovery.SEQUENCE_CONTRACT, mapper))
        .doesNotThrowAnyException();
  }

  /** Preserva produção normal e outra classe de falha sem criar contrato de voz. */
  @Test
  void preservesPreviousValidPaths() throws Exception {
    var f = fixture(91009, mapper);
    assertThat(VideoNarrationRecovery.prepare(f.source(), null, f.request(), mapper, f.assets()))
        .isEmpty();
    f.previous().setFailureCode("NETWORK_FAILURE");
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
