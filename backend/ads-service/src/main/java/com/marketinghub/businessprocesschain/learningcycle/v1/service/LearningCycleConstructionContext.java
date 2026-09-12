package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: compor o contrato privado do sucessor com aprovações do seu próprio ciclo. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LearningCycleConstructionContext {
  private final LearningSalesCycleRepository cycles;
  private final BusinessProcessChainDefinitionRepository chains;
  private final AgentTaskRepository tasks;
  private final ObjectMapper mapper;

  @org.springframework.beans.factory.annotation.Autowired
  private LearningCyclePrototypeContext prototypeContext;

  /**
   * Entrega construção, comunicação e criativos do ciclo sem substituir a experiência histórica.
   */
  @Transactional(readOnly = true)
  public Optional<AgentTaskTargetResponse> resolve(
      String reference, Experiment experiment, String processCode) {
    if (experiment == null
        || processCode == null
        || !java.util.Set.of(
                "pde-construction-approval",
                "pde-communication-sales-journey",
                "creative-production-approval")
            .contains(processCode)) return Optional.empty();
    var cycle = cycles.findByExperimentId(experiment.getId()).orElse(null);
    if (cycle == null || (cycle.isBaseline() && !"pde-construction-approval".equals(processCode)))
      return Optional.empty();
    var product = experiment.getProduct();
    if (product == null || !Objects.equals(cycle.getProductId(), product.getId()))
      throw new IllegalStateException("Ciclo e experimento pertencem a produtos diferentes.");
    JsonNode privateContext = context(cycle);
    String privateUrl =
        privateContext == null
            ? null
            : privateContext
                .path("privatePrototypeAcceptance")
                .path("privateAccessUrl")
                .asText(null);
    return Optional.of(
        new AgentTaskTargetResponse(
            reference,
            experiment.getId(),
            product.getId(),
            product.getSlug(),
            product.getName(),
            product.getInternalName(),
            cycle.getProductVersion(),
            privateUrl,
            null,
            null,
            null,
            experiment.getUnitPrice(),
            privateContext));
  }

  /**
   * Publica plano multiagente e linhagem por projeções sem prompts; mantém ausência se as
   * aprovações falharem.
   */
  private JsonNode context(LearningSalesCycle cycle) {
    try {
      if (!"OPEN".equals(cycle.getStatus()) || cycle.isBaseline()) return null;
      var chain = chains.findById(cycle.getChainDefinitionId()).orElseThrow();
      Long planningId =
          chain.getItems().stream()
              .map(item -> item.getProcessDefinition())
              .filter(process -> "pde-commercial-plan-offer".equals(process.getProcessCode()))
              .map(process -> process.getId())
              .findFirst()
              .orElseThrow();
      var approved =
          tasks.findFunctionalSnapshotsByProcessSince(
              planningId, "experiment:" + cycle.getExperimentId(), cycle.getCreatedAt());
      var strategyTask = latest(approved, "marketStrategy", "experiment-strategist");
      var economicsTask = latest(approved, "economics", "financial-agent");
      var architectureTask = latest(approved, "productArchitecture", "landing-generator");
      var strategyResult = mapper.readTree(strategyTask.resultJson());
      var economicsResult = mapper.readTree(economicsTask.resultJson());
      var architectureResult = mapper.readTree(architectureTask.resultJson());
      var strategy = strategyResult.path("marketStrategicContract");
      var economics = economicsResult.path("economics");
      var architecture = architectureResult.path("productArchitecture");
      if (!"APPROVE".equals(strategyResult.path("decision").asText())
          || !"MARKET_STRATEGY_V3".equals(strategy.path("contractVersion").asText())
          || !"READY_FOR_PRIVATE_VALIDATION".equals(strategy.path("status").asText())
          || !"APPROVE".equals(economicsResult.path("decision").asText())
          || !"PDE_PRIVATE_ECONOMICS_V1".equals(economicsResult.path("contractVersion").asText())
          || !economics.isObject()
          || economics.path("commercialSpendAuthorized").asBoolean(true)
          || !"APPROVE".equals(architectureResult.path("decision").asText())
          || !architecture.path("privatePrototype").isObject()
          || !strategy.path("privateValidationPlan").isObject()
          || architectureTask.deliveredAt().isBefore(economicsTask.deliveredAt())
          || economicsTask.deliveredAt().isBefore(strategyTask.deliveredAt()))
        throw new IllegalStateException(
            "Aprovações privadas ausentes, incompatíveis ou desatualizadas.");
      var context = mapper.createObjectNode();
      context.put("contractVersion", "PDE_HARNESS_PLAN_V1");
      context.put("experienceVersion", cycle.getProductVersion());
      context.put("status", "PLANNED");
      try (var input =
          getClass().getResourceAsStream("/contracts/pde-agent-validation-plan-v1.json")) {
        var plan = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(input);
        plan.put("sourceReference", "experiment:" + cycle.getExperimentId());
        context.set("agentValidationPlan", plan);
      }
      if (prototypeContext != null) {
        prototypeContext
            .resolve(cycle)
            .ifPresent(
                acceptance -> {
                  context.set("privatePrototypeAcceptance", acceptance);
                  context.put("status", "PRIVATE_PROTOTYPE_READY");
                  context.set("implementationEvidence", acceptance);
                });
      }
      context
          .putObject("lineage")
          .put("learningCycleId", cycle.getId())
          .put("productId", cycle.getProductId())
          .put("experimentId", cycle.getExperimentId())
          .put("strategyTaskId", strategyTask.id())
          .put("economicsTaskId", economicsTask.id())
          .put("architectureTaskId", architectureTask.id());
      context.set("marketStrategy", strategy);
      context.set("economics", economics);
      context.set("metrics", economicsResult.path("metrics"));
      context.set("harness", architecture);
      context.set("privateValidationPlan", strategy.path("privateValidationPlan"));
      context.set("inheritedLearning", mapper.readTree(cycle.getInheritedLearningJson()));
      context.set("cycleBrief", mapper.readTree(cycle.getBriefJson()));
      context.put(
          "publicationBoundary",
          "Construção privada do sucessor. Sem autorização de contato, publicação, campanha, cobrança ou gasto comercial; preservar a versão histórica.");
      return context;
    } catch (Exception ex) {
      log.error(
          "Contrato privado do ciclo indisponível. cycleId={} productId={} experimentId={}",
          cycle.getId(),
          cycle.getProductId(),
          cycle.getExperimentId(),
          ex);
      return null;
    }
  }

  /**
   * Exige aprovação da tentativa mais recente, sem reaproveitar entrega substituída ou bloqueada.
   */
  private AgentTaskFunctionalSnapshot latest(
      List<AgentTaskFunctionalSnapshot> candidates, String activity, String agent) {
    var task =
        candidates.stream()
            .filter(value -> activity.equals(value.processActivityId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Aprovação ausente: " + activity));
    if (!"COMPLETED".equals(task.status())
        || task.deliveredAt() == null
        || task.agentKey() == null
        || !agent.equals(task.agentKey()))
      throw new IllegalStateException(
          "Última tentativa ainda não é uma aprovação válida: " + activity);
    return task;
  }
}
