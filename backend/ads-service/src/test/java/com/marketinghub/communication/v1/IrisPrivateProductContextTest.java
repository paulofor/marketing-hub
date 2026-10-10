package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.execution.service.backendactivity.BackendProductProcessActivityReadiness;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.agentvalidation.*;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: proteger a continuidade pré-comercial e a integridade das provas de origem. */
class IrisPrivateProductContextTest {
  private static final String SOURCE = "product:91010@agent-validation-v1";
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final ProductRepository products = mock(ProductRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final AgentTaskTargetContextProvider targets = mock(AgentTaskTargetContextProvider.class);
  private final PdeAgentValidationGateActivityExecutor validator =
      mock(PdeAgentValidationGateActivityExecutor.class);
  private final IrisPrivateProductContext provider =
      new IrisPrivateProductContext(products, tasks, instances, targets, validator, json);
  private final Product product =
      Product.builder()
          .id(91010L)
          .slug("local-mira")
          .name("Rotina local")
          .internalName("Mira local")
          .validationDefinitionVersion("PDE_AGENT_VALIDATED_V1")
          .build();
  private final ObjectNode pde = json.createObjectNode();
  private final ObjectNode proof = json.createObjectNode();
  private final BusinessProcessActivityInstance gate = new BusinessProcessActivityInstance();
  private final List<PdeValidationTaskSnapshot> reviews = new ArrayList<>();
  private final List<AgentTaskFunctionalSnapshot> origin = new ArrayList<>();

  /**
   * Prepara dados sintéticos da descoberta e cinco provas aprovadas, sem experimento ou ciclo de
   * venda.
   */
  @BeforeEach
  void setup() throws Exception {
    var process = new BusinessProcessDefinition();
    process.setId(91070L);
    process.setProcessCode("pde-construction-approval");
    var activity = new BusinessProcessActivityDefinition();
    activity.setId(91011L);
    activity.setActivityId("agentValidationGate");
    activity.setProcessDefinition(process);
    gate.setId(91242L);
    gate.setActivityDefinition(activity);
    gate.setStatus("COMPLETED");
    gate.setObjectiveAchieved(true);
    when(products.findById(product.getId())).thenReturn(Optional.of(product));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "pde-construction-approval", SOURCE))
        .thenReturn(List.of(gate));
    when(validator.historicalEvidenceReadiness(process, activity, product, SOURCE))
        .thenReturn(new BackendProductProcessActivityReadiness(true, "Provas locais aprovadas"));
    pde.putObject("lineage")
        .put("cycleId", 91064L)
        .put("dossierId", 91036L)
        .put("opportunityId", 91053L);
    pde.putObject("marketStrategy")
        .put("contractVersion", "MARKET_STRATEGY_V3")
        .put("offerThesis", "Rotina consultável");
    pde.putObject("economics").put("offerPriceBrl", 49).put("commercialSpendAuthorized", false);
    pde.putObject("harness").put("format", "Experiência privada").put("audiovisualRequired", false);
    pde.putObject("privatePrototypeAcceptance")
        .put("status", "READY")
        .put("prototypeVersion", "local-mira-v3");
    var target =
        new AgentTaskTargetResponse(
            SOURCE,
            null,
            product.getId(),
            product.getSlug(),
            product.getName(),
            product.getInternalName(),
            "local-mira-v3",
            "https://mira.example/private",
            null,
            null,
            null,
            null,
            pde);
    when(targets.resolve(SOURCE, "pde-construction-approval")).thenReturn(Optional.of(target));
    proof
        .put("evidenceType", "PDE_AGENT_VALIDATION_GATE_V1")
        .put("productId", product.getId())
        .put("productSlug", product.getSlug())
        .put("sourceReference", SOURCE)
        .put("prototypeVersion", target.experienceVersion())
        .put("publicUrl", target.publicUrl())
        .put("trafficClass", "AGENT_VALIDATION")
        .put("internalMarker", "mh_internal_test")
        .put("mediaSpendAuthorizedBrl", 0);
    for (String key :
        List.of(
            "paymentEnabled",
            "publicationAuthorized",
            "campaignAuthorized",
            "humanEvidenceClaimed",
            "commercialEvidenceClaimed")) proof.put(key, false);
    var evidence = proof.putArray("taskEvidence");
    long id = 91371;
    for (String code :
        List.of(
            "technicalHomologation",
            "psiqueAdherent",
            "psiqueRecovery",
            "psiqueSafety",
            "commercialIntegrityReview")) {
      String raw = "{\"decision\":\"APPROVED\",\"scenario\":\"" + code + "\"}";
      reviews.add(
          new PdeValidationTaskSnapshot(
              id, process.getId(), code, "COMPLETED", null, null, raw, null));
      evidence
          .addObject()
          .put("taskId", id++)
          .put("activityId", code)
          .put("agentKey", "customer-agent")
          .put("resultSha256", sha(raw));
    }
    when(tasks.findPdeValidationTaskSnapshots(SOURCE, "pde-construction-approval"))
        .thenReturn(reviews);
    origin.add(
        origin(
            91327L,
            "marketStrategy",
            "experiment-strategist",
            "marketStrategicContract",
            "marketStrategy"));
    origin.add(origin(91329L, "economics", "financial-agent", "economics", "economics"));
    origin.add(
        origin(
            91331L, "productArchitecture", "landing-generator", "productArchitecture", "harness"));
    when(tasks.findFunctionalSnapshots(
            eq("product-discovery-cycle:91064"), anyCollection(), isNull()))
        .thenReturn(origin);
    updateProof();
  }

  /** Mantém a origem da descoberta separada da referência privada do produto. */
  private AgentTaskFunctionalSnapshot origin(
      long id, String activity, String agent, String resultKey, String productKey) {
    var result =
        json.createObjectNode()
            .put("decision", "APPROVE")
            .put("selectedDossierId", 91036L)
            .put("selectedOpportunityId", 91053L);
    result.set(resultKey, pde.path(productKey).deepCopy());
    return new AgentTaskFunctionalSnapshot(
        id,
        91067L,
        "pde-commercial-plan-offer",
        activity,
        agent,
        "COMPLETED",
        Instant.EPOCH,
        Instant.EPOCH,
        result.toString());
  }

  /** Atualiza somente a prova sintética persistida do cenário local. */
  private void updateProof() {
    gate.setObjectiveEvidenceJson(proof.toString());
  }

  /** Confere duas identidades com estratégia antiga preservada e planejamento novo aprovado. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {91010, 92020})
  void initialPlanningUsesAcceptedSoftwareProofWithoutRewritingOrigin(long id) throws Exception {
    String reference = prepareScopedProof(id);
    ((ObjectNode) pde.path("marketStrategy"))
        .put("contractVersion", "MARKET_STRATEGY_V4")
        .put("offerThesis", "Hipótese atual aprovada em outro planejamento");
    assertThat(provider.resolve(reference).orElseThrow())
        .containsEntry("inputReadiness", "BLOCKED");
    var software = provider.approvedProductProof(reference).orElseThrow();
    assertThat(software)
        .containsEntry("inputReadiness", "READY")
        .containsEntry("proofScope", IrisPrivateProductContext.PROOF_SCOPE)
        .doesNotContainKeys("marketStrategicContract", "economics", "productArchitecture");
    var initial = initialPlanningWithRealProof(true);
    assertThat(initial).containsEntry("productProofScope", IrisPrivateProductContext.PROOF_SCOPE);
    assertThat(initial)
        .containsEntry("inputReadiness", "READY")
        .containsEntry(
            "mode", IrisCommunicationMaterializationContextProvider.INITIAL_EXPERIMENT_PRIVATE_MODE)
        .containsEntry("publicationAuthorized", false)
        .containsEntry("paymentEnabled", false);
    assertThat(initial.get("prototypeVersion")).isEqualTo("local-mira-v3");
    assertThat(
            json.readTree(origin.getFirst().resultJson())
                .path("marketStrategicContract")
                .path("contractVersion")
                .asText())
        .isEqualTo("MARKET_STRATEGY_V3");
    verify(instances, never()).save(any());
    verify(tasks, never()).save(any());
    verify(products, never()).save(any());
  }

  /** A prova técnica não substitui Plutus nem os demais pareceres do planejamento atual. */
  @Test
  void acceptedSoftwareProofDoesNotApproveMissingCurrentEconomics() throws Exception {
    prepareScopedProof(91010);
    var initial = initialPlanningWithRealProof(false);
    assertThat(initial).containsEntry("inputReadiness", "BLOCKED");
    assertThat(initial.get("missingRequiredPredecessors").toString()).contains("Plutus");
  }

  /** Vincula fonte, versão, URL e bytes sintéticos da homologação à identidade escolhida. */
  private String prepareScopedProof(long id) throws Exception {
    String reference = "product:" + id + "@agent-validation-v1";
    product.setId(id);
    product.setSlug("local-product-" + id);
    var target =
        new AgentTaskTargetResponse(
            reference,
            null,
            id,
            product.getSlug(),
            product.getName(),
            product.getInternalName(),
            "local-mira-v3",
            "https://mira.example/private",
            null,
            null,
            null,
            null,
            pde);
    when(products.findById(id)).thenReturn(Optional.of(product));
    when(targets.resolve(reference, "pde-construction-approval")).thenReturn(Optional.of(target));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "pde-construction-approval", reference))
        .thenReturn(List.of(gate));
    when(validator.historicalEvidenceReadiness(any(), any(), same(product), eq(reference)))
        .thenReturn(
            new BackendProductProcessActivityReadiness(true, "Prova técnica sintética aprovada"));
    when(tasks.findPdeValidationTaskSnapshots(reference, "pde-construction-approval"))
        .thenReturn(reviews);
    proof
        .put("productId", id)
        .put("productSlug", product.getSlug())
        .put("sourceReference", reference);
    var technical =
        json.createObjectNode()
            .put("contractVersion", "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1")
            .put("decision", "APPROVED")
            .put("sourceReference", reference)
            .put("productId", id)
            .put("prototypeVersion", target.experienceVersion())
            .put("publicUrl", target.publicUrl());
    technical
        .putArray("artifacts")
        .addObject()
        .put("artifactId", id + 1)
        .put("sha256", "a".repeat(64))
        .put("sourceUrl", target.publicUrl());
    String raw = technical.toString();
    var previous = reviews.getFirst();
    reviews.set(
        0,
        new PdeValidationTaskSnapshot(
            previous.id(),
            previous.processDefinitionId(),
            previous.processActivityId(),
            "COMPLETED",
            null,
            null,
            raw,
            null));
    ((ObjectNode) proof.path("taskEvidence").get(0)).put("resultSha256", sha(raw));
    updateProof();
    return reference;
  }

  /** Usa os dois provedores produtivos e simula somente registros de planejamento e catálogo. */
  private Map<String, Object> initialPlanningWithRealProof(boolean economicsReady)
      throws Exception {
    var seed = new IrisCommunicationMaterializationContextProviderTest();
    var fixture = seed.fixture(List.of(), false);
    fixture.plan().getExperiment().setProduct(product);
    when(fixture.plans().findByExperimentReference(88L)).thenReturn(List.of(fixture.plan()));
    var strategy =
        seed.strategy()
            .replace("MARKET_STRATEGY_V3", "MARKET_STRATEGY_V4")
            .replace("READY_FOR_PRIVATE_VALIDATION", "READY_FOR_AGENT_VALIDATION")
            .replace("privateValidationPlan", "agentValidationPlan");
    var economics = seed.economics().replace("PDE_PRIVATE_ECONOMICS_V1", "PDE_AGENT_ECONOMICS_V1");
    if (!economicsReady) economics = economics.replace("APPROVE", "BLOCK");
    when(fixture
            .tasks()
            .findFunctionalSnapshots("experiment:88", Set.of("pde-commercial-plan-offer"), null))
        .thenReturn(
            List.of(
                seed.snapshot(91401L, "marketStrategy", "experiment-strategist", strategy),
                seed.snapshot(91402L, "economics", "financial-agent", economics),
                seed.snapshot(
                    91403L, "productArchitecture", "landing-generator", seed.architecture())));
    org.springframework.test.util.ReflectionTestUtils.setField(
        fixture.provider(), "privateProducts", provider);
    var context = fixture.provider().resolve("experiment:88").orElseThrow();
    if (economicsReady) {
      String visualDestination =
          org.springframework.test.util.ReflectionTestUtils.invokeMethod(
              Class.forName("com.marketinghub.agenttask.FrozenCreativeVisualAuthorization"),
              "resolve",
              json.valueToTree(context),
              "experiment:88");
      assertThat(visualDestination).isEqualTo("https://mira.example/private");
      exportClaimedIrisInput(fixture.provider());
    }
    return context;
  }

  /** Valida o transporte HTTP real da tarefa sem reservar, escrever ou chamar modelos. */
  private void exportClaimedIrisInput(IrisCommunicationMaterializationContextProvider communication)
      throws Exception {
    var repository = mock(AgentTaskRepository.class);
    var agents = mock(com.marketinghub.repository.jpa.agent.AgentRepository.class);
    var agent = new com.marketinghub.agent.Agent();
    agent.setId(91409L);
    agent.setAgentKey("communication-director");
    var process = new BusinessProcessDefinition();
    process.setId(91410L);
    process.setProcessCode("pde-communication-sales-journey");
    process.setVersionNumber(11);
    process.setDiagramJson("{\"nodes\":[]}");
    var task = new AgentTask();
    task.setId(91411L);
    task.setAssignedAgent(agent);
    task.setProcessDefinition(process);
    task.setProcessActivityId("communicationContract");
    task.setSourceReference("experiment:88");
    task.setStatus("IN_PROGRESS");
    task.setTaskKind("WORK");
    task.setCreatedAt(Instant.EPOCH);
    when(repository.findById(task.getId())).thenReturn(Optional.of(task));
    var service =
        new AgentTaskService(
            repository,
            null,
            agents,
            mock(
                com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository
                    .class),
            null,
            null,
            json,
            null,
            MarketStrategicContextProvider.empty(),
            AgentTaskTargetContextProvider.empty());
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "communicationMaterializationContextProvider", communication);
    var http =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new InternalAgentTaskExecutionController(
                    service, mock(AgentTaskVisualEvidenceService.class)))
            .build();
    String body =
        http.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/internal/agent-tasks/communication-director/stage-executions/91411"))
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    var input = json.readTree(body);
    var frozen = json.readTree(input.path("processContextJson").asText());
    assertThat(
            frozen.path("communicationMaterializationContext").path("productProofScope").asText())
        .isEqualTo(IrisPrivateProductContext.PROOF_SCOPE);
    assertThat(frozen.path("marketStrategicContract").path("contractVersion").asText())
        .isEqualTo("MARKET_STRATEGY_V4");
    assertThat(input.path("sourceReference").asText()).isEqualTo("experiment:88");
    verify(repository, never()).save(any());
    String file = System.getenv("IRIS_INITIAL_INPUT_FILE");
    if (file != null) Files.writeString(Path.of(file), body);
  }

  /** Calcula o hash dos bytes usados pelo contrato de evidência. */
  private String sha(String text) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
  }

  /** Entrega V3, economia, protótipo e provas reais sem fabricar plano, experimento ou ciclo. */
  @Test
  void resolvesProductBeforeExperimentAndExportsWorkerContract() throws Exception {
    product.setCommercialStatus("VALIDACAO_COMERCIAL");
    var context = provider.resolve(SOURCE).orElseThrow();
    assertThat(context)
        .containsEntry("inputReadiness", "READY")
        .containsEntry("mode", "PRODUCT_PRIVATE")
        .containsEntry("prototypeVersion", "local-mira-v3")
        .doesNotContainKeys("experiment", "cycleId", "commercialPlanId");
    assertThat(json.valueToTree(context).path("approvedUpstreamArtifacts")).hasSize(7);
    assertThat(context)
        .containsEntry("paymentEnabled", false)
        .containsEntry("publicationAuthorized", false)
        .containsEntry("externalMediaSpendAuthorized", false);
    verify(validator).historicalEvidenceReadiness(any(), any(), same(product), eq(SOURCE));
    String output = System.getenv("MIRA_IRIS_INPUT_FILE");
    if (output != null)
      Files.writeString(
          Path.of(output),
          json.writeValueAsString(
              Map.of(
                  "taskId",
                  91402L,
                  "sourceReference",
                  SOURCE,
                  "processCode",
                  "pde-communication-sales-journey",
                  "activityId",
                  "communicationContract",
                  "processContextJson",
                  json.writeValueAsString(
                      Map.of(
                          "marketStrategicContract",
                          context.get("marketStrategicContract"),
                          "communicationMaterializationContext",
                          context)))));
  }

  /** Rejeita alterações de identidade, versão, marcadores e permissões mesmo com gate concluído. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "productId",
        "productSlug",
        "sourceReference",
        "prototypeVersion",
        "publicUrl",
        "trafficClass",
        "internalMarker",
        "paymentEnabled",
        "publicationAuthorized",
        "campaignAuthorized",
        "humanEvidenceClaimed",
        "commercialEvidenceClaimed",
        "mediaSpendAuthorizedBrl"
      })
  void rejectsGateTampering(String key) {
    if (proof.path(key).isBoolean()) proof.put(key, true);
    else if (proof.path(key).isNumber()) proof.put(key, 9);
    else proof.put(key, "divergente");
    updateProof();
    assertThat(provider.resolve(SOURCE).orElseThrow()).containsEntry("inputReadiness", "BLOCKED");
    assertThat(provider.approvedProductProof(SOURCE).orElseThrow())
        .containsEntry("inputReadiness", "BLOCKED");
  }

  /** Um novo parecer bloqueado impede consumir a prova aprovada anteriormente. */
  @Test
  void rejectsNewerReviewAndChangedBytes() {
    reviews.add(
        new PdeValidationTaskSnapshot(
            91999L, 91070L, "psiqueSafety", "BLOCKED", null, null, "{}", null));
    assertThat(provider.resolve(SOURCE).orElseThrow()).containsEntry("inputReadiness", "BLOCKED");
    assertThat(provider.approvedProductProof(SOURCE).orElseThrow())
        .containsEntry("inputReadiness", "BLOCKED");
    reviews.removeLast();
    ((ObjectNode) proof.path("taskEvidence").get(0)).put("resultSha256", "0".repeat(64));
    updateProof();
    assertThat(provider.resolve(SOURCE).orElseThrow()).containsEntry("inputReadiness", "BLOCKED");
    assertThat(provider.approvedProductProof(SOURCE).orElseThrow())
        .containsEntry("inputReadiness", "BLOCKED");
  }

  /** Não aceita contrato alterado no produto nem parecer de origem posterior reprovado. */
  @Test
  void rejectsChangedOriginAndRevokedGate() {
    ((ObjectNode) pde.path("economics")).put("offerPriceBrl", 99);
    assertThat(provider.resolve(SOURCE).orElseThrow()).containsEntry("inputReadiness", "BLOCKED");
    ((ObjectNode) pde.path("economics")).put("offerPriceBrl", 49);
    gate.setObjectiveAchieved(false);
    assertThat(provider.resolve(SOURCE).orElseThrow()).containsEntry("inputReadiness", "BLOCKED");
  }

  /** Artefatos anteriores à aprovação vigente não são reutilizados e novo planejamento bloqueia. */
  @Test
  void excludesStaleCommunicationAndRejectsNewPlanning() {
    proof.put("completedAt", "2026-09-13T01:00:00Z");
    updateProof();
    var old =
        new AgentTaskFunctionalSnapshot(
            91390L,
            91063L,
            "pde-communication-sales-journey",
            "communicationContract",
            "communication-director",
            "COMPLETED",
            Instant.EPOCH,
            Instant.EPOCH,
            "{}");
    when(tasks.findFunctionalSnapshots(eq(SOURCE), anyCollection(), isNull()))
        .thenReturn(List.of(old));
    var context = provider.resolve(SOURCE).orElseThrow();
    assertThat(context).containsEntry("inputReadiness", "READY");
    assertThat((Collection<?>) context.get("communicationArtifacts")).isEmpty();
    origin.add(
        origin(
            91999L,
            "marketStrategy",
            "experiment-strategist",
            "marketStrategicContract",
            "marketStrategy"));
    assertThat(provider.resolve(SOURCE).orElseThrow()).containsEntry("inputReadiness", "BLOCKED");
  }

  /**
   * Não transforma referências comerciais, outro produto ou validação antiga em contexto privado.
   */
  @Test
  void rejectsOtherScopesAndUnvalidatedProduct() {
    for (String source :
        List.of("experiment:91092", "product:91010@private-validation-v1", "product:91010"))
      assertThat(provider.resolve(source)).isEmpty();
    product.setValidationDefinitionVersion("PDE_AGENT_VALIDATION_V1");
    assertThat(provider.resolve(SOURCE).orElseThrow()).containsEntry("inputReadiness", "BLOCKED");
  }
}
