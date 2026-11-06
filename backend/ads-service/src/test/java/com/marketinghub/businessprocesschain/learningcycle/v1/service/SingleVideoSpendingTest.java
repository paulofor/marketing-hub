package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.singleVideoBudget.SingleVideoBudgetResponse.Authorization;
import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agentaudioreview.AgentAudioReviewRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.salesvideo.*;
import com.marketinghub.salesvideo.*;
import com.marketinghub.videocontract.v1.BoundedSpendContracts.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;

/** Responsabilidade: provar admissão compartilhada e recusas antes da rede, sem inferência paga. */
class SingleVideoSpendingTest {
  final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  final AgentTaskTargetContextProvider targets = mock(AgentTaskTargetContextProvider.class);
  final VideoProjectRepository projects = mock(VideoProjectRepository.class);
  final VideoProductionCycleRepository videoCycles = mock(VideoProductionCycleRepository.class);
  final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  final LearningCycleSingleVideoBudget budgets = mock(LearningCycleSingleVideoBudget.class);
  final OpenAiModelRepository models = mock(OpenAiModelRepository.class);
  final StudioCostLedgerEntryRepository costs = mock(StudioCostLedgerEntryRepository.class);
  final SingleVideoSpendRepository spends = mock(SingleVideoSpendRepository.class);
  final SalesVideoJobRepository jobs = mock(SalesVideoJobRepository.class);
  final ProductRepository products = mock(ProductRepository.class);
  final AgentRepository agents = mock(AgentRepository.class);
  final SalesVideoProviderModelRepository videoModels =
      mock(SalesVideoProviderModelRepository.class);
  final VideoCreditReservationRepository credits = mock(VideoCreditReservationRepository.class);
  final SalesVideoScriptRepository scripts = mock(SalesVideoScriptRepository.class);
  final com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository
      finalVideos =
          mock(
              com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository
                  .class);
  final LearningCycleJson json = new LearningCycleJson(mapper);
  final Map<Long, SingleVideoSpend> rows = new LinkedHashMap<>();
  final AtomicLong ids = new AtomicLong();
  final Instant now = Instant.parse("2026-11-05T22:00:00Z");
  final SingleVideoSpendLedger ledger = new SingleVideoSpendLedger(spends, json);
  final SingleVideoSpending service =
      new SingleVideoSpending(
          tasks,
          targets,
          projects,
          videoCycles,
          cycles,
          budgets,
          models,
          costs,
          ledger,
          json,
          jobs,
          videoModels,
          mock(AgentAudioReviewRepository.class),
          products,
          agents,
          credits,
          scripts,
          finalVideos);
  VideoProject project;
  LearningSalesCycle cycle;
  AgentTask task;
  Authorization receipt;

