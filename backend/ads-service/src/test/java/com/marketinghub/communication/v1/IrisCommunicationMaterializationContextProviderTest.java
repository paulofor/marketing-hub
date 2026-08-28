package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o contexto segregado que o backend entrega à Íris. */
class IrisCommunicationMaterializationContextProviderTest {

  /** Consolida plano, produto, provas e predecessores da mesma versão com hash auditável. */
  @Test
  void shouldResolveReadyVersionedJourneyContext() {
    Fixture fixture =
        fixture(List.of(task(11L, "financial-agent"), task(12L, "landing-generator")));

    Map<String, Object> context =
        fixture.provider().resolve("commercial-plan:1@v2:journey:attempt-1").orElseThrow();

    assertThat(context)
        .containsEntry("availability", "AVAILABLE")
        .containsEntry("contractVersion", "IRIS_INPUT_V1")
        .containsEntry("commercialPlanId", 1L)
        .containsEntry("commercialPlanVersion", 2)
        .containsEntry("inputReadiness", "READY")
        .containsEntry("publicationAuthorized", false)
        .containsEntry("externalMediaSpendAuthorized", false);
    assertThat(context.get("commercialPlanSnapshotHash").toString()).matches("[0-9a-f]{64}");
    assertThat(context.get("approvedUpstreamArtifacts").toString())
        .contains("financial-agent", "landing-generator")
        .doesNotContain("another-plan");
    assertThat(context.get("approvedLandingAssets").toString()).contains("assetUrl");
    assertThat(context.get("product").toString())
        .contains("Rigel", "pdeExperience")
        .doesNotContain("address", "legalName");
  }

  /** Expõe lacunas de Plutus e Dédalo sem completar o contrato com dados inferidos. */
  @Test
  void shouldBlockWhenRequiredPredecessorsAreMissing() {
    Fixture fixture = fixture(List.of(task(11L, "experiment-strategist")));

    Map<String, Object> context =
        fixture.provider().resolve("commercial-plan:1@v2:journey").orElseThrow();

    assertThat(context).containsEntry("inputReadiness", "BLOCKED");
    assertThat(context.get("missingRequiredPredecessors").toString()).contains("Plutus", "Dédalo");
  }

  /** Recusa silenciosamente outra versão do plano e não mistura seu snapshot. */
  @Test
  void shouldRejectDifferentRequestedPlanVersion() {
    Fixture fixture =
        fixture(List.of(task(11L, "financial-agent"), task(12L, "landing-generator")));

    Map<String, Object> context =
        fixture.provider().resolve("commercial-plan:1@v3:journey").orElseThrow();

    assertThat(context)
        .containsEntry("availability", "MISSING")
        .containsEntry("sourceReference", "commercial-plan:1@v3:journey");
    assertThat(context.get("reason").toString()).contains("versão diferente");
  }

  /** Resolve o experimento somente quando ele pertence ao plano comercial encontrado. */
  @Test
  void shouldResolveOwnedExperimentReference() {
    Fixture fixture =
        fixture(List.of(task(11L, "financial-agent"), task(12L, "landing-generator")));
    when(fixture.plans().findByExperimentReference(88L)).thenReturn(List.of(fixture.plan()));

    Map<String, Object> context = fixture.provider().resolve("experiment:88").orElseThrow();

    assertThat(context).containsEntry("availability", "AVAILABLE");
    assertThat(context.get("experiment").toString()).contains("id=88", "checkoutUrl");
    assertThat(fixture.provider().experimentId("experiment:88")).contains(88L);
  }

  /** Monta as dependências e entidades mínimas de um plano Rigel segregado. */
  private Fixture fixture(List<AgentTask> upstream) {
    CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
    CommercialPlanVersionService versions = mock(CommercialPlanVersionService.class);
    CommercialPlanLandingAssetService assets = mock(CommercialPlanLandingAssetService.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    Product product =
        Product.builder()
            .id(7L)
            .slug("rigel")
            .name("Rigel")
            .internalName("Rigel")
            .productType("PDE")
            .productFormat("WEBAPP")
            .deliveryMode("AUTOMATIC")
            .revenueModel("ONE_TIME")
            .valueUnit("Jornada personalizada")
            .pdeExperienceJson("{\"harness\":\"personalizado\"}")
            .build();
    Experiment experiment =
        Experiment.builder()
            .id(88L)
            .name("Rigel primeira venda")
            .product(product)
            .primaryCta("Começar")
            .commercialCheckoutUrl("https://checkout.example.test/rigel")
            .unitPrice(new java.math.BigDecimal("349.00"))
            .build();
    CommercialPlan plan =
        CommercialPlan.builder().id(1L).name("Agenda Cheia").experiment(experiment).build();
    when(plans.findById(1L)).thenReturn(Optional.of(plan));
    when(versions.current(1L))
        .thenReturn(
            new CommercialPlanVersionDto(
                4L, 1L, 2, "{\"version\":2}", "test", "homologação", Instant.now()));
    when(assets.payloadForExperiment(88L))
        .thenReturn(
            List.of(
                Map.of(
                    "assetId",
                    71L,
                    "assetUrl",
                    "https://assets.example.test/rigel.png",
                    "version",
                    3)));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:1@v2"))
        .thenReturn(upstream);
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:88"))
        .thenReturn(List.of());
    IrisCommunicationMaterializationContextProvider provider =
        new IrisCommunicationMaterializationContextProvider(
            plans, versions, assets, tasks, new ObjectMapper());
    return new Fixture(provider, plans, plan);
  }

  /** Cria um artefato predecessor concluído e atribuído a uma identidade canônica. */
  private AgentTask task(Long id, String agentKey) {
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setStatus("COMPLETED");
    task.setAssignedAgent(Agent.builder().agentKey(agentKey).build());
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("process-" + agentKey);
    task.setProcessDefinition(process);
    task.setProcessActivityId("artifact");
    task.setResultJson("{\"status\":\"APPROVED\"}");
    task.setEvidenceJson("{\"source\":\"test\"}");
    return task;
  }

  /** Agrupa o provedor e os mocks necessários para variar a origem do contexto. */
  private record Fixture(
      IrisCommunicationMaterializationContextProvider provider,
      CommercialPlanRepository plans,
      CommercialPlan plan) {}
}
