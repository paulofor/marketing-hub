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
  private final CreativeVisualEvidenceService service =
      new CreativeVisualEvidenceService(tasks, images, storage, context, json);
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
