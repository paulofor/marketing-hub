package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.repository.jpa.agenttask.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Responsabilidade: impedir cruzamento de referências e revisão de briefings sem arquivos reais.
 */
class CreativeVisualEvidenceServiceTest {
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final AgentTaskVisualEvidenceRepository images =
      mock(AgentTaskVisualEvidenceRepository.class);
  private final AgentTaskVisualEvidenceService storage = mock(AgentTaskVisualEvidenceService.class);
  private final CommunicationMaterializationContextProvider context =
      mock(CommunicationMaterializationContextProvider.class);
  private final ObjectMapper json = new ObjectMapper();
  private final CreativeIllustrationSources illustrations = mock(CreativeIllustrationSources.class);
  private final CreativeVisualEvidenceService service =
      new CreativeVisualEvidenceService(tasks, images, storage, context, json, illustrations);
  private AgentTask task;

  /** Configura origem e peça sintéticas em uma referência exclusiva de homologação. */
  @BeforeEach
  void setup() throws Exception {
    var process = new BusinessProcessDefinition();
    process.setId(91064L);
    process.setProcessCode("creative-production-approval");
    task = new AgentTask();
    task.setId(910403L);
    task.setSourceReference("experiment:91092");
    task.setProcessDefinition(process);
    task.setAssignedAgent(Agent.builder().agentKey("communication-director").build());
    task.setProcessActivityId("nonAudiovisual");
    task.setStatus("IN_PROGRESS");
    when(tasks.findById(task.getId())).thenReturn(Optional.of(task));
    var source = image(910395L, 910118L, "FULL_PAGE", "a".repeat(64));
    var rendered = image(task.getId(), 910130L, "CREATIVE_RENDER", "b".repeat(64));
    when(images.findByIdAndTaskId(910118L, 910395L)).thenReturn(Optional.of(source));
    when(images.findByIdAndTaskId(910130L, task.getId())).thenReturn(Optional.of(rendered));
    var proof =
        Map.of(
            "decision",
            "APPROVED",
            "prototypeVersion",
            "sandbox-v12",
            "sourceReference",
            task.getSourceReference(),
            "artifacts",
            List.of(Map.of("artifactId", 910118L, "sha256", "a".repeat(64))));
    when(context.resolve(task.getSourceReference()))
        .thenReturn(
            Optional.of(
                Map.of(
                    "inputReadiness",
                    "READY",
                    "prototypeVersion",
                    "sandbox-v12",
                    "approvedUpstreamArtifacts",
                    List.of(Map.of("taskId", 910395L, "result", proof)))));
  }

  /** Permite apoio ilustrativo da tarefa reservada sem tornar o template um passe comercial. */
  @Test
  void routesIllustrationOnlyForClaimedTaskAndPrivateContext() throws Exception {
    var metadata =
        new CreativeIllustrationSources.Illustration(
            "img-sandbox", "d".repeat(64), 800, 500, "GENERATED_ILLUSTRATION", false);
    when(illustrations.list(any(), eq(task.getSourceReference()))).thenReturn(List.of(metadata));
    when(illustrations.read(any(), eq(task.getSourceReference()), eq("img-sandbox")))
        .thenReturn(new CreativeIllustrationSources.Content(metadata, new byte[] {1, 2, 3}));
    var mvc =
        MockMvcBuilders.standaloneSetup(new InternalCreativeVisualInputController(service)).build();
    String base =
        "/api/internal/agent-tasks/communication-director/stage-executions/910403/visual-inputs/illustrations";
    mvc.perform(get(base)).andExpect(status().isOk());
    mvc.perform(get(base + "/img-sandbox/content")).andExpect(status().isOk());
    mvc.perform(get(base + "/img-foreign/content")).andExpect(status().isConflict());
    var output = json.readTree(result());
    var render =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            output.path("functionalOutput").path("renderedAssets").get(0);
    render
        .put("templateVersion", "EDITORIAL_CARD_V1")
        .put("proofPresentation", "LINKED_FUNCTIONAL_PROOF");
    assertThatThrownBy(() -> service.validateCompletion(task, output.toString()))
        .hasMessageContaining("preparação privada");
    var privateContext = new HashMap<>(context.resolve(task.getSourceReference()).orElseThrow());
    privateContext.put("mode", "PRODUCT_PRIVATE");
    when(context.resolve(task.getSourceReference())).thenReturn(Optional.of(privateContext));
    render.set("illustration", json.valueToTree(metadata));
    when(illustrations.validate(any(), eq(task.getSourceReference()), any())).thenReturn(metadata);
    service.validateCompletion(task, output.toString());
    verify(illustrations)
        .validate(any(), eq(task.getSourceReference()), eq(render.path("illustration")));
    task.setStatus("COMPLETED");
    mvc.perform(get(base)).andExpect(status().isConflict());
  }

