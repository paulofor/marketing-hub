package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.controller.LearningCycleController;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.singleVideoBudget.*;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.tenant.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Verifica registro integral sem herança histórica, duplicação, gasto ou contaminação entre
 * produtos.
 */
class LearningCycleSingleVideoBudgetTest {
  final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  final LearningSalesCycleEventRepository events = mock(LearningSalesCycleEventRepository.class);
  final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  final VideoProjectRepository projects = mock(VideoProjectRepository.class);
  final ProductRepository products = mock(ProductRepository.class);
  final ExperimentRepository experiments = mock(ExperimentRepository.class);
  final LearningCycleJson json = new LearningCycleJson(mapper);
  final LearningCycleSingleVideoBudget budget =
      new LearningCycleSingleVideoBudget(events, projects, cycles, json);
  final List<LearningSalesCycleEvent> history = new ArrayList<>();
  final LearningSalesCycle cycle = new LearningSalesCycle();
  final VideoProject project = new VideoProject();
  final Instant now = Instant.parse("2026-11-05T14:00:00Z");
  MockMvc http;

  /** Exercita controller e service reais com persistência simulada e identidades sintéticas. */
  @BeforeEach
  void setup() {
    TenantContextHolder.set(new TenantContext("test-a", "operator@test.local", false));
    cycle.setId(21L);
    cycle.setProductId(4L);
    cycle.setChainDefinitionId(27L);
    cycle.setExperimentId(114L);
    cycle.setProductVersion("video-v18");
    cycle.setStage("PLANNING");
    cycle.setStatus("OPEN");
    cycle.setRevision(8L);
    cycle.setVersionChangedAt(now.minusSeconds(60));
    project.setId(14L);
    project.setProductId(4L);
    project.setExperimentId(114L);
    project.setSalesVideoProfileId(69L);
    project.setCampaignKey("video-v18");
    project.setTenantId("test-a");
    when(projects.findById(anyLong()))
        .thenAnswer(
            c ->
                Objects.equals(c.getArgument(0), project.getId())
                    ? Optional.of(project)
                    : Optional.empty());
    when(events.findByCycleIdAndActionOrderByRevisionDesc(anyLong(), eq("AUTHORIZE_SINGLE_VIDEO")))
        .thenAnswer(
            c ->
                history.stream()
                    .filter(e -> Objects.equals(e.getCycleId(), c.getArgument(0)))
                    .toList()
                    .reversed());
    when(events.findByCycleIdAndRequestKey(anyLong(), anyString()))
        .thenAnswer(
            c ->
                history.stream()
                    .filter(
                        e ->
                            Objects.equals(e.getCycleId(), c.getArgument(0))
                                && e.getRequestKey().equals(c.getArgument(1)))
                    .findFirst());
    when(events.saveAndFlush(any()))
        .thenAnswer(
            c -> {
              LearningSalesCycleEvent event = c.getArgument(0);
              event.setId((long) history.size() + 1);
              history.add(event);
              return event;
            });
    when(cycles.findLocked(4L, 21L)).thenReturn(Optional.of(cycle));
    when(cycles.findById(21L)).thenReturn(Optional.of(cycle));
    when(cycles.findByExperimentId(114L)).thenReturn(Optional.of(cycle));
    var product = Product.builder().id(4L).name("Produto teste").build();
    when(products.findLockedById(4L)).thenReturn(Optional.of(product));
    when(products.findById(4L)).thenReturn(Optional.of(product));
    var experiment = new Experiment();
    experiment.setId(114L);
    experiment.setProduct(product);
    when(experiments.findById(114L)).thenReturn(Optional.of(experiment));
    var service =
        new LearningCycleService(
            cycles,
            events,
            products,
            experiments,
            null,
            null,
            null,
            json,
            null,
            null,
            null,
            null,
            null);
    ReflectionTestUtils.setField(service, "singleVideoBudget", budget);
    http =
        MockMvcBuilders.standaloneSetup(new LearningCycleController(service))
            .addFilters(new TenantContextFilter())
            .build();
  }

  /** Limpa o contexto para impedir que um teste empreste tenant ao seguinte. */
  @AfterEach
  void clearTenant() {
    TenantContextHolder.clear();
  }

  /** Cria uma decisão de teste com o único valor financeiro informado pelo usuário. */
  SingleVideoBudgetRequest request(UUID key, boolean confirmed) {
    return new SingleVideoBudgetRequest(
        key,
        cycle.getRevision(),
        cycle.getChainDefinitionId(),
        cycle.getExperimentId(),
        cycle.getProductVersion(),
        project.getId(),
        project.getSalesVideoProfileId(),
        new BigDecimal("15"),
        "Operador de teste",
        "Autorização sintética: USD 15 para um único vídeo novo, sem histórico.",
        confirmed);
  }

