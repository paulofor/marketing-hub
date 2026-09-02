package com.marketinghub.product.service.privatevalidation;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.product.Product;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Responsabilidade: preservar a ordem dos agentes durante construção e validação privada do PDE.
 */
@Service
public class PdePrivateValidationAgentReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private static final Set<String> AGENT_ACTIVITIES =
      Set.of(
          "journey",
          "deliverables",
          "audiovisual",
          "access",
          "humanExperienceReview",
          "commercialIntegrityReview");
  private final ProductProcessActivityPredecessorService predecessors;

  /** Configura a fonte canônica que interpreta predecessoras no diagrama publicado. */
  public PdePrivateValidationAgentReadinessProvider(
      ProductProcessActivityPredecessorService predecessors) {
    this.predecessors = predecessors;
  }

  /** Governa somente atividades de agente do processo privado, sem conflitar com gates humanos. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return process != null
        && activityDefinition != null
        && PdePrivateReadingHumanActivityHandler.PROCESS_CODE.equals(process.getProcessCode())
        && AGENT_ACTIVITIES.contains(activityDefinition.getActivityId());
  }

  /** Bloqueia o agente até todas as atividades anteriores atingirem o objetivo. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    ProductProcessActivityPredecessorReadiness readiness =
        predecessors.readiness(process, activityDefinition, sourceReference);
    return new AgentProductProcessActivityReadiness(readiness.ready(), readiness.reason());
  }
}
