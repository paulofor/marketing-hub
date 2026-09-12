package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.controller.LearningCycleController;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.videoBudget.*;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: proteger escopo, rastreabilidade e idempotência do teto financeiro dos vídeos.
 */
class LearningCycleVideoBudgetTest {
  final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  final LearningCycleJson json = new LearningCycleJson(mapper);
  final LearningSalesCycleEventRepository events = mock(LearningSalesCycleEventRepository.class);
  final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  final ProductRepository products = mock(ProductRepository.class);
  final ExperimentRepository experiments = mock(ExperimentRepository.class);
  final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  final LearningCycleVideoBudget budget = new LearningCycleVideoBudget(events, json);
  final LearningSalesCycle cycle = new LearningSalesCycle();
  final List<LearningSalesCycleEvent> history = new ArrayList<>();
  final Product product = Product.builder().id(4L).internalName("Vega").name("MUSA").build();
  final Instant now = Instant.parse("2026-09-12T23:00:00Z");
  MockMvc http;
  LearningCycleService service;

  /** Simula apenas persistência adjacente; controller, validação, service e recibos são reais. */
  @BeforeEach
  void setup() {
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setChainDefinitionId(14L);
    cycle.setProcessDefinitionId(76L);
    cycle.setProductVersion("musa-v12");
    cycle.setStage("VIDEO_BRIEF");
    cycle.setStatus("OPEN");
    cycle.setRevision(7);
    cycle.setVersionChangedAt(now.minusSeconds(60));
    when(events.findByCycleIdAndActionOrderByRevisionDesc(2L, "AUTHORIZE_VIDEO_BUDGET"))
        .thenAnswer(call -> history.reversed());
    when(events.findByCycleIdAndRequestKey(eq(2L), anyString()))
        .thenAnswer(
            call ->
                history.stream()
                    .filter(e -> e.getRequestKey().equals(call.getArgument(1)))
                    .findFirst());
    when(events.saveAndFlush(any()))
        .thenAnswer(
            call -> {
              LearningSalesCycleEvent event = call.getArgument(0);
              event.setId((long) history.size() + 1);
              history.add(event);
              return event;
            });
    when(cycles.findLocked(4L, 2L)).thenReturn(Optional.of(cycle));
    when(cycles.findById(2L)).thenReturn(Optional.of(cycle));
    when(products.findLockedById(4L)).thenReturn(Optional.of(product));
    when(products.findById(4L)).thenReturn(Optional.of(product));
    var experiment = new Experiment();
    experiment.setId(92L);
    experiment.setProduct(product);
    when(experiments.findById(92L)).thenReturn(Optional.of(experiment));
    var process = new BusinessProcessDefinition();
    process.setVersionNumber(4);
    when(processes.findById(76L)).thenReturn(Optional.of(process));
    service =
        new LearningCycleService(
            cycles,
            events,
            products,
            experiments,
            null,
            processes,
            null,
            json,
            null,
            null,
            null,
            null,
            null);
    ReflectionTestUtils.setField(service, "videoBudget", budget);
    http = MockMvcBuilders.standaloneSetup(new LearningCycleController(service)).build();
  }

  /** Prepara autorização sintética sem herdar qualquer orçamento real. */
  private AuthorizeVideoBudgetRequest request(String amount) {
    return new AuthorizeVideoBudgetRequest(
        UUID.randomUUID(),
        cycle.getRevision(),
        14L,
        92L,
        cycle.getProductVersion(),
        new BigDecimal(amount),
        "Operador local",
        "Demonstrar o primeiro ajuste e testar vendas",
        true);
  }