  /**
   * Confere valor integral, origem, itens incluídos e nenhuma passagem ou autorização comercial.
   */
  @Test
  void recordsExactIncrementalScopeWithoutHistoricalOrCommercialAuthority() {
    budget.authorize(cycle, request(UUID.randomUUID(), true), now);
    var proof = json.read(history.getFirst().getEvidenceJson());
    assertThat(proof.path("totalLimitUsd").decimalValue()).isEqualByComparingTo("15");
    assertThat(proof.path("includedCosts")).hasSize(8);
    for (String flag :
        List.of(
            "historicalCostsAuthorized",
            "mediaAuthorized",
            "commercialPublicationAuthorized",
            "financialReviewApproved")) assertThat(proof.path(flag).asBoolean(true)).isFalse();
    assertThat(cycle.getStage()).isEqualTo("PLANNING");
    assertThat(budget.read(cycle).startsProduction()).isFalse();
    assertThat(budget.read(cycle).authorizations().getFirst().current()).isTrue();
    verify(events, never())
        .findByCycleIdAndActionOrderByRevisionDesc(anyLong(), eq("AUTHORIZE_PREPARATION_BUDGET"));
    verify(events, never())
        .findByCycleIdAndActionOrderByRevisionDesc(anyLong(), eq("AUTHORIZE_VIDEO_BUDGET"));
  }

  /** Repetição da mesma decisão não duplica teto, revisão ou evento. */
  @Test
  void replayIsIdempotentAndAnotherKeyCannotRenewBudget() {
    var request = request(UUID.randomUUID(), true);
    budget.authorize(cycle, request, now);
    budget.authorize(cycle, request, now.plusSeconds(2));
    assertThat(history).hasSize(1);
    assertThat(cycle.getRevision()).isEqualTo(9L);
    assertThatThrownBy(() -> budget.authorize(cycle, request(UUID.randomUUID(), true), now))
        .hasMessageContaining("já possui um recibo");
  }

  /** A mesma chave não pode trocar o valor autorizado. */
  @Test
  void rejectsChangedReplay() {
    var request = request(UUID.randomUUID(), true);
    budget.authorize(cycle, request, now);
    var modified =
        new SingleVideoBudgetRequest(
            request.requestKey(),
            request.expectedRevision(),
            27L,
            114L,
            "video-v18",
            14L,
            69L,
            new BigDecimal("16"),
            request.operatorName(),
            request.authorizationSource(),
            true);
    assertThatThrownBy(() -> budget.authorize(cycle, modified, now))
        .hasMessageContaining("outros valores");
  }

  /** Outro produto e outra ocorrência usam as mesmas regras, sem exceção por nome ou ID. */
  @ParameterizedTest
  @CsvSource({"4,21,114,14,69", "7,99,207,33,87"})
  void supportsDistinctIdentities(
      long productId, long cycleId, long experimentId, long projectId, long profileId) {
    cycle.setProductId(productId);
    cycle.setId(cycleId);
    cycle.setExperimentId(experimentId);
    project.setProductId(productId);
    project.setId(projectId);
    project.setExperimentId(experimentId);
    project.setSalesVideoProfileId(profileId);
    budget.authorize(cycle, request(UUID.randomUUID(), true), now);
    assertThat(budget.read(cycle).authorizations().getFirst().videoProjectId())
        .isEqualTo(projectId);
    assertThat(history.getFirst().getCycleId()).isEqualTo(cycleId);
  }

  /** Projeto de outro contexto nunca recebe a decisão financeira deste ciclo. */
  @Test
  void rejectsDifferentProductExperimentVersionProfileAndTenant() {
    var request = request(UUID.randomUUID(), true);
    project.setProductId(7L);
    assertThatThrownBy(() -> budget.authorize(cycle, request, now))
        .hasMessageContaining("não pertence");
    project.setProductId(4L);
    project.setExperimentId(111L);
    assertThatThrownBy(() -> budget.authorize(cycle, request, now))
        .hasMessageContaining("não pertence");
    project.setExperimentId(114L);
    project.setCampaignKey("old");
    assertThatThrownBy(() -> budget.authorize(cycle, request, now))
        .hasMessageContaining("não pertence");
    project.setCampaignKey("video-v18");
    project.setSalesVideoProfileId(70L);
    assertThatThrownBy(() -> budget.authorize(cycle, request, now))
        .hasMessageContaining("não pertence");
    project.setSalesVideoProfileId(69L);
    project.setTenantId("test-b");
    assertThatThrownBy(() -> budget.authorize(cycle, request, now)).hasMessageContaining("tenant");
    assertThat(history).isEmpty();
  }

