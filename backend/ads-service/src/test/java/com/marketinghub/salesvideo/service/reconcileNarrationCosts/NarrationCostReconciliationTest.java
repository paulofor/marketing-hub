package com.marketinghub.salesvideo.service.reconcileNarrationCosts;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobEventRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.tenant.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

/** Reproduz custo desconhecido, recuperação e segregação antes de liberar nova escuta paga. */
class NarrationCostReconciliationTest {
  private final ObjectMapper json = new ObjectMapper();
  private final SalesVideoJobRepository jobs = mock(SalesVideoJobRepository.class);
  private final StudioCostLedgerEntryRepository ledger =
      mock(StudioCostLedgerEntryRepository.class);
  private final SalesVideoJobEventRepository events = mock(SalesVideoJobEventRepository.class);
  private final NarrationCostReconciliation service =
      new NarrationCostReconciliation(
          jobs,
          new com.marketinghub.financialagent.service.StudioCostLedgerService(ledger),
          events,
          json);

  /** Define um operador técnico identificado, sem assumir decisões comerciais. */
  @BeforeEach
  void context() {
    TenantContextHolder.set(new TenantContext("tenant-fixture", "operator@test.local", false));
  }

  /** Remove o escopo ao terminar o teste. */
  @AfterEach
  void clear() {
    TenantContextHolder.clear();
  }

  /** Importa o consumo real em duas identidades e preserva custo, auditoria e idempotência. */
  @ParameterizedTest
  @ValueSource(longs = {9007, 9010})
  void reconcilesAuditedUsageForIndependentProducts(long productId) throws Exception {
    var first = job(91001, productId, 99001);
    var second = job(91002, productId, 99001);
    var input = request(List.of(first.getId(), second.getId()), usage(2), true);
    var result = service.reconcile(99001L, input);
    assertThat(result.estimatedCostUsd()).isEqualByComparingTo("0.002520");
    assertThat(result.providerInvoiceConfirmed()).isFalse();
    assertThat(result.newProviderRequests()).isEqualTo(2);
    var cost = ledger.findBySourceTypeAndSourceId("SALES_VIDEO_JOB", "91001").orElseThrow();
    assertThat(cost.getProviderCostUsd()).isNull();
    assertThat(cost.getEstimatedCostUsd()).isEqualByComparingTo("0.001260");
    verify(events, times(2)).save(any());
    service.reconcile(99001L, input);
    verify(events, times(2)).save(any());
    assertThat(first.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_READY);
  }