  /** Compõe fontes sintéticas e o ledger real, mantendo valores incertos retidos. */
  @BeforeEach
  void prepare() {
    service.setClock(Clock.fixed(now, ZoneOffset.UTC));
    ledger.setClock(Clock.fixed(now, ZoneOffset.UTC));
    project = new VideoProject();
    project.setId(14L);
    project.setProductId(4L);
    project.setExperimentId(114L);
    project.setCampaignKey("private-version");
    project.setTenantId("local-test");
    project.setSalesVideoProfileId(69L);
    project.setProviderPlan("Runway Gen-4.5 (RUNWAY)");
    project.setTargetDurationSeconds(32);
    cycle = new LearningSalesCycle();
    cycle.setId(21L);
    cycle.setExperimentId(114L);
    cycle.setStatus("OPEN");
    receipt =
        new Authorization(
            128L,
            "local-receipt",
            14L,
            69L,
            new BigDecimal("15"),
            "test",
            "LOCAL_FIXTURE",
            now.minusSeconds(300),
            true);
    var agent = new Agent();
    agent.setAgentKey("communication-director");
    agent.setAutomaticExecutionEnabled(true);
    task = new AgentTask();
    task.setId(9001L);
    task.setAssignedAgent(agent);
    task.setSourceReference("experiment:114");
    task.setTaskKind("MODEL");
    task.setStatus("IN_PROGRESS");
    task.setSpendingContract(SingleVideoSpending.PROTOCOL);
    var preparation = new VideoProductionCycle();
    preparation.setVideoProjectId(project.getId());
    preparation.setIrisAgentTaskId(task.getId());
    when(videoCycles.findByIrisAgentTaskId(task.getId())).thenReturn(List.of(preparation));
    when(tasks.findById(task.getId())).thenReturn(Optional.of(task));
    when(targets.resolve(eq(task.getSourceReference()), any()))
        .thenReturn(
            Optional.of(
                new AgentTaskTargetResponse(
                    task.getSourceReference(),
                    114L,
                    4L,
                    "local",
                    "Local",
                    "Local",
                    "private-version",
                    null,
                    null,
                    null,
                    null,
                    null)));
    when(projects.findByExperimentIdOrderByUpdatedAtDesc(114L)).thenReturn(List.of(project));
    when(budgets.protectedReceipt(project)).thenAnswer(a -> receipt);
    when(budgets.forProject(project)).thenAnswer(a -> receipt.current() ? receipt : null);
    when(cycles.findByExperimentId(114L)).thenReturn(Optional.of(cycle));
    when(cycles.findLockedById(21L)).thenReturn(Optional.of(cycle));
    when(products.findById(4L))
        .thenReturn(Optional.of(Product.builder().id(4L).automaticExecutionEnabled(true).build()));
    when(spends.findByReceiptIdAndOperationKey(anyLong(), anyString()))
        .thenAnswer(
            a ->
                rows.values().stream()
                    .filter(
                        r ->
                            r.getReceiptId().equals(a.getArgument(0))
                                && r.getOperationKey().equals(a.getArgument(1)))
                    .findFirst());
    when(spends.findByReceiptIdOrderByIdAsc(anyLong()))
        .thenAnswer(
            a ->
                rows.values().stream()
                    .filter(r -> r.getReceiptId().equals(a.getArgument(0)))
                    .toList());
    when(spends.findById(anyLong()))
        .thenAnswer(a -> Optional.ofNullable(rows.get(a.getArgument(0))));
    when(spends.saveAndFlush(any()))
        .thenAnswer(
            a -> {
              SingleVideoSpend r = a.getArgument(0);
              if (r.getId() == null) r.setId(ids.incrementAndGet());
              rows.put(r.getId(), r);
              return r;
            });
    var model = new OpenAiModel();
    model.setCode("gpt-6-astra");
    model.setPriceInputStandard(new BigDecimal("10"));
    model.setPriceOutputStandard(new BigDecimal("50"));
    model.setLastPricingSyncAt(now.minusSeconds(60));
    model.setPricingSource("https://developers.openai.com/api/docs/models/gpt-6-astra");
    when(models.findByCode(model.getCode())).thenReturn(Optional.of(model));
    handshake();
  }

  /**
   * Aceita só a experiência privada explicitamente herdada, mantendo vídeo alvo e revisões
   * independentes.
   */
  @Test
  void validatesInheritedFunctionalRecordingWithoutMigratingHistory() {
    cycle.setInheritedLearningJson(
        "{\"events\":[{\"evidence\":{\"privatePrototype\":{\"prototypeVersion\":\"accepted-source\",\"desktopValidated\":true,\"mobileValidated\":true,\"testDataExcluded\":true,\"noExternalSideEffects\":true}}}]}");
    var manifest = mapper.createObjectNode().put("sourcePrototypeVersion", "accepted-source");
    service.validateProductRecording(project, manifest);
    assertThat(project.getCampaignKey()).isEqualTo("private-version");
    manifest.put("sourcePrototypeVersion", "another-source");
    assertThatThrownBy(() -> service.validateProductRecording(project, manifest))
        .hasMessageContaining("não consta");
    receipt =
        new Authorization(
            128L,
            "old",
            14L,
            69L,
            new BigDecimal("15"),
            "test",
            "LOCAL_FIXTURE",
            now.minusSeconds(300),
            false);
    assertThatThrownBy(() -> service.validateProductRecording(project, manifest))
        .hasMessageContaining("não vigente");
  }

