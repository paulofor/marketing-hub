package com.marketinghub.communication.v1;

import com.marketinghub.agenttask.CommunicationMaterializationContextProvider;
import com.marketinghub.agenttask.MarketStrategicContextProvider;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.product.Product;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Responsabilidade: alinhar o gate operacional da tela aos contratos exigidos por Íris. */
@Service
@lombok.extern.slf4j.Slf4j
public class IrisProductProcessActivityReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private static final String PROCESS_CODE = "pde-communication-sales-journey";
  private static final String ACTIVITY_ID = "communicationContract";
  private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
      new com.fasterxml.jackson.databind.ObjectMapper();
  private final MarketStrategicContextProvider marketStrategy;
  private final CommunicationMaterializationContextProvider communicationContext;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.repository.jpa.agenttask.AgentTaskRepository tasks;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository
      instances;

  /** Configura os mesmos contextos estratégicos e funcionais entregues ao worker de Íris. */
  public IrisProductProcessActivityReadinessProvider(
      MarketStrategicContextProvider marketStrategy,
      CommunicationMaterializationContextProvider communicationContext) {
    this.marketStrategy = marketStrategy;
    this.communicationContext = communicationContext;
  }

  /** Governa a abertura da comunicação e as atividades de landing executadas por Íris. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return (PROCESS_CODE.equals(process.getProcessCode())
            && ACTIVITY_ID.equals(activityDefinition.getActivityId()))
        || ("creative-production-approval".equals(process.getProcessCode())
            && "route".equals(activityDefinition.getActivityId()))
        || ("landing-page-generation".equals(process.getProcessCode())
            && java.util.Set.of("select", "strategy", "compose", "html")
                .contains(activityDefinition.getActivityId()));
  }

  /**
   * Exige os contratos aprovados do produto privado, ciclo ou plano e antecipa o gate comercial do
   * HTML.
   */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    List<String> missing = new ArrayList<>();
    Map<String, Object> context = communicationContext.resolve(sourceReference).orElse(Map.of());
    boolean approvedPrivateDestination =
        Set.of(IrisLearningCycleContext.MODE, IrisPrivateProductContext.MODE)
            .contains(context.getOrDefault("mode", ""));
    boolean privateStrategy =
        approvedPrivateDestination
            || IrisCommunicationMaterializationContextProvider.INITIAL_EXPERIMENT_PRIVATE_MODE
                .equals(context.getOrDefault("mode", ""));
    boolean landing = process != null && "landing-page-generation".equals(process.getProcessCode());
    if (approvedPrivateDestination && landing) {
      return new AgentProductProcessActivityReadiness(
          false,
          "Esta preparação usa a experiência privada já homologada como destino. Retome o processo de comunicação; uma landing comercial separada não faz parte do contrato aprovado.");
    }
    Map<?, ?> strategy =
        privateStrategy
            ? (context.get("marketStrategicContract") instanceof Map<?, ?> value ? value : Map.of())
            : marketStrategy.resolve(sourceReference).orElse(Map.of());
    if (!"AVAILABLE".equals(strategy.get("availability"))
        || !(privateStrategy ? "MARKET_STRATEGY_V3" : "MARKET_STRATEGY_V2")
            .equals(strategy.get("contractVersion"))
        || !hasText(strategy.get("contentHash"))) {
      missing.add(
          privateStrategy
              ? "Contrato Estratégico de Mercado V3 aprovado na origem deste contexto"
              : "Contrato Estratégico de Mercado v2 concluído de Atena");
    }
    if (!"AVAILABLE".equals(context.get("availability"))) {
      missing.add(
          hasText(context.get("reason"))
              ? String.valueOf(context.get("reason"))
              : "Contexto comercial e funcional disponível");
    } else if (!"READY".equals(context.get("inputReadiness"))) {
      Object predecessors = context.get("missingRequiredPredecessors");
      if (predecessors instanceof Collection<?> values) {
        values.stream().map(String::valueOf).filter(this::hasText).forEach(missing::add);
      }
    }
    if (landing
        && (!(context.get("approvedLandingAssets") instanceof Collection<?> assets)
            || assets.isEmpty())) {
      missing.add("Provas visuais aprovadas e rastreáveis para a landing");
    }
    if (landing && "html".equals(activityDefinition.getActivityId())) {
      if (!hasCommercialCheckout(context)) {
        missing.add("Checkout comercial canônico vinculado ao experimento");
      }
      if (!IrisLandingInstrumentationContract.valid(
          context.get("landingInstrumentationContract"))) {
        missing.add("Contrato canônico de instrumentação da landing");
      }
    }
    List<String> uniqueMissing = missing.stream().distinct().toList();
    if (!uniqueMissing.isEmpty()) {
      return new AgentProductProcessActivityReadiness(
          false, "Antes de executar Íris, conclua: " + String.join("; ", uniqueMissing) + ".");
    }
    return new AgentProductProcessActivityReadiness(
        true, "Estratégia, economia, PDE e provas estão prontos para Íris.");
  }

  /** Reabre a mensagem do produto quando o novo gate ou contrato substitui a prova usada. */
  @Override
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    if ("creative-production-approval".equals(process.getProcessCode())
        && "route".equals(activity.getActivityId())) {
      return creativeRouteInputChanged(activity, reference);
    }
    if (tasks == null || !ACTIVITY_ID.equals(activity.getActivityId())) return false;
    var context = communicationContext.resolve(reference).orElse(Map.of());
    if (IrisCommunicationMaterializationContextProvider.INITIAL_EXPERIMENT_PRIVATE_MODE.equals(
        context.get("mode"))) {
      return initialExperimentInputChanged(process, reference, context);
    }
    if (!IrisPrivateProductContext.supports(reference)) return false;
    var latest =
        tasks.findFunctionalSnapshotsByProcessSince(process.getId(), reference, null).stream()
            .filter(t -> ACTIVITY_ID.equals(t.processActivityId()))
            .max(java.util.Comparator.comparing(t -> t.id()));
    if (latest.isEmpty() || !"COMPLETED".equals(latest.get().status())) return false;
    if (!"READY".equals(context.get("inputReadiness"))) return true;
    Object artifacts = context.get("communicationArtifacts");
    return !(artifacts instanceof Collection<?> values)
        || values.stream()
            .noneMatch(
                value ->
                    value instanceof Map<?, ?> artifact
                        && latest.get().id().equals(artifact.get("taskId")));
  }

  /** Reabre a resolução de formatos quando Íris conclui um contrato de comunicação mais novo. */
  private boolean creativeRouteInputChanged(
      BusinessProcessActivityDefinition activity, String reference) {
    if (tasks == null || instances == null) return false;
    var latestRoute =
        instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), reference);
    if (latestRoute
        .filter(
            instance -> "COMPLETED".equals(instance.getStatus()) && instance.isObjectiveAchieved())
        .isEmpty()) return false;
    var latestCommunication =
        tasks
            .findFunctionalSnapshots(
                reference, java.util.Set.of("pde-communication-sales-journey"), null)
            .stream()
            .filter(
                task ->
                    "communicationContract".equals(task.processActivityId())
                        && "communication-director".equals(task.agentKey()))
            .max(java.util.Comparator.comparing(task -> task.id()));
    if (latestCommunication.isEmpty() || !"COMPLETED".equals(latestCommunication.get().status()))
      return true;
    try {
      long routedTaskId =
          JSON.readTree(latestRoute.orElseThrow().getObjectiveEvidenceJson())
              .path("communicationTaskId")
              .asLong();
      return routedTaskId != latestCommunication.get().id();
    } catch (Exception ex) {
      log.warn(
          "Rota criativa concluída possui prova inválida. activityDefinitionId={} sourceReference={}",
          activity.getId(),
          reference,
          ex);
      return true;
    }
  }

  /** Reabre o contrato quando a versão ou a autorização visual mudou desde a última conclusão. */
  private boolean initialExperimentInputChanged(
      BusinessProcessDefinition process, String reference, Map<String, Object> context) {
    String expectedHash = String.valueOf(context.getOrDefault("communicationInputHash", ""));
    if (!"READY".equals(context.get("inputReadiness")) || !expectedHash.matches("[0-9a-f]{64}"))
      return true;
    var latest =
        tasks.findCompletedActivitySnapshots(
            process.getId(),
            reference,
            ACTIVITY_ID,
            org.springframework.data.domain.PageRequest.of(0, 1));
    if (latest.isEmpty() || latest.get(0).evidenceJson() == null) return true;
    try {
      var usedInput =
          JSON.readTree(latest.get(0).evidenceJson()).path("communicationInputReference");
      String usedHash = usedInput.path("communicationInputHash").asText();
      if (expectedHash.equals(usedHash)) return false;
      return !usedHash.matches("[0-9a-f]{64}")
          || !IrisCommunicationInputFingerprint.equivalent(JSON, context, usedInput);
    } catch (Exception ex) {
      log.warn(
          "Entrada auditada da comunicação inicial está inválida. processDefinitionId={} sourceReference={}",
          process.getId(),
          reference,
          ex);
      return true;
    }
  }

  /** Verifica se um valor de contrato possui texto útil. */
  private boolean hasText(Object value) {
    return value != null && !String.valueOf(value).isBlank();
  }

  /** Exige a URL de pagamento persistida no contrato do experimento antes de solicitar HTML. */
  private boolean hasCommercialCheckout(Map<String, Object> context) {
    Object experiment = context.get("experiment");
    return experiment instanceof Map<?, ?> values && hasText(values.get("checkoutUrl"));
  }
}
