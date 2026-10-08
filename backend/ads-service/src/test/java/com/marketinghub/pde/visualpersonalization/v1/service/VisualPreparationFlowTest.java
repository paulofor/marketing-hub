package com.marketinghub.pde.visualpersonalization.v1.service;

import static com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationService.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.experiment.*;
import com.marketinghub.financialagent.*;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.imagegenerator.ImageGenerationRequest;
import com.marketinghub.imagegenerator.service.ImageGenerationUsageCost;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.pde.visualpersonalization.v1.service.contract.VisualPreparationContract.*;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.financialagent.*;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: comprovar isolamento, limite e recuperação da entrega visual sem provedor real.
 */
class VisualPreparationFlowTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final ImageGenerationRequestRepository images =
      mock(ImageGenerationRequestRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final StudioCostLedgerEntryRepository ledgerRepository =
      mock(StudioCostLedgerEntryRepository.class);
  private final FinancialAgentExecutionRepository financial =
      mock(FinancialAgentExecutionRepository.class);
  private final ExperimentStrategistExecutionRepository strategist =
      mock(ExperimentStrategistExecutionRepository.class);
  private final OpenAiPricingService pricing = mock(OpenAiPricingService.class);
  private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
  private final Map<String, ImageGenerationRequest> rows = new LinkedHashMap<>();
  private final Map<String, StudioCostLedgerEntry> costs = new LinkedHashMap<>();
  private final Map<Long, Product> productMap = new HashMap<>();
  private final Map<Long, CommercialPlan> planMap = new HashMap<>();
  private VisualPreparationBudget budget;
  private VisualPreparationService service;
  private VisualPreparationExecution execution;

  /** Configura referências distintas e persistência simulada com o mesmo serviço de custo real. */
  @BeforeEach
  void setup() throws Exception {
    for (long id : List.of(501L, 502L)) {
      var product = new Product();
      product.setId(id);
      product.setName("Produto sintético " + id);
      product.setAutomaticExecutionEnabled(true);
      product.setPdeExperienceJson(
          """
          {"privatePrototypeAcceptance":{"prototypeVersion":"visual-private-v1","privateAccessUrl":"http://localhost:5184"},
           "agentValidation":{"status":"PASS"}}
          """);
      productMap.put(id, product);
      var experiment = new Experiment();
      experiment.setId(id + 200);
      experiment.setProduct(product);
      experiment.setStatus(ExperimentStatus.PLANNED);
      var plan = new CommercialPlan();
      plan.setId(id + 100);
      plan.setExperiment(experiment);
      plan.setNextAction(authorization(id, id + 200));
      planMap.put(plan.getId(), plan);
      when(experiments.findById(experiment.getId())).thenReturn(Optional.of(experiment));
    }
    when(products.findById(anyLong()))
        .thenAnswer(i -> Optional.ofNullable(productMap.get(i.getArgument(0))));
    when(products.findLockedById(anyLong()))
        .thenAnswer(i -> Optional.ofNullable(productMap.get(i.getArgument(0))));
    when(plans.findById(anyLong()))
        .thenAnswer(i -> Optional.ofNullable(planMap.get(i.getArgument(0))));
    when(images.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              ImageGenerationRequest row = i.getArgument(0);
              if (row.getId() == null) row.setId((long) rows.size() + 1);
              rows.put(row.getJobId(), row);
              return row;
            });
    when(images.findFirstByJobId(anyString()))
        .thenAnswer(i -> Optional.ofNullable(rows.get(i.getArgument(0))));
    when(images.findByJobId(anyString()))
        .thenAnswer(i -> Optional.ofNullable(rows.get(i.getArgument(0))));
    when(images.findByProductIdAndCommercialPlanIdAndExperimentIdAndBatchJobId(
            anyLong(), anyLong(), anyLong(), anyString()))
        .thenAnswer(
            i ->
                rows.values().stream()
                    .filter(
                        r ->
                            r.getProductId().equals(i.getArgument(0))
                                && r.getCommercialPlanId().equals(i.getArgument(1))
                                && r.getExperimentId().equals(i.getArgument(2))
                                && r.getBatchJobId().equals(i.getArgument(3)))
                    .findFirst());
    when(images.existsByProductIdAndExperimentIdAndStatusIn(anyLong(), anyLong(), anyCollection()))
        .thenAnswer(
            i ->
                rows.values().stream()
                    .anyMatch(
                        r ->
                            r.getProductId().equals(i.getArgument(0))
                                && r.getExperimentId().equals(i.getArgument(1))
                                && ((Collection<?>) i.getArgument(2)).contains(r.getStatus())));
    when(images.findByStatusAndJobIdStartingWithOrderByCreatedAtAsc(
            anyString(), eq(PREFIX), any(Pageable.class)))
        .thenAnswer(
            i ->
                rows.values().stream()
                    .filter(r -> r.getStatus().equals(i.getArgument(0)))
                    .toList());
    when(images.findByStatusAndOpenAiRequestBodyIsNullAndJobIdStartingWithOrderByCreatedAtAsc(
            anyString(), eq(PREFIX), any(Pageable.class)))
        .thenAnswer(
            i ->
                rows.values().stream()
                    .filter(
                        r ->
                            r.getStatus().equals(i.getArgument(0))
                                && r.getOpenAiRequestBody() == null)
                    .toList());
    when(ledgerRepository.findBySourceTypeAndSourceId(anyString(), anyString()))
        .thenAnswer(i -> Optional.ofNullable(costs.get(i.getArgument(1))));
    when(ledgerRepository.save(any()))
        .thenAnswer(
            i -> {
              StudioCostLedgerEntry cost = i.getArgument(0);
              if (cost.getCreatedAt() == null) cost.setCreatedAt(Instant.now());
              costs.put(cost.getSourceId(), cost);
              return cost;
            });
    when(ledgerRepository.findByCommercialPlanIdOrderByCreatedAtAsc(anyLong()))
        .thenAnswer(
            i ->
                costs.values().stream()
                    .filter(c -> c.getCommercialPlanId().equals(i.getArgument(0)))
                    .toList());
    when(financial.findByCommercialPlanIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
    when(strategist.findByCommercialPlanIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
    when(pricing.estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong()))
        .thenReturn(Optional.of(new BigDecimal("0.01")));
    when(transactions.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
    budget = new VisualPreparationBudget(ledgerRepository, financial, strategist, json);
    service = new VisualPreparationService(products, plans, experiments, images, budget, json);
    ReflectionTestUtils.setField(service, "model", "gpt-5.6");
    execution =
        new VisualPreparationExecution(
            images,
            products,
            service,
            budget,
            new StudioCostLedgerService(ledgerRepository),
            new ImageGenerationUsageCost(pricing, json),
            transactions);
  }

  /**
   * Repete o percurso com outro produto e confirma entrega, custo e recuperação sem reenfileirar.
   */
  @ParameterizedTest
  @ValueSource(longs = {501, 502})
  void deliversAndRecoversForDifferentProducts(long id) throws Exception {
    String job = register(id);
    assertThat(execution.pending()).hasSize(1);
    execution.claim(job);
    execution.request(job, new RequestAudit(request(job)));
    var response = response(job);
    var result = execution.receive(job, new Result(response, 200, true));
    assertThat(result.path("status").asText()).isEqualTo("PDE_COMPLETED");
    assertThat(result.path("imageBase64").asText()).isNotBlank();
    assertThat(result.path("functionalReviewStatus").asText())
        .isEqualTo("PENDING_INDEPENDENT_REVIEW");
    assertThat(result.path("paymentEnabled").asBoolean()).isFalse();
    assertThat(service.session(job, secret()).path("input")).isEqualTo(json.valueToTree(input()));
    execution.receive(job, new Result(response, 200, true));
    execution.apply(job);
    assertThat(rows).hasSize(1);
    assertThat(costs).hasSize(1);
    assertThat(execution.pending()).isEmpty();
    assertThat(
            budget
                .read(productMap.get(id), planMap.get(id + 100), id + 200, null)
                .knownEstimatedUsd())
        .isEqualByComparingTo("0.0405");
  }

  /** Perda do retorno de reserva recupera a mesma tentativa somente antes da request auditada. */
  @Test
  void recoversInterruptedClaimBeforeAnyProviderRequest() throws Exception {
    String job = register(501);
    execution.claim(job);
    var started = rows.get(job).getStartedAt();
    assertThat(execution.pending()).hasSize(1);
    assertThat(execution.claim(job).path("status").asText()).isEqualTo("PDE_RUNNING");
    assertThat(rows.get(job).getStartedAt()).isEqualTo(started);
    assertThat(costs).hasSize(1);
    execution.request(job, new RequestAudit(request(job)));
    assertThat(execution.pending()).isEmpty();
    assertThatThrownBy(() -> execution.claim(job)).isInstanceOf(ResponseStatusException.class);
    assertThat(execution.receive(job, new Result(response(job), 200, true)).path("status").asText())
        .isEqualTo("PDE_COMPLETED");
  }

  /** Uma reserva abortada antes da request não deixa custo artificialmente desconhecido. */
  @Test
  void interruptedClaimBlockedByStopHasNoProviderCost() {
    String job = register(501);
    execution.claim(job);
    productMap.get(501L).setAutomaticExecutionEnabled(false);
    assertThat(execution.claim(job).path("status").asText()).isEqualTo("PDE_BLOCKED");
    assertThat(costs.get(job).getEstimatedCostUsd()).isZero();
    assertThat(costs.get(job).getCostEvidence()).isEqualTo("NO_PROVIDER_REQUEST");
    assertThat(rows.get(job).getOpenAiRequestBody()).isNull();
  }

  /** Mantém a operação idempotente e recusa outra entrada sob a mesma identidade. */
  @Test
  void repeatedRegistrationNeverCreatesSecondInference() {
    String job = register(501);
    assertThat(register(501)).isEqualTo(job);
    var different = new Input("Outra ocasião", List.of("camisa", "calça"), "Conforto", "");
    var original = create(501);
    var changed =
        new Create(
            original.commercialPlanId(),
            original.experimentId(),
            original.productVersion(),
            original.accessToken(),
            original.operationKey(),
            original.authorizationHash(),
            true,
            different);
    assertThatThrownBy(() -> service.create(501L, changed))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(rows).hasSize(1);
  }

  /** Impede troca de plano, experimento, credencial e autorização entre produtos. */
  @Test
  void refusesAnotherScopeOrCredential() {
    var original = create(501);
    assertThatThrownBy(() -> service.create(502L, original))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(rows).isEmpty();
    String job = register(501);
    assertThatThrownBy(() -> service.session(job, "b".repeat(64)))
        .isInstanceOf(ResponseStatusException.class);
    var foreignHash =
        new Create(
            602L,
            702L,
            "visual-private-v1",
            secret(),
            "a".repeat(32),
            original.authorizationHash(),
            true,
            input());
    assertThatThrownBy(() -> service.create(502L, foreignHash))
        .isInstanceOf(ResponseStatusException.class);
  }

  /** O STOP anterior à reserva fica persistido e não deixa uma pendência girando na fila. */
  @Test
  void stopBeforeClaimRecordsBlockedStateWithoutProvider() {
    String job = register(501);
    productMap.get(501L).setAutomaticExecutionEnabled(false);
    var blocked = execution.claim(job);
    assertThat(blocked.path("status").asText()).isEqualTo("PDE_BLOCKED");
    assertThat(blocked.path("error").asText()).contains("STOP");
    assertThat(execution.pending()).isEmpty();
    assertThat(costs).isEmpty();
  }

  /** Outra operação ativa é recusada; retomar a reserva antes da request não duplica consumo. */
  @Test
  void activeAttemptPreventsAnotherReservation() {
    String job = register(501);
    var original = create(501);
    var duplicate =
        new Create(
            601L,
            701L,
            original.productVersion(),
            secret(),
            "b".repeat(32),
            original.authorizationHash(),
            true,
            input());
    assertThatThrownBy(() -> service.create(501L, duplicate))
        .isInstanceOf(ResponseStatusException.class);
    execution.claim(job);
    assertThat(execution.claim(job).path("status").asText()).isEqualTo("PDE_RUNNING");
    assertThat(costs).hasSize(1);
  }

  /** Custo sem fonte e consumo já acima do total continuam bloqueando novas tentativas. */
  @Test
  void missingCostAndExceededBudgetBlockBeforeQueueing() {
    var entry = new StudioCostLedgerEntry();
    entry.setSourceId("previous");
    entry.setCommercialPlanId(601L);
    entry.setExperimentId(701L);
    entry.setCreatedAt(Instant.now());
    costs.put("previous", entry);
    assertThatThrownBy(() -> register(501)).isInstanceOf(ResponseStatusException.class);
    entry.setEstimatedCostUsd(new BigDecimal("11"));
    assertThatThrownBy(() -> register(501)).isInstanceOf(ResponseStatusException.class);
    assertThat(rows).isEmpty();
  }

  /** Uma fonte desconhecida anterior à autorização permanece histórica sem virar custo zero. */
  @Test
  void keepsUnknownHistoricalCostOutsideNewPreparation() {
    var entry = new StudioCostLedgerEntry();
    entry.setSourceId("historical");
    entry.setCommercialPlanId(601L);
    entry.setExperimentId(701L);
    entry.setCreatedAt(Instant.parse("2026-10-04T10:00:00Z"));
    costs.put("historical", entry);
    assertThat(register(501)).startsWith(PREFIX);
    assertThat(entry.getEstimatedCostUsd()).isNull();
  }

  /** A request deve transportar a entrada correta, Flex e os limites antes do provedor. */
  @Test
  void refusesRequestForAnotherInputOrTier() throws Exception {
    String job = register(501);
    execution.claim(job);
    var wrongTier = request(job);
    wrongTier.put("service_tier", "default");
    assertThatThrownBy(() -> execution.request(job, new RequestAudit(wrongTier)))
        .isInstanceOf(ResponseStatusException.class);
    var wrong = request(job);
    ((ObjectNode) wrong.path("input").path(2)).put("content", "{}");
    var mismatch = wrong;
    assertThatThrownBy(() -> execution.request(job, new RequestAudit(mismatch)))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(rows.get(job).getOpenAiRequestBody()).isNull();
  }

  /** Não aceita a resposta de outro job, mesmo quando a imagem e os tokens são válidos. */
  @Test
  void refusesCrossExecutionCallback() throws Exception {
    String first = register(501), second = register(502);
    execution.claim(first);
    execution.claim(second);
    execution.request(first, new RequestAudit(request(first)));
    execution.request(second, new RequestAudit(request(second)));
    var firstResponse = response(first);
    assertThatThrownBy(() -> execution.receive(second, new Result(firstResponse, 200, true)))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(rows.get(second).getOpenAiResponseBody()).isNull();
    assertThat(costs.get(second).getEstimatedCostUsd()).isNull();
  }

  /** Uma falha ao aplicar custo preserva o payload já confirmado e permite replay sem IA. */
  @Test
  void callbackApplicationFailurePreservesResponseForReplay() throws Exception {
    String job = register(501);
    execution.claim(job);
    execution.request(job, new RequestAudit(request(job)));
    var response = response(job);
    when(pricing.estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong()))
        .thenThrow(new IllegalStateException("Catálogo temporariamente indisponível"));
    assertThatThrownBy(() -> execution.receive(job, new Result(response, 200, true)))
        .isInstanceOf(IllegalStateException.class);
    assertThat(rows.get(job).getStatus()).isEqualTo("PDE_RAW_RECEIVED");
    assertThat(json.readTree(rows.get(job).getOpenAiResponseBody())).isEqualTo(response);
    doReturn(Optional.of(new BigDecimal("0.01")))
        .when(pricing)
        .estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong());
    assertThat(execution.apply(job).path("status").asText()).isEqualTo("PDE_COMPLETED");
    assertThat(costs).hasSize(1);
  }

  /**
   * Resposta sem uso recupera a imagem, mas bloqueia outra inferência até conciliar o mesmo
   * payload.
   */
  @Test
  void missingPriceCanBeReconciledWithoutGeneration() throws Exception {
    String job = register(501);
    execution.claim(job);
    execution.request(job, new RequestAudit(request(job)));
    var response = response(job);
    when(pricing.estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong()))
        .thenReturn(Optional.empty());
    var pending = execution.receive(job, new Result(response, 200, true));
    assertThat(pending.path("status").asText()).isEqualTo("PDE_COST_PENDING");
    assertThat(pending.path("imageBase64").asText()).isNotBlank();
    assertThat(budget.read(productMap.get(501L), planMap.get(601L), 701L, null).costComplete())
        .isFalse();
    when(pricing.estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong()))
        .thenReturn(Optional.of(new BigDecimal("0.01")));
    assertThat(execution.apply(job).path("status").asText()).isEqualTo("PDE_COMPLETED");
  }

  /** Um PNG apresentado sem chamada ao provedor não comprova geração personalizada real. */
  @Test
  void imageWithoutProviderCallNeverCountsAsRealGeneration() throws Exception {
    String job = register(501);
    execution.claim(job);
    var result = execution.receive(job, new Result(response(job), 200, false));
    assertThat(result.path("status").asText()).isEqualTo("PDE_FAILED");
    assertThat(result.path("providerCalls").asInt()).isZero();
    assertThat(result.has("imageBase64")).isFalse();
    assertThat(costs.get(job).getEstimatedCostUsd()).isZero();
  }

  /** PNG ausente ou múltiplas imagens não contam como saída funcional da etapa. */
  @Test
  void invalidImageNeverCountsAsCompleted() throws Exception {
    String job = register(501);
    execution.claim(job);
    execution.request(job, new RequestAudit(request(job)));
    var response = response(job);
    ((ObjectNode) response.path("output").path(0)).put("result", "dGVzdA==");
    assertThat(execution.receive(job, new Result(response, 200, true)).path("status").asText())
        .isEqualTo("PDE_FAILED");
    assertThat(rows.get(job).getOpenAiResponseBody()).contains("dGVzdA==");
    assertThat(costs.get(job).getEstimatedCostUsd()).isPositive();
  }

  /** Rejeita alterações tardias do payload já concluído em vez de apagar a auditoria. */
  @Test
  void changedResponseCannotReplaceCompletedResult() throws Exception {
    String job = register(501);
    execution.claim(job);
    execution.request(job, new RequestAudit(request(job)));
    var original = response(job);
    execution.receive(job, new Result(original, 200, true));
    var changed = original.deepCopy();
    changed.put("id", "another-response");
    assertThatThrownBy(() -> execution.receive(job, new Result(changed, 200, true)))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(json.readTree(rows.get(job).getOpenAiResponseBody())).isEqualTo(original);
  }

  /** Dados da homologação excluem credencial, payload bruto e promessa de aceite comercial. */
  @Test
  void viewsKeepSecretsAndRawPayloadPrivate() {
    String job = register(501);
    String visible = service.session(job, secret()).toString();
    assertThat(visible)
        .doesNotContain(secret(), "sessionHash", "openAiResponseBody", "authorizationHash");
    assertThat(service.context(501L, 601L, 701L).path("budget").path("maximumUsd").decimalValue())
        .isEqualByComparingTo("10");
  }

  /** Monta autorização explícita de referência sem usar produto ou orçamento produtivo. */
  private String authorization(long product, long experiment) {
    return "Autorização explícita do usuário em 05/10/2026: teto TOTAL de USD 10 para homologar geração personalizada real "
        + "(produto "
        + product
        + ", experimento "
        + experiment
        + ", execução 901). Sem transferência; sem mídia, campanha, cobrança real.";
  }

  /** Retorna somente dados sintéticos pequenos para o teste determinístico. */
  private Input input() {
    return new Input(
        "Jantar informal",
        List.of("camisa azul", "calça bege"),
        "Conforto, sem salto",
        "Não comprar peças");
  }

  /** Constrói uma correlação fixa dentro da fixture, nunca uma credencial real. */
  private String secret() {
    return "a".repeat(64);
  }

  /** Usa o hash da origem correta e mantém a mesma chave para testar idempotência. */
  private Create create(long id) {
    return new Create(
        id + 100,
        id + 200,
        "visual-private-v1",
        secret(),
        "a".repeat(32),
        VisualPreparationBudget.hash(planMap.get(id + 100).getNextAction()),
        true,
        input());
  }

  /** Abre uma preparação e retorna somente sua identidade opaca. */
  private String register(long id) {
    return service.create(id, create(id)).path("jobId").asText();
  }

  /** Representa a request que o worker envia com entrada separada das instruções. */
  private ObjectNode request(String job) throws Exception {
    var row = rows.get(job);
    var context = json.readTree(row.getPrompt());
    var request = json.createObjectNode();
    request.put("model", "gpt-5.6");
    request.put("service_tier", "flex");
    request.put("max_tool_calls", 1);
    request.put("max_output_tokens", 4096);
    request.putObject("tool_choice").put("type", "image_generation");
    request.set(
        "tools",
        json.readTree(
            "[{\"type\":\"image_generation\",\"action\":\"generate\",\"output_format\":\"png\",\"model\":\"gpt-image-2.5-sunburst\",\"size\":\"1024x1024\",\"quality\":\"high\"}]"));
    var input = request.putArray("input");
    input.addObject().put("role", "system").put("content", "Instruções de teste");
    input.addObject().put("role", "developer").put("content", "Contexto de teste");
    input.addObject().put("role", "user").put("content", context.path("input").toString());
    request
        .putObject("metadata")
        .put("mh_job_id", job)
        .put("mh_input_hash", context.path("inputHash").asText());
    return request;
  }

  /** Sintetiza PNG pelo código e uso completo do provedor, sem HTTP externo ou IA. */
  private ObjectNode response(String job) throws Exception {
    var image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(Color.BLUE);
    graphics.fillRect(0, 0, 1024, 1024);
    graphics.dispose();
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    var response =
        (ObjectNode)
            json.readTree(
                """
        {"id":"fixture-response","status":"completed","model":"gpt-5.6-sol","service_tier":"flex",
         "tools":[{"type":"image_generation","model":"gpt-image-2.5-sunburst"}],
         "output":[{"type":"image_generation_call","status":"completed","result":""}],
         "usage":{"input_tokens":1000,"output_tokens":100,"input_tokens_details":{"cached_tokens":0}},
         "tool_usage":{"image_gen":{"input_tokens":100,"output_tokens":1000,
          "input_tokens_details":{"text_tokens":100,"image_tokens":0},
          "output_tokens_details":{"image_tokens":1000,"text_tokens":0}}}}
        """);
    ((ObjectNode) response.path("output").path(0))
        .put("result", Base64.getEncoder().encodeToString(bytes.toByteArray()));
    response.set("metadata", request(job).path("metadata"));
    return response;
  }
}