  /** Reconciliação tardia ou tarefa encerrada bloqueiam o envio mesmo depois da reserva inicial. */
  @Test
  void rechecksCoverageAndTaskStatusImmediatelyBeforeProviderCall() {
    var lease = service.reserve("communication-director", task.getId(), request());
    var external = new com.marketinghub.financialagent.StudioCostLedgerEntry();
    external.setCreatedAt(now);
    external.setSourceType("EXTERNAL_FEE");
    external.setEstimatedCostUsd(new BigDecimal("13"));
    when(costs.findByExperimentIdOrderByCreatedAtAsc(114L)).thenReturn(List.of(external));
    assertThatThrownBy(() -> service.begin("communication-director", task.getId(), lease.id()))
        .hasMessageContaining("Cobertura integral mudou");
    assertThat(ledger.find(128L, "task:" + task.getId() + ":main").state()).isEqualTo("RESERVED");
    when(costs.findByExperimentIdOrderByCreatedAtAsc(114L)).thenReturn(List.of());
    task.setStatus("COMPLETED");
    assertThatThrownBy(() -> service.begin("communication-director", task.getId(), lease.id()))
        .hasMessageContaining("elegível");
  }

  /** Retira apenas contexto HTTP local, sem alterar fontes ou autorizações de produção. */
  @AfterEach
  void cleanup() {
    RequestContextHolder.resetRequestAttributes();
  }

  /** Mantém o modelo original, limita a saída e recupera o mesmo resultado sem novo envio. */
  @Test
  void reservesOneImmutableRequestAndReplaysAfterStop() {
    var lease = service.reserve("communication-director", task.getId(), request());
    assertThat(lease.maximumUsd()).isEqualByComparingTo("2.50820000");
    assertThat(service.begin("communication-director", task.getId(), lease.id()).mayInvoke())
        .isTrue();
    assertThat(service.begin("communication-director", task.getId(), lease.id()).mayInvoke())
        .isFalse();
    cycle.setStatus("CLOSED");
    var saved =
        service.record(
            "communication-director",
            task.getId(),
            lease.id(),
            new Result(200, "{\"id\":\"response-local\"}", "req-local"));
    assertThat(saved.rawResponse()).contains("response-local");
    assertThat(ledger.liability(128L)).isEqualByComparingTo(lease.maximumUsd());
    assertThatThrownBy(() -> service.begin("communication-director", task.getId(), lease.id()))
        .hasMessageContaining("encerrado");
  }

  /** Resposta incerta mantém o máximo; não há retry que crie novo saldo. */
  @Test
  void uncertainResponseRetainsLiability() {
    var lease = service.reserve("communication-director", task.getId(), request());
    service.begin("communication-director", task.getId(), lease.id());
    service.record(
        "communication-director", task.getId(), lease.id(), new Result(null, null, null));
    assertThat(service.begin("communication-director", task.getId(), lease.id()).mayInvoke())
        .isFalse();
    assertThat(service.context("communication-director", task.getId()).reservation().state())
        .isEqualTo("UNCERTAIN");
    assertThat(rows).hasSize(1);
  }

  /** Exerce os gates de recibo, STOP e transporte antes da primeira operação paga. */
  @Test
  void rejectsLegacyStopAndStaleReceipt() {
    RequestContextHolder.resetRequestAttributes();
    assertThat(service.claimBlocker(task)).contains("executor antigo");
    assertThatThrownBy(() -> service.reserve("communication-director", task.getId(), request()))
        .hasMessageContaining("Transporte");
    handshake();
    task.getAssignedAgent().setAutomaticExecutionEnabled(false);
    assertThatThrownBy(() -> service.reserve("communication-director", task.getId(), request()))
        .hasMessageContaining("STOP");
    task.getAssignedAgent().setAutomaticExecutionEnabled(true);
    receipt =
        new Authorization(
            128L, "local", 14L, 69L, new BigDecimal("15"), "test", "local", now, false);
    assertThat(service.context("communication-director", task.getId()).required()).isTrue();
    assertThatThrownBy(() -> service.reserve("communication-director", task.getId(), request()))
        .hasMessageContaining("vigente");
    assertThat(rows).isEmpty();
  }