  /** Confirma persistência, consulta, referência e escopo do formulário completo. */
  @Test
  void recordsTotalWithoutAdvancingOrCreatingPaidWork() throws Exception {
    http.perform(
            post("/api/business-process-chains/learning-cycles/v1/products/4/2/video-budget")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.write(request("20.50"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.currentAuthorization.budgetLimitUsd").value(20.5))
        .andExpect(jsonPath("$.revision").value(8));
    var proof = json.read(history.getFirst().getEvidenceJson());
    for (String flag :
        List.of(
            "financialReviewApproved",
            "mediaAuthorized",
            "billingAuthorized",
            "commercialPublicationAuthorized")) assertThat(proof.path(flag).asBoolean()).isFalse();
    assertThat(proof.path("videoRoles").size()).isEqualTo(2);
    assertThat(cycle.getStage()).isEqualTo("VIDEO_BRIEF");
    http.perform(
            get(
                "/api/business-process-chains/learning-cycles/v1/products/4/2/video-budget?chainId=14"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.history.length()").value(1))
        .andExpect(
            jsonPath("$.cycleUrl")
                .value(
                    "/business-process-chains/learning-cycles?chainId=14&productId=4&cycleId=2"));
  }

  /** Impede duplicar recibo quando a resposta HTTP anterior não chega ao navegador. */
  @Test
  void identicalRetryHasOneReceipt() {
    var request = request("20.00");
    budget.authorize(cycle, request, true, now);
    budget.authorize(cycle, request, true, now.plusSeconds(5));
    assertThat(history).hasSize(1);
    assertThat(cycle.getRevision()).isEqualTo(8);
  }

  /** Impede usar uma chave aceita para mudar o limite sem nova decisão. */
  @Test
  void refusesChangedReplay() {
    var request = request("20");
    budget.authorize(cycle, request, true, now);
    var altered =
        new AuthorizeVideoBudgetRequest(
            request.requestKey(),
            7L,
            14L,
            92L,
            "musa-v12",
            new BigDecimal("30"),
            request.operatorName(),
            request.justification(),
            true);
    assertThatThrownBy(() -> budget.authorize(cycle, altered, true, now))
        .hasMessageContaining("conteúdo diferente");
    assertThat(history).hasSize(1);
  }

  /** Preserva limites substituídos e impede que abas antigas sobrescrevam uma nova decisão. */
  @Test
  void preservesReplacementsAndRejectsStaleRevision() {
    var stale = request("50");
    budget.authorize(cycle, request("20"), true, now);
    assertThatThrownBy(() -> budget.authorize(cycle, stale, true, now))
        .hasMessageContaining("ciclo mudou");
    budget.authorize(cycle, request("25"), true, now.plusSeconds(2));
    var result = budget.response(cycle, product, true);
    assertThat(result.history()).hasSize(2);
    assertThat(result.history().getFirst().current()).isTrue();
    assertThat(result.history().getLast().current()).isFalse();
    assertThat(result.currentAuthorization().budgetLimitUsd()).isEqualByComparingTo("25");
  }

  /** Recusa valores incompatíveis antes de gravar ou converter centavos silenciosamente. */
  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "20.001", "1000000"})
  void refusesInvalidAmount(String amount) throws Exception {
    http.perform(
            post("/api/business-process-chains/learning-cycles/v1/products/4/2/video-budget")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.write(request(amount))))
        .andExpect(status().isBadRequest());
    assertThat(history).isEmpty();
  }

  /** Campos obrigatórios não podem ser omitidos mesmo por clientes que não usam o formulário. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "confirmed",
        "budgetLimitUsd",
        "operatorName",
        "justification",
        "requestKey",
        "expectedRevision"
      })
  void refusesMissingRequiredField(String field) throws Exception {
    var body = (ObjectNode) json.read(json.write(request("20")));
    body.remove(field);
    http.perform(
            post("/api/business-process-chains/learning-cycles/v1/products/4/2/video-budget")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.write(body)))
        .andExpect(status().isBadRequest());
    assertThat(history).isEmpty();
  }

  /** Não aceita confirmação falsa nem confundida com presença do campo JSON. */
  @Test
  void refusesUncheckedScope() throws Exception {
    var body = (ObjectNode) json.read(json.write(request("20")));
    body.put("confirmed", false);
    http.perform(
            post("/api/business-process-chains/learning-cycles/v1/products/4/2/video-budget")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.write(body)))
        .andExpect(status().isBadRequest());
  }

  /** Mantém o histórico disponível sem permitir alterar teto após início do fluxo de produção. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "CAMPAIGN_VIDEO",
        "PDE_ENTRY_VIDEO",
        "VIDEO_APPROVAL",
        "AUTHORIZATION",
        "PLANNING"
      })
  void otherStagesAreReadOnly(String stage) {
    budget.authorize(cycle, request("20"), true, now);
    cycle.setStage(stage);
    assertThat(budget.response(cycle, product, true).canAuthorize()).isFalse();
    assertThatThrownBy(() -> budget.authorize(cycle, request("30"), true, now))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(history).hasSize(1);
  }

  /**
   * Novo produto, experimento, cadeia ou versão não herdam a autorização da ocorrência anterior.
   */
  @Test
  void rejectsForeignIdentityAndInvalidatesChangedVersion() {
    assertThatThrownBy(() -> service.videoBudget(5L, 2L, 14L))
        .hasMessageContaining("neste produto");
    assertThatThrownBy(() -> service.videoBudget(4L, 2L, 15L)).hasMessageContaining("cadeia");
    var wrong =
        new AuthorizeVideoBudgetRequest(
            UUID.randomUUID(),
            7L,
            14L,
            91L,
            "musa-v12",
            BigDecimal.TEN,
            "Operador local",
            "Contexto incorreto",
            true);
    assertThatThrownBy(() -> budget.authorize(cycle, wrong, true, now))
        .hasMessageContaining("divergentes");
    budget.authorize(cycle, request("20"), true, now);
    cycle.setProductVersion("musa-v13");
    cycle.setVersionChangedAt(now.plusSeconds(1));
    assertThat(budget.current(cycle)).isNull();
    assertThat(budget.response(cycle, product, true).history()).hasSize(1);
  }

  /** Processo legado e ciclo encerrado não recebem autorização nova. */
  @Test
  void closedAndLegacyCyclesAreReadOnly() {
    assertThat(budget.response(cycle, product, false).canAuthorize()).isFalse();
    assertThatThrownBy(() -> budget.authorize(cycle, request("20"), false, now))
        .hasMessageContaining("não possui");
    cycle.setStatus("CLOSED");
    assertThatThrownBy(() -> budget.authorize(cycle, request("20"), true, now))
        .hasMessageContaining("encerrado");
  }

  /**
   * O briefing recebe o limite persistido, sem confiar em teto ou aprovação enviados pelo cliente.
   */
  @Test
  void attachesCanonicalLimitAndRefusesForgedReferences() {
    budget.authorize(cycle, request("20"), true, now);
    var data =
        mapper
            .createObjectNode()
            .put("productionBudgetReference", budget.current(cycle).reference());
    data.putObject("productionBudget")
        .put("budgetLimitUsd", 9000)
        .put("financialReviewApproved", true);
    budget.attachToBrief(cycle, data);
    assertThat(data.path("productionBudget").path("budgetLimitUsd").decimalValue())
        .isEqualByComparingTo("20");
    assertThat(data.path("productionBudget").path("financialReviewApproved").asBoolean()).isFalse();
    data.put("productionBudgetReference", "internal://learning-cycles/1/video-budget/other");
    assertThatThrownBy(() -> budget.attachToBrief(cycle, data))
        .hasMessageContaining("teto vigente");
  }

  /** Não converte referências livres anteriores em autorização humana estruturada. */
  @Test
  void legacyReferencesRemainExplicitlyUnstructured() {
    var data =
        mapper
            .createObjectNode()
            .put("productionBudgetReference", "Documento histórico de produção");
    data.putObject("productionBudget").put("budgetLimitUsd", 9000);
    budget.attachToBrief(cycle, data);
    assertThat(data.has("productionBudget")).isFalse();
    assertThat(budget.current(cycle)).isNull();
    data.put("productionBudgetReference", "internal://learning-cycles/2/video-budget/missing");
    assertThatThrownBy(() -> budget.attachToBrief(cycle, data))
        .hasMessageContaining("não corresponde");
  }
}
