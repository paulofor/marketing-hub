package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.financialagent.FinancialAgentExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.service.CommercialPlanLandingAssetService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar o contexto segregado que o backend entrega à Íris. */
class IrisCommunicationMaterializationContextProviderTest {

  /**
   * Mantém o contrato privado ou seu bloqueio explícito, sem recorrer a um plano de outro regime.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"AVAILABLE", "MISSING"})
  void keepsThePrivateCycleContextWithoutFallingBackToCommercialPlan(String availability) {
    var plans = mock(CommercialPlanRepository.class);
    var cycle = mock(IrisLearningCycleContext.class);
    var provider =
        new IrisCommunicationMaterializationContextProvider(
            plans,
            mock(CommercialPlanVersionService.class),
            mock(CommercialPlanLandingAssetService.class),
            mock(AgentTaskRepository.class),
            mock(FinancialAgentExecutionRepository.class),
            new ObjectMapper());
    org.springframework.test.util.ReflectionTestUtils.setField(provider, "learningCycles", cycle);
    Map<String, Object> result =
        Map.of("availability", availability, "mode", IrisLearningCycleContext.MODE);
    when(cycle.resolve("experiment:92")).thenReturn(Optional.of(result));
    assertThat(provider.resolve("experiment:92").orElseThrow()).isSameAs(result);
    assertThat(provider.experimentId("experiment:92")).isEmpty();
    when(cycle.resolve("experiment:92"))
        .thenReturn(
            Optional.of(
                Map.of(
                    "availability",
                    "AVAILABLE",
                    "inputReadiness",
                    "READY",
                    "experiment",
                    Map.of("id", 92L))));
    assertThat(provider.experimentId("experiment:92")).contains(92L);
    org.mockito.Mockito.verifyNoInteractions(plans);
  }

  /** Consolida plano, produto, provas e predecessores da mesma versão com hash auditável. */
  @Test
  void shouldResolveReadyVersionedJourneyContext() {
    Fixture fixture = fixture(List.of(task(12L, "landing-generator")), true);

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
        .contains("FINANCIAL_AGENT_EXECUTION", "financial-agent", "landing-generator")
        .doesNotContain("another-plan");
    assertThat(context.get("approvedLandingAssets").toString()).contains("assetUrl");
    assertThat(context.get("product").toString())
        .contains("Rigel", "pdeExperience")
        .doesNotContain("address", "legalName");
  }

  /** Expõe lacunas de Plutus e Dédalo sem completar o contrato com dados inferidos. */
  @Test
  void shouldBlockWhenRequiredPredecessorsAreMissing() {
    Fixture fixture = fixture(List.of(task(11L, "experiment-strategist")), false);

    Map<String, Object> context =
        fixture.provider().resolve("commercial-plan:1@v2:journey").orElseThrow();

    assertThat(context).containsEntry("inputReadiness", "BLOCKED");
    assertThat(context.get("missingRequiredPredecessors").toString()).contains("Plutus", "Dédalo");
  }

  /** Não aceita tarefa financeira genérica sem a execução canônica e versionada de Plutus. */
  @Test
  void shouldRejectGenericFinancialTaskAsEconomicEvidence() {
    Fixture fixture =
        fixture(List.of(task(11L, "financial-agent"), task(12L, "landing-generator")), false);

    Map<String, Object> context =
        fixture.provider().resolve("commercial-plan:1@v2:journey").orElseThrow();

    assertThat(context).containsEntry("inputReadiness", "BLOCKED");
    assertThat(context.get("missingRequiredPredecessors").toString())
        .contains("Plutus")
        .doesNotContain("Dédalo");
  }

  /** Recusa silenciosamente outra versão do plano e não mistura seu snapshot. */
  @Test
  void shouldRejectDifferentRequestedPlanVersion() {
    Fixture fixture = fixture(List.of(task(12L, "landing-generator")), true);

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
    Fixture fixture = fixture(List.of(task(12L, "landing-generator")), true);
    when(fixture.plans().findByExperimentReference(88L)).thenReturn(List.of(fixture.plan()));

    Map<String, Object> context = fixture.provider().resolve("experiment:88").orElseThrow();

    assertThat(context).containsEntry("availability", "AVAILABLE");
    assertThat(context.get("experiment").toString()).contains("id=88", "checkoutUrl");
    assertThat(fixture.provider().experimentId("experiment:88")).contains(88L);
  }