  /** Tools e fontes remotas não podem ampliar o consumo depois da reserva. */
  @Test
  void refusesUnboundedOrChangedBody() {
    var input = request();
    ((ObjectNode) input.request()).putArray("tools").addObject().put("type", "web_search");
    assertThatThrownBy(() -> service.reserve("communication-director", task.getId(), input))
        .hasMessageContaining("Campo fora");
    var accepted = service.reserve("communication-director", task.getId(), request());
    var other = request();
    ((ObjectNode) other.request()).put("max_output_tokens", 1000);
    assertThatThrownBy(() -> service.reserve("communication-director", task.getId(), other))
        .hasMessageContaining("outro request");
    assertThat(rows).hasSize(1);
    assertThat(accepted.receiptId()).isEqualTo(128L);
  }

  /** Replica a regra em outro projeto; nenhum identificador especial concede saldo adicional. */
  @ParameterizedTest
  @ValueSource(longs = {14, 9014})
  void sameReceiptCannotFundParallelRenders(long id) {
    ledger.bindRenderJob(128L, 21L, id, 7001L);
    ledger.bindRenderJob(128L, 21L, id, 7001L);
    assertThatThrownBy(() -> ledger.bindRenderJob(128L, 21L, id, 7002L))
        .hasMessageContaining("outro render");
    assertThat(rows).hasSize(1);
  }

  /**
   * Mantém o caminho antes válido sem recibo e impede fallback após remover o contexto protegido.
   */
  @Test
  void legacyIsPreservedButLostProtectedContextFailsClosed() {
    when(budgets.protectedReceipt(project)).thenReturn(null);
    assertThatThrownBy(() -> service.context("communication-director", task.getId()))
        .hasMessageContaining("fallback");
    task.setSpendingContract(null);
    assertThat(service.context("communication-director", task.getId()).required()).isFalse();
    assertThat(service.claimBlocker(task)).isNull();
  }

  /** Cotação antiga não autoriza rede; renovação preserva a reserva e sua auditoria. */
  @Test
  void expiredUnsubmittedQuoteRenewsWithoutFreeingUncertainMoney() {
    var lease = service.reserve("communication-director", task.getId(), request());
    ledger.setClock(Clock.fixed(now.plusSeconds(301), ZoneOffset.UTC));
    assertThatThrownBy(() -> service.begin("communication-director", task.getId(), lease.id()))
        .hasMessageContaining("venceu");
    var renewed = service.reserve("communication-director", task.getId(), request());
    assertThat(renewed.id()).isEqualTo(lease.id());
    assertThat(rows.get(lease.id()).getPricingEvidenceJson()).contains("renewedAt");
    assertThat(service.begin("communication-director", task.getId(), lease.id()).mayInvoke())
        .isTrue();
  }

  /** Soma reservas anteriores inclusive quando pertencem a outro executor. */
  @Test
  void refusesOverspendAndCrossContextReservation() {
    ledger.reserve(
        128L,
        21L,
        14L,
        "job:8800:CLIP_1",
        "RUNWAY",
        "gen4.5",
        "{}",
        new BigDecimal("14"),
        new BigDecimal("15"),
        BigDecimal.ZERO,
        "{}");
    assertThatThrownBy(() -> service.reserve("communication-director", task.getId(), request()))
        .hasMessageContaining("teto integral");
    var lease =
        ledger.reserve(
            500L,
            71L,
            9014L,
            "task:other:main",
            "OPENAI",
            "local",
            "{}",
            BigDecimal.ONE,
            BigDecimal.TEN,
            BigDecimal.ZERO,
            "{}");
    assertThatThrownBy(() -> service.begin("communication-director", task.getId(), lease.id()))
        .hasMessageContaining("outro contexto");
  }

