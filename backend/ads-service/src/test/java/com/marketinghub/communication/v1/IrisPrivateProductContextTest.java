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
    when(validator.readiness(process, activity, product, SOURCE))
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

  /** Calcula o hash dos bytes usados pelo contrato de evidência. */
  private String sha(String text) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
  }

  /** Entrega V3, economia, protótipo e provas reais sem fabricar plano, experimento ou ciclo. */
  @Test
  void resolvesProductBeforeExperimentAndExportsWorkerContract() throws Exception {
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
  }

  /** Um novo parecer bloqueado impede consumir a prova aprovada anteriormente. */
  @Test
  void rejectsNewerReviewAndChangedBytes() {
    reviews.add(
        new PdeValidationTaskSnapshot(
            91999L, 91070L, "psiqueSafety", "BLOCKED", null, null, "{}", null));
    assertThat(provider.resolve(SOURCE).orElseThrow()).containsEntry("inputReadiness", "BLOCKED");
    reviews.removeLast();
    ((ObjectNode) proof.path("taskEvidence").get(0)).put("resultSha256", "0".repeat(64));
    updateProof();
    assertThat(provider.resolve(SOURCE).orElseThrow()).containsEntry("inputReadiness", "BLOCKED");
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