  /** Reaproveitar voz conhecida não gera cobrança, inferência ou segunda alocação do consumo. */
  @Test
  void separatesReuseFromOriginalPaidSpeech() throws Exception {
    var original = job(91001, 9007, 99001);
    service.reconcile(99001L, request(List.of(91001L), usage(1), true));
    var reused = job(91002, 9007, 99001);
    ObjectNode metadata = (ObjectNode) json.readTree(reused.getMetadataJson());
    metadata
        .putObject("preservedNarration")
        .put("sourceJobId", original.getId())
        .put("newTtsAuthorized", false);
    var item = (ObjectNode) metadata.path("tts_interactions").get(0);
    item.put("status", "REUSED")
        .put("incremental_cost_usd", 0)
        .put("interaction_type", "TEXT_TO_SPEECH_REUSE")
        .put("source_job_id", original.getId())
        .put("source_asset_id", 123)
        .put("source_sha256", "b".repeat(64));
    reused.setMetadataJson(metadata.toString());
    var result = service.reconcile(99001L, request(List.of(91002L), null, false));
    assertThat(result.estimatedCostUsd()).isZero();
    assertThat(result.newProviderRequests()).isZero();
    assertThat(
            ledger
                .findBySourceTypeAndSourceId("SALES_VIDEO_JOB", "91002")
                .orElseThrow()
                .getCostEvidence())
        .isEqualTo(NarrationCostReconciliation.REUSED);
    item.put("source_job_id", 91003);
    reused.setMetadataJson(metadata.toString());
    assertThatThrownBy(() -> service.reconcile(99001L, request(List.of(91002L), null, false)))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** Falha comprovada no gate anterior à voz é distinta de chamada com custo desconhecido. */
  @Test
  void recognizesProofFailureBeforeSpeech() throws Exception {
    var job = job(91001, 9007, 99001);
    var metadata = (ObjectNode) json.readTree(job.getMetadataJson());
    metadata.remove("tts_interactions");
    metadata.putObject("post_production");
    job.setMetadataJson(metadata.toString());
    job.setStatus(SalesVideoStatus.VIDEO_FAILED);
    job.setFailureCode("PDE_PRODUCT_PROOF_INVALID");
    var result = service.reconcile(99001L, request(List.of(91001L), null, false));
    assertThat(result.estimatedCostUsd()).isZero();
    job.setFailureCode("APOLLO_NARRATION_DURATION_EXCEEDED");
    assertThatThrownBy(() -> service.reconcile(99001L, request(List.of(91001L), null, false)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("recibos");
  }

  /** Recusa dados ambíguos ou cruzados e não grava custos parciais de um lote inválido. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "tenant",
        "product",
        "experiment",
        "active",
        "count",
        "exclusive",
        "missing",
        "hash",
        "rate",
        "model",
        "duplicate",
        "partial",
        "period",
        "receipt"
      })
  void rejectsUnsupportedReconciliation(String scenario) throws Exception {
    var job = job(91001, 9007, 99001);
    var metadata = (ObjectNode) json.readTree(job.getMetadataJson());
    var row = (ObjectNode) metadata.path("tts_interactions").get(0);
    ObjectNode usage = usage(1);
    switch (scenario) {
      case "tenant" -> job.setTenantId("other");
      case "product" -> metadata.put("productId", 9008);
      case "experiment" -> metadata.put("experimentId", 99002);
      case "active" -> job.setStatus(SalesVideoStatus.VIDEO_PROCESSING);
      case "count" -> ((ObjectNode) usage.at("/data/0/results/0")).put("num_model_requests", 2);
      case "missing" -> metadata.remove("tts_interactions");
      case "hash" -> ((ObjectNode) row.path("raw_response")).put("sha256", "wrong");
      case "rate" ->
          ((ObjectNode) row.path("pricing")).put("output_usd_per_million_audio_tokens", 0);
      case "model" -> ((ObjectNode) usage.at("/data/0/results/0")).put("model", "other");
      case "duplicate" ->
          ((com.fasterxml.jackson.databind.node.ArrayNode) usage.at("/data/0/results"))
              .add(usage.at("/data/0/results/0").deepCopy());
      case "partial" -> usage.put("has_more", true);
      case "period" -> ((ObjectNode) usage.at("/data/0")).put("start_time", 10).put("end_time", 20);
      case "receipt" -> ((ObjectNode) row.path("raw_response")).remove("request_id");
    }
    job.setMetadataJson(metadata.toString());
    assertThatThrownBy(
            () ->
                service.reconcile(
                    99001L, request(List.of(91001L), usage, !scenario.equals("exclusive"))))
        .isInstanceOf(ResponseStatusException.class);
    verify(ledger, never()).save(any());
    verifyNoInteractions(events);
  }

  /** Não permite esconder cobrança confirmada nem alterar uma conciliação já aceita. */
  @Test
  void preservesProviderChargesAndExistingReconciliation() throws Exception {
    job(91001, 9007, 99001);
    var entry = ledger.findBySourceTypeAndSourceId("SALES_VIDEO_JOB", "91001").orElseThrow();
    entry.setProviderCostUsd(new BigDecimal("0.01"));
    assertThatThrownBy(() -> service.reconcile(99001L, request(List.of(91001L), usage(1), true)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("cobrança confirmada");
    verify(ledger, never()).save(any());
  }

  /** Exercita o endpoint e o DTO reais até o ledger, sem chamar provedor ou aprovar vídeo. */
  @Test
  void bindsHttpContractToAuditedReconciliation() throws Exception {
    job(91001, 9007, 99001);
    var facade = mock(com.marketinghub.salesvideo.service.SalesVideoService.class);
    when(facade.reconcileNarrationCosts(anyLong(), any()))
        .thenAnswer(call -> service.reconcile(call.getArgument(0), call.getArgument(1)));
    var mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new com.marketinghub.salesvideo.controller.SalesVideoController(
                    facade, mock(com.marketinghub.media.mapper.AssetMapper.class)))
            .build();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/sales-videos/experiments/99001/narration-costs/reconcile")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(request(List.of(91001L), usage(1), true))))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.newProviderRequests")
                .value(1))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.providerInvoiceConfirmed")
                .value(false));
  }

  /** Monta uma tentativa encerrada com tarifa, recibo e lançamento desconhecido auditáveis. */
  private SalesVideoJob job(long id, long productId, long experimentId) {
    var product = new Product();
    product.setId(productId);
    var profile = SalesVideoProfile.builder().id(id + 10).product(product).build();
    var metadata =
        json.createObjectNode().put("productId", productId).put("experimentId", experimentId);
    var interaction =
        metadata
            .putArray("tts_interactions")
            .addObject()
            .put("job_id", id)
            .put("status", "COMPLETED")
            .put("provider", "OPENAI")
            .put("model", "tts-fixture")
            .put("completed_at", "2026-10-10T21:30:00Z");
    interaction
        .put("interaction_type", "TEXT_TO_SPEECH")
        .put("endpoint", "https://api.openai.com/v1/audio/speech");
    interaction
        .putObject("raw_request")
        .put("model", "tts-fixture")
        .put("input", "Fala sintética para teste.");
    interaction
        .putObject("raw_response")
        .put("request_id", "request-" + id)
        .put("bytes", 1000)
        .put("sha256", "a".repeat(64));
    interaction
        .putObject("pricing")
        .put("input_usd_per_million_text_tokens", 0.6)
        .put("output_usd_per_million_audio_tokens", 12);
    var job =
        SalesVideoJob.builder()
            .id(id)
            .profile(profile)
            .tenantId("tenant-fixture")
            .providerName("MUSA_POST_PRODUCTION")
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .status(SalesVideoStatus.VIDEO_READY)
            .metadataJson(metadata.toString())
            .events(new ArrayList<>())
            .build();
    when(jobs.findByIdForUpdate(id)).thenReturn(Optional.of(job));
    var entry = new StudioCostLedgerEntry();
    entry.setSourceType("SALES_VIDEO_JOB");
    entry.setSourceId(Long.toString(id));
    entry.setExperimentId(experimentId);
    entry.setProductId(productId);
    entry.setAssetType("AUDIO");
    entry.setCostEvidence("PROVIDER_COST_NOT_REPORTED");
    when(ledger.findBySourceTypeAndSourceId("SALES_VIDEO_JOB", Long.toString(id)))
        .thenReturn(Optional.of(entry));
    return job;
  }

  /** Cria o extrato de uma janela exclusiva com a contagem exata dos recibos da fixture. */
  private ObjectNode usage(int calls) {
    var usage = json.createObjectNode().put("has_more", false);
    var bucket =
        usage
            .putArray("data")
            .addObject()
            .put("start_time", 1791666000L)
            .put("end_time", 1791669600L);
    bucket
        .putArray("results")
        .addObject()
        .put("object", "organization.usage.completions.result")
        .put("model", "tts-fixture")
        .put("num_model_requests", calls)
        .put("input_text_tokens", calls * 100)
        .put("output_audio_tokens", calls * 100)
        .put("input_audio_tokens", 0)
        .put("output_text_tokens", 0);
    return usage;
  }

  /** Identifica a consulta pública do consumo sem transmitir a credencial administrativa. */
  private ReconcileNarrationCostsRequest request(
      List<Long> ids, ObjectNode usage, boolean exclusive) {
    return new ReconcileNarrationCostsRequest(
        ids,
        usage,
        exclusive,
        "https://api.openai.com/v1/organization/usage/completions?start_time=1791666000");
  }
}