  /**
   * Créditos retidos e chamadas individuais ocupam a mesma parcela, sem somar o clipe duas vezes.
   */
  @Test
  void creditEnvelopeAndAgentsShareOneLimit() {
    var videoCycle = new VideoProductionCycle();
    videoCycle.setId(47L);
    videoCycle.setVideoProjectId(14L);
    videoCycle.setProductId(4L);
    videoCycle.setExperimentId(114L);
    assertThat(service.holdProviderCredits(project, videoCycle, new BigDecimal("4.8"))).isNull();
    assertThat(service.holdProviderCredits(project, videoCycle, new BigDecimal("4.8"))).isNull();
    var agent = service.reserve("communication-director", task.getId(), request());
    BigDecimal before = ledger.liability(128L);
    var clip =
        ledger.reserveClip(
            128L,
            21L,
            14L,
            "cycle:47:PROVIDER_CREDITS",
            "job:31:CLIP_1",
            "gen4.5",
            "{\"duration\":10}",
            new BigDecimal("1.5"),
            new BigDecimal("15"),
            BigDecimal.ZERO,
            "{}");
    assertThat(ledger.liability(128L)).isEqualByComparingTo(before);
    assertThat(ledger.begin(clip.id(), 128L, 14L, "job:31:CLIP_1").mayInvoke()).isTrue();
    ledger.record(clip.id(), 128L, 14L, "job:31:CLIP_1", new Result(null, null, null));
    assertThat(ledger.liability(128L))
        .isEqualByComparingTo(agent.maximumUsd().add(new BigDecimal("4.8")));
    assertThatThrownBy(
            () ->
                ledger.reserve(
                    128L,
                    21L,
                    14L,
                    "other",
                    "TEST",
                    "test",
                    "{}",
                    BigDecimal.TEN,
                    new BigDecimal("15"),
                    BigDecimal.ZERO,
                    "{}"))
        .hasMessageContaining("teto integral");
  }

  /**
   * Falta de cobertura retorna causa persistível antes da tarefa financeira, sem alterar o recibo.
   */
  @Test
  void deniedCreditHoldCreatesNoSpend() {
    var videoCycle = new VideoProductionCycle();
    videoCycle.setId(47L);
    videoCycle.setVideoProjectId(14L);
    videoCycle.setProductId(4L);
    videoCycle.setExperimentId(114L);
    assertThat(service.holdProviderCredits(project, videoCycle, new BigDecimal("16")))
        .contains("teto integral");
    assertThat(rows).isEmpty();
  }

  /** Escuta usa toda a janela oficial; resposta conhecida deixa de constar como envio incerto. */
  @Test
  void audioReviewIsBoundedAndItsResponsePreserved() {
    task.getAssignedAgent().setAgentKey("customer-agent");
    finalReviewFixture();
    var request =
        mapper.createObjectNode().put("model", "gpt-audio-1.5").put("max_completion_tokens", 2000);
    request.putArray("modalities").add("text");
    request.putArray("messages").addObject().put("role", "user").put("content", "Áudio local");
    assertThat(service.reserveAudio("customer-agent", task.getId(), 53L, request))
        .isEqualByComparingTo("4.116");
    assertThat(ledger.find(128L, "task:9001:audio:53").state()).isEqualTo("SUBMITTED");
    service.recordAudio("customer-agent", task.getId(), 53L, "{\"id\":\"audio-local\"}");
    assertThat(ledger.find(128L, "task:9001:audio:53").state()).isEqualTo("RESPONSE_RECORDED");
    assertThat(ledger.liability(128L)).isEqualByComparingTo("4.116");
    assertThatThrownBy(() -> service.reserveAudio("customer-agent", task.getId(), 53L, request))
        .hasMessageContaining("anterior");
  }

  /** Outra atividade do mesmo experimento permanece protegida e não obtém fallback nem reserva. */
  @Test
  void unrelatedTaskInSameExperimentCannotSpendVideoReceipt() {
    when(videoCycles.findByIrisAgentTaskId(task.getId())).thenReturn(List.of());
    assertThat(service.context("communication-director", task.getId()).required()).isTrue();
    assertThat(service.context("communication-director", task.getId()).blocker())
        .contains("não está vinculada");
    assertThat(service.claimBlocker(task)).contains("não está vinculada");
    assertThatThrownBy(() -> service.reserve("communication-director", task.getId(), request()))
        .hasMessageContaining("não está vinculada");
    assertThat(rows).isEmpty();
  }

