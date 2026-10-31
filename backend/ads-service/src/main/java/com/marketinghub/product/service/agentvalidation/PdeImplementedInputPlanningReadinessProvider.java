package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleImplementedInputContext;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleValidationProtocol;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: revalidar planejamento que contradiz a entrada ou o protocolo da candidata. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PdeImplementedInputPlanningReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private static final Pattern EXPERIMENT = Pattern.compile("^experiment:([1-9][0-9]{0,17})$");
  private final LearningSalesCycleRepository cycles;
  private final LearningCycleImplementedInputContext inputs;
  private final AgentTaskRepository tasks;
  private final ObjectMapper json;

  /** Reconhece apenas a sequência multiagente de planejamento, sem alterar contratos legados. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && "pde-commercial-plan-offer".equals(process.getProcessCode())
        && process.getVersionNumber() != null
        && process.getVersionNumber() >= 11
        && Set.of("marketStrategy", "economics", "productArchitecture")
            .contains(activity.getActivityId());
  }

  /** Exige estratégia alinhada e economia corrente antes de encaminhar novamente a arquitetura. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    var state = state(process, activity, product, reference);
    if (state.isPresent() && unchangedFunctionalBlock(state.get(), activity.getActivityId()))
      return new AgentProductProcessActivityReadiness(
          false,
          "A tentativa anterior pediu ajustes e nenhuma entrega predecessora foi corrigida. Resolva a origem indicada antes de repetir; retomar não autoriza gastar novamente com as mesmas entradas.");
    if (state.isEmpty() || "marketStrategy".equals(activity.getActivityId()))
      return new AgentProductProcessActivityReadiness(
          true, "Atena pode preservar a entrada da versão implementada no planejamento.");
    var value = state.orElseThrow();
    if (!aligned(value))
      return new AgentProductProcessActivityReadiness(
          false,
          "Atena precisa alinhar a estratégia à base disponível e ao protocolo da candidata antes desta passagem.");
    if ("productArchitecture".equals(activity.getActivityId()) && !currentEconomics(value))
      return new AgentProductProcessActivityReadiness(
          false, "Plutus precisa avaliar a estratégia corrigida antes da arquitetura de Dédalo.");
    return new AgentProductProcessActivityReadiness(
        true, "Os predecessores preservam a entrada implementada e a ordem dos pareceres.");
  }

  /** Impede repetição funcional por botão ou retomada; falha técnica conserva sua recuperação. */
  private boolean unchangedFunctionalBlock(PlanningState state, String activity) {
    var current = latest(state.history(), activity);
    if (current == null || !"BLOCKED".equals(current.status()) || current.resultJson() == null)
      return false;
    try {
      var result = json.readTree(current.resultJson());
      if (result == null || !Set.of("ADJUST", "REJECT").contains(result.path("decision").asText()))
        return false;
      if (!"marketStrategy".equals(activity) && !aligned(state)) return false;
      List<String> predecessors =
          switch (activity) {
            case "productArchitecture" -> List.of("marketStrategy", "economics");
            case "economics" -> List.of("marketStrategy");
            default -> List.of();
          };
      return predecessors.stream()
          .map(id -> latest(state.history(), id))
          .noneMatch(
              task ->
                  task != null && "COMPLETED".equals(task.status()) && task.id() > current.id());
    } catch (IOException ex) {
      log.error(
          "Falha ao conferir bloqueio funcional taskId={} cycleId={}",
          current.id(),
          state.cycle().getId(),
          ex);
      return true;
    }
  }

  /** Revalida parecer dependente da entrada antiga, sem repetir bloqueio atual ou tarefa ativa. */
  @Override
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    var state = state(process, activity, product, reference);
    if (state.isEmpty()) return false;
    var value = state.orElseThrow();
    var current = latest(value.history(), activity.getActivityId());
    if (current == null || !Set.of("COMPLETED", "BLOCKED").contains(current.status())) return false;
    if ("marketStrategy".equals(activity.getActivityId()))
      return "COMPLETED".equals(current.status()) && !aligned(value);
    if (!aligned(value)) return true;
    var strategy = latest(value.history(), "marketStrategy");
    if ("economics".equals(activity.getActivityId())) return current.id() < strategy.id();
    var economics = latest(value.history(), "economics");
    return !currentEconomics(value)
        || current.id() < strategy.id()
        || current.id() < economics.id();
  }

  /**
   * Lê somente o ciclo aberto, produto e definição exatos, com projeção funcional sem auditoria.
   */
  private Optional<PlanningState> state(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    if (!supports(process, activity) || product == null || reference == null)
      return Optional.empty();
    var match = EXPERIMENT.matcher(reference);
    if (!match.matches()) return Optional.empty();
    var cycle =
        cycles
            .findByExperimentId(Long.valueOf(match.group(1)))
            .filter(value -> product.getId().equals(value.getProductId()))
            .filter(
                value -> "OPEN".equals(value.getStatus()) && "PLANNING".equals(value.getStage()));
    return cycle.map(
        value -> {
          var input = inputs.resolve(value).orElse(null);
          return new PlanningState(
              value,
              input == null ? null : input.path("minimumCustomerInput").asText(),
              LearningCycleValidationProtocol.resolve(json, value, input),
              tasks.findFunctionalSnapshotsByProcessSince(
                  process.getId(), reference, value.getCreatedAt()));
        });
  }

  /** Confere a entrada quando implementada e recusa comparação sem capacidade da candidata. */
  private boolean aligned(PlanningState state) {
    var strategy = latest(state.history(), "marketStrategy");
    if (strategy == null || !"COMPLETED".equals(strategy.status())) return false;
    if (strategy.resultJson() == null || strategy.resultJson().isBlank()) return false;
    try {
      var result = json.readTree(strategy.resultJson());
      return result != null
          && "APPROVE".equals(result.path("decision").asText())
          && !LearningCycleValidationProtocol.hasUnscopedComparison(result, state.protocol())
          && (state.minimumInput() == null
              || state
                  .minimumInput()
                  .equals(
                      result
                          .path("marketStrategicContract")
                          .path("agentValidationPlan")
                          .path("customerValueDelivery")
                          .path("minimumCustomerInput")
                          .asText()));
    } catch (IOException ex) {
      log.error(
          "Falha ao conferir planejamento taskId={} productId={} cycleId={}",
          strategy.id(),
          state.cycle().getProductId(),
          state.cycle().getId(),
          ex);
      return false;
    }
  }

  /** Confere a economia concluída após a estratégia que realmente será usada por Dédalo. */
  private boolean currentEconomics(PlanningState state) {
    var strategy = latest(state.history(), "marketStrategy");
    var economics = latest(state.history(), "economics");
    return strategy != null
        && economics != null
        && "COMPLETED".equals(economics.status())
        && economics.id() > strategy.id();
  }

  /** Seleciona a última tentativa da atividade, inclusive pendência ou falha, sem herdar outra. */
  private AgentTaskFunctionalSnapshot latest(
      List<AgentTaskFunctionalSnapshot> history, String activity) {
    return history.stream()
        .filter(task -> activity.equals(task.processActivityId()))
        .max(Comparator.comparing(AgentTaskFunctionalSnapshot::id))
        .orElse(null);
  }

  /** Responsabilidade: agrupar a entrada e as provas do contexto exato durante uma leitura. */
  private record PlanningState(
      LearningSalesCycle cycle,
      String minimumInput,
      JsonNode protocol,
      List<AgentTaskFunctionalSnapshot> history) {}
}