  /** Aceita somente PNG final com hash, tarefa e versão da origem aprovada. */
  @Test
  void validatesRealDerivationAndRejectsForeignHashOrVersion() throws Exception {
    service.validateCompletion(task, result());
    assertThatThrownBy(
            () -> service.validateCompletion(task, result().replace("sandbox-v12", "foreign-v11")))
        .hasMessageContaining("outra versão");
    assertThatThrownBy(
            () ->
                service.validateCompletion(task, result().replace("a".repeat(64), "c".repeat(64))))
        .hasMessageContaining("outra versão");
    assertThatThrownBy(() -> service.validateCompletion(task, result().replace("910130", "910131")))
        .hasMessageContaining("não foi persistida");
  }

  /** Reutiliza pixels do gate do produto somente com autorização exata para o experimento. */
  @Test
  void acceptsExplicitReauthorizationOfCurrentProductProof() {
    String proofReference = "product:91010@agent-validation-v1";
    var proof =
        Map.ofEntries(
            Map.entry("contractVersion", "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1"),
            Map.entry("decision", "APPROVED"),
            Map.entry("prototypeVersion", "sandbox-v12"),
            Map.entry("sourceReference", proofReference),
            Map.entry("productId", 91010L),
            Map.entry("publicUrl", "https://example.test/private"),
            Map.entry(
                "artifacts", List.of(Map.of("artifactId", 910118L, "sha256", "a".repeat(64)))));
    Map<String, Object> authorized = new LinkedHashMap<>();
    authorized.put("inputReadiness", "READY");
    authorized.put("prototypeVersion", "sandbox-v12");
    authorized.put("product", Map.of("id", 91010L));
    authorized.put(
        "approvedDestination",
        Map.of("url", "https://example.test/private", "prototypeVersion", "sandbox-v12"));
    authorized.put(
        "visualProofAuthorization",
        Map.of(
            "contractVersion",
            "COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1",
            "proofSourceReference",
            proofReference,
            "targetSourceReference",
            task.getSourceReference(),
            "productId",
            91010L,
            "prototypeVersion",
            "sandbox-v12",
            "gateInstanceId",
            91002L,
            "publicUrl",
            "https://example.test/private"));
    authorized.put("approvedVisualArtifacts", List.of(Map.of("taskId", 910395L, "result", proof)));
    when(context.resolve(task.getSourceReference())).thenReturn(Optional.of(authorized));

    assertThat(service.inputs("communication-director", task.getId())).hasSize(1);
    assertThatCode(() -> service.validateCompletion(task, result())).doesNotThrowAnyException();

    authorized.put(
        "visualProofAuthorization",
        Map.of(
            "contractVersion",
            "COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1",
            "proofSourceReference",
            proofReference,
            "targetSourceReference",
            "experiment:99999",
            "productId",
            91010L,
            "prototypeVersion",
            "sandbox-v12",
            "gateInstanceId",
            91002L,
            "publicUrl",
            "https://example.test/private"));
    assertThatThrownBy(() -> service.inputs("communication-director", task.getId()))
        .hasMessageContaining("outro experimento");

    authorized.put(
        "visualProofAuthorization",
        Map.of(
            "contractVersion",
            "COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1",
            "proofSourceReference",
            proofReference,
            "targetSourceReference",
            task.getSourceReference(),
            "productId",
            99999L,
            "prototypeVersion",
            "sandbox-v12",
            "gateInstanceId",
            91002L,
            "publicUrl",
            "https://example.test/private"));
    assertThatThrownBy(() -> service.inputs("communication-director", task.getId()))
        .hasMessageContaining("outro produto");

    authorized.put(
        "visualProofAuthorization",
        Map.of(
            "contractVersion",
            "COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1",
            "proofSourceReference",
            proofReference,
            "targetSourceReference",
            task.getSourceReference(),
            "productId",
            91010L,
            "prototypeVersion",
            "sandbox-v12",
            "gateInstanceId",
            91002L,
            "publicUrl",
            "https://example.test/foreign"));
    assertThatThrownBy(() -> service.inputs("communication-director", task.getId()))
        .hasMessageContaining("origem e destino");
  }