  /** Reutiliza estratégia, economia e arquitetura privadas do experimento inicial sem legado. */
  @Test
  void shouldResolveInitialPrivateExperimentPlanning() {
    Fixture fixture = fixture(List.of(), false);
    var privateProducts = mock(IrisPrivateProductContext.class);
    org.springframework.test.util.ReflectionTestUtils.setField(
        fixture.provider(), "privateProducts", privateProducts);
    when(privateProducts.resolve("experiment:88")).thenReturn(Optional.empty());
    when(privateProducts.resolve("product:7@agent-validation-v1"))
        .thenReturn(Optional.of(privateProductProof()));
    when(fixture.plans().findByExperimentReference(88L)).thenReturn(List.of(fixture.plan()));
    when(fixture.tasks().findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:88"))
        .thenReturn(
            List.of(
                privatePlanningTask(496L, "marketStrategy", "experiment-strategist", strategy()),
                privatePlanningTask(498L, "economics", "financial-agent", economics()),
                privatePlanningTask(
                    499L, "productArchitecture", "landing-generator", architecture())));
    when(fixture
            .tasks()
            .findFunctionalSnapshots(
                "experiment:88", java.util.Set.of("pde-commercial-plan-offer"), null))
        .thenReturn(
            List.of(
                snapshot(496L, "marketStrategy", "experiment-strategist", strategy()),
                snapshot(498L, "economics", "financial-agent", economics()),
                snapshot(499L, "productArchitecture", "landing-generator", architecture())));
    when(fixture
            .tasks()
            .findFunctionalSnapshots(
                "experiment:88", java.util.Set.of("pde-communication-sales-journey"), null))
        .thenReturn(List.of(communicationSnapshot(500L)));

    Map<String, Object> context = fixture.provider().resolve("experiment:88").orElseThrow();

    assertThat(context)
        .containsEntry("availability", "AVAILABLE")
        .containsEntry("inputReadiness", "READY")
        .containsEntry(
            "mode", IrisCommunicationMaterializationContextProvider.INITIAL_EXPERIMENT_PRIVATE_MODE)
        .containsEntry("prototypeVersion", "rigel-private-v3")
        .containsEntry("paymentEnabled", false);
    assertThat(context.get("communicationInputHash").toString()).matches("[0-9a-f]{64}");
    assertThat(context.get("approvedDestination").toString())
        .contains("rigel-private-v3", "https://example.test/rigel-private");
    assertThat(context.get("visualProofAuthorization").toString())
        .contains(
            "COMMUNICATION_VISUAL_PROOF_AUTHORIZATION_V1",
            "product:7@agent-validation-v1",
            "experiment:88");
    assertThat(context.get("approvedVisualArtifacts").toString())
        .contains("\"artifactId\":95", "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1");
    assertThat(context.get("communicationArtifacts").toString())
        .contains("taskId=500", "COMMUNICATION_PACKAGE");
    assertThat(context.get("marketStrategicContract").toString())
        .contains("MARKET_STRATEGY_V3", "strategistTaskId=496", "contentHash");
    assertThat(context.get("approvedUpstreamArtifacts").toString())
        .contains("experiment-strategist", "financial-agent", "landing-generator")
        .doesNotContain("FINANCIAL_AGENT_EXECUTION");
  }

  /** Mantém o modo V3 bloqueado quando Plutus ainda não concluiu, sem voltar ao contrato legado. */
  @Test
  void shouldExposeMissingInitialPrivateEconomicsWithoutLegacyFallback() {
    Fixture fixture = fixture(List.of(), true);
    var privateProducts = mock(IrisPrivateProductContext.class);
    org.springframework.test.util.ReflectionTestUtils.setField(
        fixture.provider(), "privateProducts", privateProducts);
    when(privateProducts.resolve("experiment:88")).thenReturn(Optional.empty());
    when(privateProducts.resolve("product:7@agent-validation-v1"))
        .thenReturn(Optional.of(privateProductProof()));
    when(fixture.plans().findByExperimentReference(88L)).thenReturn(List.of(fixture.plan()));
    when(fixture
            .tasks()
            .findFunctionalSnapshots(
                "experiment:88", java.util.Set.of("pde-commercial-plan-offer"), null))
        .thenReturn(
            List.of(
                snapshot(496L, "marketStrategy", "experiment-strategist", strategy()),
                snapshot(499L, "productArchitecture", "landing-generator", architecture())));

    Map<String, Object> context = fixture.provider().resolve("experiment:88").orElseThrow();

    assertThat(context)
        .containsEntry(
            "mode", IrisCommunicationMaterializationContextProvider.INITIAL_EXPERIMENT_PRIVATE_MODE)
        .containsEntry("inputReadiness", "BLOCKED");
    assertThat(context.get("missingRequiredPredecessors").toString()).contains("Plutus");
    assertThat(context.get("marketStrategicContract").toString())
        .contains("MARKET_STRATEGY_V3")
        .doesNotContain("MARKET_STRATEGY_V2");
    assertThat(context.get("approvedUpstreamArtifacts").toString())
        .doesNotContain("FINANCIAL_AGENT_EXECUTION");
  }

  /** Bloqueia o experimento inicial quando a versão vigente não possui pixels homologados. */
  @Test
  void shouldBlockInitialExperimentWithoutCurrentProductProof() {
    Fixture fixture = fixture(List.of(), false);
    when(fixture.plans().findByExperimentReference(88L)).thenReturn(List.of(fixture.plan()));
    when(fixture
            .tasks()
            .findFunctionalSnapshots(
                "experiment:88", java.util.Set.of("pde-commercial-plan-offer"), null))
        .thenReturn(
            List.of(
                snapshot(496L, "marketStrategy", "experiment-strategist", strategy()),
                snapshot(498L, "economics", "financial-agent", economics()),
                snapshot(499L, "productArchitecture", "landing-generator", architecture())));

    Map<String, Object> context = fixture.provider().resolve("experiment:88").orElseThrow();

    assertThat(context).containsEntry("inputReadiness", "BLOCKED");
    assertThat(context.get("missingRequiredPredecessors").toString())
        .contains("Versão vigente e capturas aprovadas");
  }

  /** Monta as dependências e entidades mínimas de um plano Rigel segregado. */
  private Fixture fixture(List<AgentTask> upstream, boolean financialReady) {
    CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
    CommercialPlanVersionService versions = mock(CommercialPlanVersionService.class);
    CommercialPlanLandingAssetService assets = mock(CommercialPlanLandingAssetService.class);
    AgentTaskRepository tasks = mock(AgentTaskRepository.class);
    FinancialAgentExecutionRepository financialExecutions =
        mock(FinancialAgentExecutionRepository.class);
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
    when(financialExecutions.findByCommercialPlanIdOrderByCreatedAtDesc(1L))
        .thenReturn(financialReady ? List.of(financialExecution(plan)) : List.of());
    IrisCommunicationMaterializationContextProvider provider =
        new IrisCommunicationMaterializationContextProvider(
            plans, versions, assets, tasks, financialExecutions, new ObjectMapper());
    return new Fixture(provider, plans, plan, tasks);
  }

  /** Cria uma tarefa privada com a definição compartilhada do planejamento v8. */
  private AgentTask privatePlanningTask(
      Long id, String activityId, String agentKey, String resultJson) {
    AgentTask task = task(id, agentKey);
    task.setProcessActivityId(activityId);
    task.setResultJson(resultJson);
    task.getProcessDefinition().setId(94L);
    task.getProcessDefinition().setProcessCode("pde-commercial-plan-offer");
    task.getProcessDefinition().setVersionNumber(8);
    return task;
  }

  /** Projeta a saída funcional mínima usada pelo provedor sem carregar auditoria técnica. */
  private com.marketinghub.agenttask.AgentTaskFunctionalSnapshot snapshot(
      Long id, String activityId, String agentKey, String resultJson) {
    return new com.marketinghub.agenttask.AgentTaskFunctionalSnapshot(
        id,
        94L,
        "pde-commercial-plan-offer",
        activityId,
        agentKey,
        "COMPLETED",
        Instant.parse("2026-09-25T14:00:00Z"),
        Instant.parse("2026-09-25T14:05:00Z"),
        resultJson);
  }

  /** Projeta a comunicação concluída sem torná-la parte do hash da própria entrada. */
  private com.marketinghub.agenttask.AgentTaskFunctionalSnapshot communicationSnapshot(Long id) {
    return new com.marketinghub.agenttask.AgentTaskFunctionalSnapshot(
        id,
        95L,
        "pde-communication-sales-journey",
        "communicationContract",
        "communication-director",
        "COMPLETED",
        Instant.parse("2026-09-25T14:10:00Z"),
        Instant.parse("2026-09-25T14:15:00Z"),
        "{\"contractVersion\":\"IRIS_COMMUNICATION_V1\",\"outputType\":\"COMMUNICATION_PACKAGE\"}");
  }

  /** Monta o Contrato Estratégico de Mercado V3 aprovado por Atena. */
  private String strategy() {
    return """
        {"decision":"APPROVE","marketStrategicContract":{"contractVersion":"MARKET_STRATEGY_V3","status":"READY_FOR_PRIVATE_VALIDATION","privateValidationPlan":{"minimumIndependentReadings":2}}}
        """;
  }

  /** Monta a economia privada sem autorização de gasto comercial. */
  private String economics() {
    return """
        {"decision":"APPROVE","contractVersion":"PDE_PRIVATE_ECONOMICS_V1","economics":{"commercialSpendAuthorized":false,"maxBudgetBrl":0}}
        """;
  }

  /** Monta a arquitetura privada aprovada por Dédalo. */
  private String architecture() {
    return """
        {"decision":"APPROVE","productArchitecture":{"format":"Wizard progressivo","privatePrototype":{"version":"mira-private-v2"}}}
        """;
  }

  /** Monta a prova V3 aprovada do produto que pode ser reautorizada no experimento inicial. */
  private Map<String, Object> privateProductProof() {
    Map<String, Object> result = new java.util.LinkedHashMap<>();
    result.put("availability", "AVAILABLE");
    result.put("inputReadiness", "READY");
    result.put("mode", IrisPrivateProductContext.MODE);
    result.put("product", Map.of("id", 7L));
    result.put("prototypeVersion", "rigel-private-v3");
    result.put("privatePrototypeAcceptance", Map.of("prototypeVersion", "rigel-private-v3"));
    result.put(
        "approvedDestination",
        Map.of(
            "prototypeVersion", "rigel-private-v3", "url", "https://example.test/rigel-private"));
    result.put("validationGate", Map.of("decision", "APPROVED"));
    result.put("gateInstanceId", 342L);
    result.put("discoveryLineage", Map.of("source", "cycle:70"));
    result.put(
        "approvedUpstreamArtifacts",
        List.of(
            Map.of(
                "taskId",
                371L,
                "activityId",
                "technicalHomologation",
                "result",
                Map.ofEntries(
                    Map.entry("contractVersion", "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1"),
                    Map.entry("decision", "APPROVED"),
                    Map.entry("sourceReference", "product:7@agent-validation-v1"),
                    Map.entry("productId", 7L),
                    Map.entry("prototypeVersion", "rigel-private-v3"),
                    Map.entry("publicUrl", "https://example.test/rigel-private"),
                    Map.entry(
                        "artifacts",
                        List.of(Map.of("artifactId", 95L, "sha256", "a".repeat(64))))))));
    return result;
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

  /** Cria um parecer de Plutus concluído na mesma versão comercial usada por Íris. */
  private FinancialAgentExecution financialExecution(CommercialPlan plan) {
    FinancialAgentExecution execution = new FinancialAgentExecution();
    execution.setId(21L);
    execution.setCommercialPlan(plan);
    execution.setStatus(FinancialAgentExecutionStatus.COMPLETED);
    execution.setAuthorityMode("READ_ONLY_REVENUE_PROJECTION");
    execution.setCommercialPlanVersion(2);
    execution.setFinancialSnapshot("{\"planId\":1,\"version\":2}");
    execution.setReconciliationJson("{\"executiveSummary\":\"Economia válida.\"}");
    execution.setDailyReport("Margem e limites revisados por Plutus.");
    execution.setFinishedAt(Instant.now());
    return execution;
  }

  /** Agrupa o provedor e os mocks necessários para variar a origem do contexto. */
  private record Fixture(
      IrisCommunicationMaterializationContextProvider provider,
      CommercialPlanRepository plans,
      CommercialPlan plan,
      AgentTaskRepository tasks) {}
}
