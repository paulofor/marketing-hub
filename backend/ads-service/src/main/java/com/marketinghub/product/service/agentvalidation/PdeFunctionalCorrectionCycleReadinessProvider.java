package com.marketinghub.product.service.agentvalidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.util.Comparator;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Responsabilidade: exigir sucessor antes de corrigir uma rejeição funcional do ciclo por IA. */
@Service
public class PdeFunctionalCorrectionCycleReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private static final Set<String> REVIEW_ACTIVITIES =
      Set.of(
          "technicalHomologation",
          "psiqueAdherent",
          "psiqueRecovery",
          "psiqueSafety",
          "commercialIntegrityReview");
  private final AgentTaskRepository tasks;
  private final ObjectMapper json;

  /** Recebe a projeção das provas existentes sem executar modelo ou alterar o histórico. */
  public PdeFunctionalCorrectionCycleReadinessProvider(
      AgentTaskRepository tasks, ObjectMapper json) {
    this.tasks = tasks;
    this.json = json;
  }

  /** Compõe somente a atividade condicional de correção no contrato de validação PDE. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return activity != null
        && PdeAgentValidationReworkReadinessProvider.CORRECTION_ACTIVITY.equals(
            activity.getActivityId())
        && PdeAgentValidationProcessContract.supports(process, json);
  }

  /** Bloqueia mudança funcional no experimento imutável e mantém os demais gates independentes. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    if (!supports(process, activity)
        || sourceReference == null
        || !sourceReference.matches("^experiment:[1-9][0-9]*$")) {
      return new AgentProductProcessActivityReadiness(
          true, "A regra de sucessor não altera este contexto.");
    }
    var latest =
        tasks
            .findPdeValidationTaskSnapshots(
                sourceReference, PdeAgentValidationGateActivityExecutor.PROCESS_CODE)
            .stream()
            .filter(
                task ->
                    "BLOCKED".equals(task.status())
                        && REVIEW_ACTIVITIES.contains(task.processActivityId()))
            .max(Comparator.comparing(PdeValidationTaskSnapshot::id));
    if (latest.filter(task -> "FUNCTIONAL_ADJUSTMENT".equals(task.blockerCategory())).isPresent()) {
      return new AgentProductProcessActivityReadiness(
          false,
          "Este ciclo preserva a versão rejeitada no parecer #"
              + latest.orElseThrow().id()
              + ". Prepare um ciclo e experimento sucessores vinculados para corrigir a experiência. A implementação e suas revisões devem ocorrer na nova candidata; não abra outra chamada de correção na referência atual.");
    }
    return new AgentProductProcessActivityReadiness(
        true,
        "A ausência de rejeição funcional atual não dispensa os demais pré-requisitos da correção.");
  }
}