  /** Alteração de versão e encerramento preservam recibo consultável sem autorização vigente. */
  @Test
  void closedOrChangedVersionCannotInheritAuthorization() {
    budget.authorize(cycle, request(UUID.randomUUID(), true), now);
    cycle.setStatus("CLOSED");
    assertThat(budget.read(cycle).authorizations().getFirst().current()).isFalse();
    cycle.setStatus("OPEN");
    cycle.setProductVersion("v19");
    assertThat(budget.read(cycle).authorizations().getFirst().current()).isFalse();
    cycle.setProductVersion("video-v18");
    cycle.setVersionChangedAt(now.plusSeconds(1));
    assertThat(budget.read(cycle).authorizations().getFirst().current()).isFalse();
  }

  /** Confirmação ausente e revisão velha recusam gravação antes de produzir efeitos. */
  @Test
  void confirmationAndRevisionRemainRequired() {
    assertThatThrownBy(() -> budget.authorize(cycle, request(UUID.randomUUID(), false), now))
        .hasMessageContaining("Confirme");
    var old = request(UUID.randomUUID(), true);
    cycle.setRevision(9L);
    assertThatThrownBy(() -> budget.authorize(cycle, old, now)).hasMessageContaining("ciclo mudou");
    assertThat(history).isEmpty();
  }

  /** O vínculo corrente do projeto prevalece sobre os campos congelados no recibo. */
  @Test
  void changedProjectDoesNotCarryAuthorization() {
    budget.authorize(cycle, request(UUID.randomUUID(), true), now);
    project.setSalesVideoProfileId(70L);
    assertThat(budget.read(cycle).authorizations().getFirst().current()).isFalse();
    project.setSalesVideoProfileId(69L);
    project.setExperimentId(999L);
    assertThat(budget.read(cycle).authorizations().getFirst().current()).isFalse();
  }

  /** Valores negativos, zero, precisão indevida e teto fora do contrato não geram recibo. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"0", "-1", "15.001", "1000000"})
  void rejectsInvalidAmount(String amount) {
    var valid = request(UUID.randomUUID(), true);
    var invalid =
        new SingleVideoBudgetRequest(
            valid.requestKey(),
            valid.expectedRevision(),
            27L,
            114L,
            "video-v18",
            14L,
            69L,
            new BigDecimal(amount),
            valid.operatorName(),
            valid.authorizationSource(),
            true);
    assertThatThrownBy(() -> budget.authorize(cycle, invalid, now))
        .hasMessageContaining("teto positivo");
    assertThat(history).isEmpty();
  }

  /**
   * A fila interna de Plutus recebe só o recibo do projeto correto, sem depender de tenant HTTP.
   */
  @Test
  void handsTheSameReceiptToPlutusWithoutAnotherApproval() {
    budget.authorize(cycle, request(UUID.randomUUID(), true), now);
    TenantContextHolder.clear();
    var receipt = budget.forProject(project);
    assertThat(receipt.reference()).isEqualTo(history.getFirst().getEvidenceReference());
    assertThat(receipt.totalLimitUsd()).isEqualByComparingTo("15");
    project.setExperimentId(111L);
    assertThat(budget.forProject(project)).isNull();
  }

  /** Projeto inexistente devolve ausência explícita sem gravar uma autorização órfã. */
  @Test
  void missingProjectIsNotServerFailure() throws Exception {
    var valid = request(UUID.randomUUID(), true);
    project.setId(999L);
    http.perform(
            post("/api/business-process-chains/learning-cycles/v1/products/4/21/single-video-budget")
                .header("X-Tenant-ID", "test-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(valid)))
        .andExpect(status().isNotFound());
    assertThat(history).isEmpty();
  }

  /** A rota real exige tenant, valida o contrato e devolve recibo sem iniciar produção. */
  @Test
  void controllerRegistersAndReadsReceiptWithTenant() throws Exception {
    String url =
        "/api/business-process-chains/learning-cycles/v1/products/4/21/single-video-budget";
    String body = mapper.writeValueAsString(request(UUID.randomUUID(), true));
    http.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    http.perform(
            post(url)
                .header("X-Tenant-ID", "test-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startsProduction").value(false))
        .andExpect(jsonPath("$.authorizations[0].totalLimitUsd").value(15));
    http.perform(get(url).header("X-Tenant-ID", "test-a").param("chainId", "27"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authorizations.length()").value(1));
    http.perform(get(url).header("X-Tenant-ID", "test-b").param("chainId", "27"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authorizations.length()").value(0));
  }
}
