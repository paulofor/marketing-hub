package com.marketinghub.growthoperator;

import com.marketinghub.agenttask.MarketStrategicContextProvider;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.product.Product;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Responsabilidade: alinhar a prontidão exibida na tela ao contrato exigido por Hermes. */
@Service
public class HermesProductProcessActivityReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private static final String PROCESS_CODE = "operacao-otimizacao-experimento";
  private static final Set<String> ACTIVITY_IDS =
      Set.of("task-1", "task-2", "task-3", "task-4", "task-10");
  private static final String CONTRACT_VERSION = "MARKET_STRATEGY_V2";
  private static final String CONTRACT_STATUS = "READY_FOR_OPERATION";
  private static final String OPERATOR_BOUNDARY = "ATENA_DEFINES_STRATEGY_HERMES_OPERATES_GROWTH";
  private final MarketStrategicContextProvider marketStrategy;

  /** Configura a mesma fonte estratégica entregue pelo backend ao executor de Hermes. */
  public HermesProductProcessActivityReadinessProvider(
      MarketStrategicContextProvider marketStrategy) {
    this.marketStrategy = marketStrategy;
  }

  /** Reconhece somente as atividades do processo operacional executadas por Hermes. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return PROCESS_CODE.equals(process.getProcessCode())
        && ACTIVITY_IDS.contains(activityDefinition.getActivityId());
  }

  /** Bloqueia uma nova tentativa enquanto Atena não entregar o contrato aceito pelo worker. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    Map<String, Object> wrapper = marketStrategy.resolve(sourceReference).orElse(Map.of());
    if (!isReady(wrapper)) {
      String detail = text(wrapper.get("reason"));
      String reason =
          "Antes de executar Hermes, solicite no plano comercial, em Operação avançada dos especialistas, um novo parecer de Atena com Contrato Estratégico de Mercado v2 pronto para operação.";
      if (detail != null) reason += " Motivo: " + detail;
      return new AgentProductProcessActivityReadiness(false, reason);
    }
    return new AgentProductProcessActivityReadiness(
        true, "Contrato Estratégico de Mercado v2 íntegro e pronto para Hermes.");
  }

  /** Valida versão, hash, estado e fronteira com os mesmos critérios do executor. */
  private boolean isReady(Map<String, Object> wrapper) {
    if (!(wrapper.get("contract") instanceof Map<?, ?> contract)) return false;
    String hash = text(wrapper.get("contentHash"));
    return "AVAILABLE".equals(wrapper.get("availability"))
        && CONTRACT_VERSION.equals(wrapper.get("contractVersion"))
        && hash != null
        && hash.matches("[0-9a-f]{64}")
        && CONTRACT_VERSION.equals(contract.get("contractVersion"))
        && CONTRACT_STATUS.equals(contract.get("status"))
        && OPERATOR_BOUNDARY.equals(contract.get("operatorBoundary"));
  }

  /** Converte somente texto útil em detalhe seguro para a orientação operacional. */
  private String text(Object value) {
    if (value == null || String.valueOf(value).isBlank()) return null;
    return String.valueOf(value).trim();
  }
}
