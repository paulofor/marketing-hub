package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskCompletionHook;
import com.marketinghub.agenttask.CompleteAgentTaskRequest;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar a reconciliação atômica do runtime antes da homologação PDE. */
class PdeAgentValidationRuntimeAcceptanceHookTest {
  private static final Instant NOW = Instant.parse("2026-09-30T10:17:20Z");
  private final ObjectMapper json = new ObjectMapper();
  private final ProductRepository products = mock(ProductRepository.class);
  private final PdeAgentValidationRuntimeProbe probe = mock(PdeAgentValidationRuntimeProbe.class);
  private final PdeAgentValidationRuntimeAcceptanceHook hook =
      new PdeAgentValidationRuntimeAcceptanceHook(
          products, json, probe, Clock.fixed(NOW, ZoneOffset.UTC));
  private final AgentTask task = new AgentTask();
  private final Product product = new Product();

  /** Prepara um produto planejado cuja aceitação histórica ainda aponta para a versão anterior. */
  @BeforeEach
  void setup() throws Exception {
    var process = new BusinessProcessDefinition();
    process.setProcessCode("pde-construction-approval");
    task.setId(7001L);
    task.setAssignedAgent(Agent.builder().agentKey("landing-generator").build());
    task.setProcessDefinition(process);
    task.setProcessActivityId("access");
    task.setSourceReference("product:11@agent-validation-v1");

    product.setId(11L);
    product.setSlug("pde-planejado-46");
    product.setCommercialStatus("PLANNED");
    product.setValidationDefinitionVersion("PDE_AGENT_VALIDATION_V1");
    product.setValidationDefinitionJson(
        """
        {"privatePrototypeAcceptance":{"status":"READY","prototypeVersion":"alcyone-private-v1","privateAccessUrl":"https://alcyone.example","sourceEvidenceReference":"public-sources-v1"}}
        """);
    product.setPdeExperienceJson(
        """
        {"contractVersion":"PDE_HARNESS_PLAN_V1","privatePrototypeAcceptance":{"status":"READY","prototypeVersion":"alcyone-private-v1","privateAccessUrl":"https://alcyone.example"}}
        """);
    when(products.findLockedById(11L)).thenReturn(Optional.of(product));
    when(probe.probe("https://alcyone.example", 11L, "pde-planejado-46")).thenReturn(identity());
  }

  /** Troca a versão aceita, preserva fontes e grava a prova de implantação do mesmo runtime. */
  @Test
  void reconcilesRuntimeIdentityBeforeCompletingAccess() throws Exception {
    assertThat(hook.supports(task)).isTrue();

    var disposition =
        hook.apply(task, new CompleteAgentTaskRequest("{\"decision\":\"READY\"}", "{}"));

    assertThat(disposition).isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
    JsonNode validation = json.readTree(product.getValidationDefinitionJson());
    JsonNode acceptance = validation.path("privatePrototypeAcceptance");
    assertThat(acceptance.path("prototypeVersion").asText()).isEqualTo("alcyone-private-v2");
    assertThat(acceptance.path("sourceEvidenceReference").asText()).isEqualTo("public-sources-v1");
    assertThat(acceptance.path("acceptanceEvidenceReference").asText())
        .isEqualTo("agent-task:7001");
    assertThat(acceptance.path("runtimeCommitSha").asText()).isEqualTo("a".repeat(40));
    assertThat(acceptance.path("acceptedAt").asText()).isEqualTo(NOW.toString());
    assertThat(validation.path("technicalDeploymentEvidence").path("httpStatus").asInt())
        .isEqualTo(200);
    assertThat(
            validation
                .path("technicalDeploymentEvidence")
                .path("diagnosticSnapshot")
                .path("experienceVersion")
                .asText())
        .isEqualTo("alcyone-private-v2");
    assertThat(
            json.readTree(product.getPdeExperienceJson())
                .path("privatePrototypeAcceptance")
                .path("prototypeVersion")
                .asText())
        .isEqualTo("alcyone-private-v2");
    verify(products).save(product);
  }

  /** Recusa callback sem prontidão e não promove a identidade por simples status técnico. */
  @Test
  void rejectsNonReadyDecision() {
    assertThatThrownBy(
            () ->
                hook.apply(task, new CompleteAgentTaskRequest("{\"decision\":\"BLOCKED\"}", "{}")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("não confirmou");
    verifyNoInteractions(products, probe);
  }

  /** Não disputa callbacks de outros processos, atividades, agentes ou referências. */
  @Test
  void supportsOnlyInitialAgentValidationAccess() {
    assertThat(hook.supports(task)).isTrue();
    task.setProcessActivityId("journey");
    assertThat(hook.supports(task)).isFalse();
    task.setProcessActivityId("access");
    task.setSourceReference("experiment:94");
    assertThat(hook.supports(task)).isFalse();
  }

  /** Monta a identidade que representa o runtime validado pelos dois endpoints públicos. */
  private PdeAgentValidationRuntimeProbe.RuntimeIdentity identity() throws Exception {
    ObjectNode diagnostic = json.createObjectNode();
    diagnostic.put("status", "UP");
    diagnostic.put("experienceVersion", "alcyone-private-v2");
    return new PdeAgentValidationRuntimeProbe.RuntimeIdentity(
        "alcyone-private-v2",
        "a".repeat(40),
        "b".repeat(64),
        "ghcr.io/example/alcyone:" + "a".repeat(40),
        "a".repeat(40),
        Instant.parse("2026-09-30T10:13:06Z"),
        "ALCYONE_AGENT_INTAKE_CONSENT_V1",
        diagnostic);
  }
}
