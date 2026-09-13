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
public class IrisProductProcessActivityReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private static final String PROCESS_CODE = "pde-communication-sales-journey";
  private static final String ACTIVITY_ID = "communicationContract";
  private final MarketStrategicContextProvider marketStrategy;
  private final CommunicationMaterializationContextProvider communicationContext;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.repository.jpa.agenttask.AgentTaskRepository tasks;

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
        || ("landing-page-generation".equals(process.getProcessCode())
            && java.util.Set.of("select", "strategy", "compose", "html")
                .contains(activityDefinition.getActivityId()));
  }

  /** Exige os contratos aprovados do produto privado, ciclo ou plano, preservando cada regime. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    List<String> missing = new ArrayList<>();
    Map<String, Object> context = communicationContext.resolve(sourceReference).orElse(Map.of());
    boolean cycle =
        Set.of(IrisLearningCycleContext.MODE, IrisPrivateProductContext.MODE)
            .contains(context.getOrDefault("mode", ""));
    boolean landing = process != null && "landing-page-generation".equals(process.getProcessCode());
    if (cycle && landing) {
      return new AgentProductProcessActivityReadiness(
          false,
          "Esta preparação usa a experiência privada já homologada como destino. Retome o processo de comunicação; uma landing comercial separada não faz parte do contrato aprovado.");
    }
    Map<?, ?> strategy =
        cycle
            ? (context.get("marketStrategicContract") instanceof Map<?, ?> value ? value : Map.of())
            : marketStrategy.resolve(sourceReference).orElse(Map.of());
    if (!"AVAILABLE".equals(strategy.get("availability"))
        || !(cycle ? "MARKET_STRATEGY_V3" : "MARKET_STRATEGY_V2")
            .equals(strategy.get("contractVersion"))
        || !hasText(strategy.get("contentHash"))) {
      missing.add(
          cycle
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
    if (tasks == null
        || !IrisPrivateProductContext.supports(reference)
        || !ACTIVITY_ID.equals(activity.getActivityId())) return false;
    var latest =
        tasks.findFunctionalSnapshotsByProcessSince(process.getId(), reference, null).stream()
            .filter(t -> ACTIVITY_ID.equals(t.processActivityId()))
            .max(java.util.Comparator.comparing(t -> t.id()));
    if (latest.isEmpty() || !"COMPLETED".equals(latest.get().status())) return false;
    var context = communicationContext.resolve(reference).orElse(Map.of());
    if (!"READY".equals(context.get("inputReadiness"))) return true;
    Object artifacts = context.get("communicationArtifacts");
    return !(artifacts instanceof Collection<?> values)
        || values.stream()
            .noneMatch(
                value ->
                    value instanceof Map<?, ?> artifact
                        && latest.get().id().equals(artifact.get("taskId")));
  }

  /** Verifica se um valor de contrato possui texto útil. */
  private boolean hasText(Object value) {
    return value != null && !String.valueOf(value).isBlank();
  }
}