  /** Acrescenta momentos funcionais em dois produtos sem gravar nem substituir o gate original. */
  @Test
  void addsSupplementalFunctionalPixelsWithoutReplacingAcceptedGate() {
    for (long product : List.of(91010L, 92010L)) {
      task.setSourceReference("experiment:" + (product + 82));
      var authorized = authorizedInput(product);
      when(context.resolve(task.getSourceReference())).thenReturn(Optional.of(authorized));
      var supplemental = supplementalProof(product);
      var source = image(920001L, 920118L, "FULL_PAGE", "c".repeat(64));
      source.setEvidenceKey("ADHERENT-IPHONE_15_PRO-CONTENT");
      source.setDeviceProfile("IPHONE_15_PRO");
      when(images.findByIdAndTaskId(920118L, 920001L)).thenReturn(Optional.of(source));
      useSupplement(supplemental);

      var inputs = service.inputs("communication-director", task.getId());
      assertThat(inputs).hasSize(2);
      assertThat(inputs.getLast().sourceTaskId()).isEqualTo(920001L);
      assertThat(inputs.getLast().evidence().evidenceKey()).endsWith("-CONTENT");
      verify(tasks, never()).save(any());
    }
  }

  /** Recusa origem, versão, gasto, arquivo e gate incompatíveis na prova complementar. */
  @Test
  void refusesSupplementalIdentityDriftAndForeignOrUnpersistedPixels() {
    long product = 91010L;
    var authorized = authorizedInput(product);
    when(context.resolve(task.getSourceReference())).thenReturn(Optional.of(authorized));
    var proof = supplementalProof(product);
    var source = image(920001L, 920118L, "FULL_PAGE", "c".repeat(64));
    when(images.findByIdAndTaskId(920118L, 920001L)).thenReturn(Optional.of(source));
    for (String field :
        List.of(
            "productId",
            "prototypeVersion",
            "sourceReference",
            "publicUrl",
            "providerCalls",
            "trafficClass",
            "captureMomentsContract")) {
      var invalid = proof.deepCopy();
      if (field.equals("productId") || field.equals("providerCalls")) invalid.put(field, 999);
      else invalid.put(field, "foreign");
      useSupplement(invalid);
      assertThatThrownBy(() -> service.inputs("communication-director", task.getId()))
          .hasMessageContaining("captura complementar");
    }
    var paid = proof.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) paid.path("sideEffects"))
        .put("mediaSpendBrl", 1);
    useSupplement(paid);
    assertThatThrownBy(() -> service.inputs("communication-director", task.getId()))
        .hasMessageContaining("captura complementar");
    useSupplement(proof);
    when(images.findByIdAndTaskId(920118L, 920001L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.inputs("communication-director", task.getId()))
        .hasMessageContaining("hash aprovado");
    authorized.put("inputReadiness", "BLOCKED");
    assertThatThrownBy(() -> service.inputs("communication-director", task.getId()))
        .hasMessageContaining("aprovação dos predecessores");
  }

  /** Mantém o bloqueio para briefing sem arquivo e pacote de outra referência. */
  @Test
  void rejectsBriefOnlyAndCrossProductPayload() throws Exception {
    var brief = json.readTree(result());
    ((com.fasterxml.jackson.databind.node.ObjectNode) brief.path("functionalOutput"))
        .remove("renderedAssets");
    assertThatThrownBy(() -> service.validateCompletion(task, brief.toString()))
        .hasMessageContaining("imagens finais");
    assertThatThrownBy(
            () ->
                service.validateCompletion(
                    task, result().replace("experiment:91092", "experiment:99999")))
        .hasMessageContaining("outra referência");
  }

  /** Conserva a fonte original quando outra captura válida atualiza a prova do mesmo destino. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {91010, 92010})
  void deliversOriginalLineageAndRefreshedContinuityWithoutRematerializing(long product)
      throws Exception {
    task.setSourceReference("experiment:" + (product + 82));
    when(context.resolve(task.getSourceReference()))
        .thenReturn(Optional.of(authorizedInput(product)));
    var oldProof = supplementalProof(product);
    var currentProof = oldProof.deepCopy();
    currentProof
        .withArray("artifacts")
        .removeAll()
        .addObject()
        .put("artifactId", 930118L)
        .put("sha256", "d".repeat(64));
    var oldCapture = capture(920001L, oldProof, "COMPLETED");
    var currentCapture = capture(930001L, currentProof, "COMPLETED");
    when(tasks.findFunctionalSnapshots(
            task.getSourceReference() + "@video-proof-v1",
            Set.of("pde-construction-approval"),
            null))
        .thenReturn(List.of(oldCapture, currentCapture));
    var original = image(920001L, 920118L, "FULL_PAGE", "c".repeat(64));
    original.setEvidenceKey("ADHERENT-IPHONE_15_PRO-CONTENT");
    var current = image(930001L, 930118L, "FULL_PAGE", "d".repeat(64));
    current.setEvidenceKey("ADHERENT-DESKTOP-CONTENT");
    when(images.findByIdAndTaskId(920118L, 920001L)).thenReturn(Optional.of(original));
    when(images.findByIdAndTaskId(930118L, 930001L)).thenReturn(Optional.of(current));
    String packageJson =
        result()
            .replace("experiment:91092", task.getSourceReference())
            .replace("910395", "920001")
            .replace("910118", "920118")
            .replace("a".repeat(64), "c".repeat(64));
    when(tasks.findFunctionalSnapshotsByProcessSince(
            task.getProcessDefinition().getId(), task.getSourceReference(), null))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    910403L,
                    91064L,
                    "creative-production-approval",
                    "nonAudiovisual",
                    "communication-director",
                    "COMPLETED",
                    null,
                    null,
                    packageJson)));
    task.setId(910405L);
    task.setAssignedAgent(Agent.builder().agentKey("meta-ad-approver").build());
    task.setProcessActivityId("commercial");
    when(tasks.findById(910405L)).thenReturn(Optional.of(task));

    var inputs = service.inputs("meta-ad-approver", task.getId());
    var proof = json.valueToTree(inputs.getFirst()).path("creativeProof");
    assertThat(inputs).hasSize(1);
    assertThat(proof.path("source").path("evidence").path("id").asLong()).isEqualTo(920118L);
    assertThat(proof.path("currentDestination").path("taskId").asLong()).isEqualTo(930001L);
    assertThat(proof.path("currentDestination").path("captures").size()).isEqualTo(1);
    service.read("meta-ad-approver", task.getId(), 920001L, 920118L);
    service.read("meta-ad-approver", task.getId(), 930001L, 930118L);
    verify(storage).read(920001L, 920118L);
    verify(storage).read(930001L, 930118L);
    assertThatThrownBy(() -> service.read("meta-ad-approver", task.getId(), 999L, 920118L))
        .hasMessageContaining("não pertence");
    task.setAssignedAgent(Agent.builder().agentKey("customer-agent").build());
    task.setProcessActivityId("customer");
    assertThat(
            json.valueToTree(service.inputs("customer-agent", task.getId()).getFirst())
                .path("creativeProof")
                .isMissingNode())
        .isTrue();
    assertThatThrownBy(() -> service.read("customer-agent", task.getId(), 920001L, 920118L))
        .hasMessageContaining("não pertence");
    task.setAssignedAgent(Agent.builder().agentKey("meta-ad-approver").build());
    task.setProcessActivityId("commercial");
    currentProof.put("productId", product + 1);
    when(tasks.findFunctionalSnapshots(
            task.getSourceReference() + "@video-proof-v1",
            Set.of("pde-construction-approval"),
            null))
        .thenReturn(List.of(oldCapture, capture(930001L, currentProof, "COMPLETED")));
    assertThatThrownBy(() -> service.inputs("meta-ad-approver", task.getId()))
        .hasMessageContaining("captura complementar");
    when(tasks.findFunctionalSnapshots(
            task.getSourceReference() + "@video-proof-v1",
            Set.of("pde-construction-approval"),
            null))
        .thenReturn(List.of(oldCapture, capture(930001L, currentProof, "BLOCKED")));
    assertThatThrownBy(() -> service.inputs("meta-ad-approver", task.getId()))
        .hasMessageContaining("captura atual");
    verify(tasks, never()).save(any());
  }

  /** Entrega a homologação principal quando não existe captura complementar, sem inventar fonte. */
  @Test
  void deliversPrimaryDestinationEvidenceWithoutSupplement() throws Exception {
    var source = image(910395L, 910118L, "FULL_PAGE", "a".repeat(64));
    source.setEvidenceKey("ADHERENT-DESKTOP-CONTENT");
    when(images.findByIdAndTaskId(910118L, 910395L)).thenReturn(Optional.of(source));
    when(tasks.findFunctionalSnapshotsByProcessSince(91064L, task.getSourceReference(), null))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    910403L,
                    91064L,
                    "creative-production-approval",
                    "nonAudiovisual",
                    "communication-director",
                    "COMPLETED",
                    null,
                    null,
                    result())));
    task.setId(910405L);
    task.setProcessActivityId("commercial");
    task.setAssignedAgent(Agent.builder().agentKey("meta-ad-approver").build());
    when(tasks.findById(910405L)).thenReturn(Optional.of(task));
    var proof =
        json.valueToTree(service.inputs("meta-ad-approver", task.getId()).getFirst())
            .path("creativeProof");
    assertThat(proof.path("currentDestination").path("taskId").asLong()).isEqualTo(910395L);
    assertThat(proof.path("currentDestination").path("captures").size()).isEqualTo(1);
    service.read("meta-ad-approver", task.getId(), 910395L, 910118L);
    verify(storage).read(910395L, 910118L);
  }

  /** Representa uma captura determinística persistida sem criar gate ou parecer adicional. */
  private AgentTaskFunctionalSnapshot capture(
      long id, com.fasterxml.jackson.databind.JsonNode proof, String status) {
    return new AgentTaskFunctionalSnapshot(
        id,
        91067L,
        "pde-construction-approval",
        "technicalHomologation",
        "customer-agent",
        status,
        null,
        null,
        proof.toString());
  }

  /** Simula o gate original vigente sem alterar seu resultado ou autorizar produção. */
  private Map<String, Object> authorizedInput(long product) {
    String proofReference = "product:" + product + "@agent-validation-v1";
    var proof =
        Map.of(
            "contractVersion",
            "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1",
            "decision",
            "APPROVED",
            "prototypeVersion",
            "sandbox-v12",
            "sourceReference",
            proofReference,
            "productId",
            product,
            "publicUrl",
            "https://example.test/private",
            "artifacts",
            List.of(Map.of("artifactId", 910118L, "sha256", "a".repeat(64))));
    var input = new LinkedHashMap<String, Object>();
    input.put("inputReadiness", "READY");
    input.put("prototypeVersion", "sandbox-v12");
    input.put("product", Map.of("id", product));
    input.put("approvedDestination", Map.of("url", "https://example.test/private"));
    input.put("approvedVisualArtifacts", List.of(Map.of("taskId", 910395L, "result", proof)));
    input.put(
        "visualProofAuthorization",
        Map.of(
            "contractVersion",
            "COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1",
            "proofSourceReference",
            proofReference,
            "targetSourceReference",
            task.getSourceReference(),
            "productId",
            product,
            "prototypeVersion",
            "sandbox-v12",
            "gateInstanceId",
            91002L,
            "publicUrl",
            "https://example.test/private"));
    return input;
  }

  /** Cria relatório determinístico sintético da mesma versão e experimento. */
  private com.fasterxml.jackson.databind.node.ObjectNode supplementalProof(long product) {
    var proof = json.createObjectNode();
    proof.put("contractVersion", "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1");
    proof.put("captureMomentsContract", "PDE_KIT_CAPTURE_MOMENTS_V1");
    proof.put("decision", "APPROVED");
    proof.put("sourceReference", task.getSourceReference() + "@video-proof-v1");
    proof.put("proofSourceReference", "product:" + product + "@agent-validation-v1");
    proof.put("productId", product);
    proof.put("prototypeVersion", "sandbox-v12");
    proof.put("publicUrl", "https://example.test/private");
    proof.put("trafficClass", "AGENT_VALIDATION");
    proof.put("internalMarker", "mh_internal_test");
    proof.put("providerCalls", 0);
    proof.put("humanEvidenceClaimed", false);
    proof.put("commercialEvidenceClaimed", false);
    proof
        .putObject("sideEffects")
        .put("paymentEnabled", false)
        .put("published", false)
        .put("campaignCreated", false)
        .put("mediaSpendBrl", 0);
    proof
        .putArray("artifacts")
        .addObject()
        .put("artifactId", 920118L)
        .put("sha256", "c".repeat(64));
    return proof;
  }

  /** Entrega a captura pelo mesmo contrato de consulta do backend, sem gravação direta. */
  private void useSupplement(com.fasterxml.jackson.databind.JsonNode proof) {
    when(tasks.findFunctionalSnapshots(
            task.getSourceReference() + "@video-proof-v1",
            Set.of("pde-construction-approval"),
            null))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    920001L,
                    91067L,
                    "pde-construction-approval",
                    "technicalHomologation",
                    "customer-agent",
                    "COMPLETED",
                    null,
                    null,
                    proof.toString())));
  }

  /** Recusa bytes de outra tarefa e preserva o conteúdo privado no contrato HTTP. */
  @Test
  void exposesOnlyAuthorizedInputThroughController() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(new InternalCreativeVisualInputController(service)).build();
    mvc.perform(
            get(
                "/api/internal/agent-tasks/communication-director/stage-executions/910403/visual-inputs"))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/internal/agent-tasks/customer-agent/stage-executions/910403/visual-inputs"))
        .andExpect(status().isConflict());
    mvc.perform(
            get(
                "/api/internal/agent-tasks/communication-director/stage-executions/910403/visual-inputs/99999/910118/content"))
        .andExpect(status().isConflict());
    verifyNoInteractions(storage);
  }

  /** Não entrega ao revisor uma peça de uma tentativa anterior substituída por bloqueio. */
  @Test
  void latestBlockedProducerInvalidatesHistoricalRender() {
    task.setProcessActivityId("customer");
    task.getAssignedAgent().setAgentKey("customer-agent");
    when(tasks.findFunctionalSnapshotsByProcessSince(91064L, task.getSourceReference(), null))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    910402L,
                    91064L,
                    "creative-production-approval",
                    "nonAudiovisual",
                    "communication-director",
                    "BLOCKED",
                    null,
                    null,
                    "{}")));
    assertThatThrownBy(() -> service.inputs("customer-agent", task.getId()))
        .hasMessageContaining("ainda não foi concluída");
  }

  /** Monta resultado funcional com linhagem explícita separada da resposta bruta de IA. */
  private String result() {
    return "{\"sourceReference\":\"experiment:91092\",\"functionalOutput\":{\"staticAssets\":[{}],\"renderedAssets\":[{\"artifactId\":910130,\"sha256\":\""
        + "b".repeat(64)
        + "\",\"sourceTaskId\":910395,\"sourceArtifactId\":910118,\"sourceSha256\":\""
        + "a".repeat(64)
        + "\",\"prototypeVersion\":\"sandbox-v12\",\"templateVersion\":\"PROOF_CARD_V1\",\"crop\":{\"x\":0,\"y\":0,\"width\":800,\"height\":500}}]}}";
  }

  /** Cria metadados locais sem S3, produto real ou permissão comercial. */
  private AgentTaskVisualEvidence image(long taskId, long id, String type, String hash) {
    var owner = new AgentTask();
    owner.setId(taskId);
    var image = new AgentTaskVisualEvidence();
    image.setTask(owner);
    image.setId(id);
    image.setEvidenceType(type);
    image.setSha256(hash);
    image.setPageNumber(1);
    return image;
  }
}