  /** Só o gate de Plutus efetivamente registrado pelo ciclo pode usar a autorização do vídeo. */
  @Test
  void financialTaskMustBeOwnedByVideoCycle() {
    task.getAssignedAgent().setAgentKey("financial-agent");
    task.setSourceReference("video-production-cycle:47");
    var videoCycle = new VideoProductionCycle();
    videoCycle.setId(47L);
    videoCycle.setVideoProjectId(project.getId());
    when(videoCycles.findById(47L)).thenReturn(Optional.of(videoCycle));
    when(projects.findById(project.getId())).thenReturn(Optional.of(project));
    assertThat(service.context("financial-agent", task.getId()).blocker())
        .contains("não está vinculada");
    videoCycle.setAgentTaskId(task.getId());
    task.setTaskKind("GATE_DECISION");
    task.setGateStatus("PENDING");
    task.setStatus("PENDING");
    task.setSpendingContract(null);
    assertThat(service.context("financial-agent", task.getId()).blocker()).isNull();
    var lease = service.reserve("financial-agent", task.getId(), request());
    assertThat(service.begin("financial-agent", task.getId(), lease.id()).mayInvoke()).isTrue();
  }

  /**
   * A revisão correta passa, mas áudio de outro asset e mídia de outro render não recebem consumo.
   */
  @Test
  void finalReviewUsesOnlyTheBoundRenderAndExactAudioAsset() {
    task.getAssignedAgent().setAgentKey("customer-agent");
    var video = finalReviewFixture();
    assertThat(service.context("customer-agent", task.getId()).blocker()).isNull();
    assertThatThrownBy(
            () ->
                service.reserveAudio(
                    "customer-agent", task.getId(), 99L, mapper.createObjectNode()))
        .hasMessageContaining("outra mídia");
    video.getSalesVideoJob().setId(7002L);
    assertThat(service.context("customer-agent", task.getId()).blocker())
        .contains("não está vinculada");
    task.getAssignedAgent().setAgentKey("meta-ad-approver");
    task.setProcessActivityId("commercial");
    video.getSalesVideoJob().setId(7001L);
    assertThat(service.context("meta-ad-approver", task.getId()).blocker()).isNull();
    video.getSalesVideoJob().setTenantId("another-tenant");
    assertThat(service.context("meta-ad-approver", task.getId()).blocker())
        .contains("não está vinculada");
  }

  /** Materializa somente fontes locais da revisão de um render fixado, sem chamada a provedor. */
  private com.marketinghub.experiment.video.ExperimentVideoAsset finalReviewFixture() {
    var definition = new com.marketinghub.businessprocess.BusinessProcessDefinition();
    definition.setProcessCode("creative-production-approval");
    task.setProcessDefinition(definition);
    task.setProcessActivityId("customer");
    ledger.bindRenderJob(128L, 21L, 14L, 7001L);
    var profile = new SalesVideoProfile();
    profile.setId(69L);
    var job = new SalesVideoJob();
    job.setId(7001L);
    job.setProfile(profile);
    job.setTenantId("local-test");
    job.setSpendingContract(SingleVideoSpending.PROTOCOL);
    job.setMetadataJson("{\"videoProjectId\":14,\"productVersion\":\"private-version\"}");
    var video = new com.marketinghub.experiment.video.ExperimentVideoAsset();
    video.setId(53L);
    video.setSalesVideoJob(job);
    video.setResponseJson("{\"hls_delivery\":{\"sourceSha256\":\"" + "a".repeat(64) + "\"}}");
    when(finalVideos.findReadyExperimentVideosForReview(any(), isNull(), eq(4L), eq(114L)))
        .thenReturn(List.of(video));
    return video;
  }

  /** Cria o corpo efetivo da API com entrada contada e limite de todos os tokens de saída. */
  Reserve request() {
    var request =
        mapper
            .createObjectNode()
            .put("model", "gpt-6-astra")
            .put("service_tier", "flex")
            .put("store", false)
            .put("max_output_tokens", 32768);
    request
        .putArray("input")
        .addObject()
        .put("role", "user")
        .putArray("content")
        .addObject()
        .put("type", "input_text")
        .put("text", "Texto local");
    return new Reserve(
        request,
        mapper.createObjectNode().put("object", "response.input_tokens").put("input_tokens", 1000));
  }

  /** Simula apenas a versão do transporte; não fornece teto ou aprovação no header. */
  void handshake() {
    var request = new MockHttpServletRequest();
    request.addHeader(SingleVideoSpending.HEADER, SingleVideoSpending.PROTOCOL);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }
}
