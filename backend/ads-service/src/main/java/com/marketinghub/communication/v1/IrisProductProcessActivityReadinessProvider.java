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
import org.springframework.stereotype.Service;

/** Responsabilidade: alinhar o gate operacional da tela aos contratos exigidos por Íris. */
@Service
public class IrisProductProcessActivityReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private static final String PROCESS_CODE = "pde-communication-sales-journey";
  private static final String ACTIVITY_ID = "communicationContract";
  private final MarketStrategicContextProvider marketStrategy;
  private final CommunicationMaterializationContextProvider communicationContext;

  /** Configura os mesmos contextos estratégicos e funcionais entregues ao worker de Íris. */
  public IrisProductProcessActivityReadinessProvider(
      MarketStrategicContextProvider marketStrategy,
      CommunicationMaterializationContextProvider communicationContext) {
    this.marketStrategy = marketStrategy;
    this.communicationContext = communicationContext;
  }

  /** Reconhece exclusivamente a abertura do contrato de comunicação no processo canônico. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return PROCESS_CODE.equals(process.getProcessCode())
        && ACTIVITY_ID.equals(activityDefinition.getActivityId());
  }

  /**
   * Recusa a execução antes do modelo quando Atena, Plutus, Dédalo ou suas provas estão ausentes.
   */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    List<String> missing = new ArrayList<>();
    Map<String, Object> strategy = marketStrategy.resolve(sourceReference).orElse(Map.of());
    if (!"AVAILABLE".equals(strategy.get("availability"))
        || !"MARKET_STRATEGY_V2".equals(strategy.get("contractVersion"))
        || !hasText(strategy.get("contentHash"))) {
      missing.add("Contrato Estratégico de Mercado v2 concluído de Atena");
    }
    Map<String, Object> context = communicationContext.resolve(sourceReference).orElse(Map.of());
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
    List<String> uniqueMissing = missing.stream().distinct().toList();
    if (!uniqueMissing.isEmpty()) {
      return new AgentProductProcessActivityReadiness(
          false, "Antes de executar Íris, conclua: " + String.join("; ", uniqueMissing) + ".");
    }
    return new AgentProductProcessActivityReadiness(
        true, "Estratégia, economia, PDE e provas estão prontos para Íris.");
  }

  /** Verifica se um valor de contrato possui texto útil. */
  private boolean hasText(Object value) {
    return value != null && !String.valueOf(value).isBlank();
  }
}
