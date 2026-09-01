package com.marketinghub.experiment.directcontact.v1;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.product.Product;
import org.springframework.stereotype.Service;

/** Responsabilidade: impedir retentativas de Hermes antes da amostra direta atingir sua meta. */
@Service
public class DirectContactSampleActivityReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private static final String PROCESS_CODE = "operacao-otimizacao-experimento";
  private static final String ACTIVITY_ID = "task-2";
  private static final String EXPERIMENT_PREFIX = "experiment:";
  private final ExperimentDirectContactService directContacts;

  /** Configura o gate com a fonte persistida da amostra. */
  public DirectContactSampleActivityReadinessProvider(
      ExperimentDirectContactService directContacts) {
    this.directContacts = directContacts;
  }

  /** Governa somente a atividade de acumulação do subprocesso de otimização. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activityDefinition) {
    return PROCESS_CODE.equals(process.getProcessCode())
        && ACTIVITY_ID.equals(activityDefinition.getActivityId());
  }

  /** Libera o modelo somente quando a revisão pode concluir com dados reais novos. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activityDefinition,
      Product product,
      String sourceReference) {
    Long experimentId = experimentId(sourceReference);
    if (experimentId == null) {
      return new AgentProductProcessActivityReadiness(
          false, "A atividade aguarda uma referência canônica de experimento.");
    }
    if (!directContacts.isDirectOneToOne(experimentId)) {
      return new AgentProductProcessActivityReadiness(
          true, "O canal do experimento não usa a amostra individual consentida.");
    }
    ExperimentDirectContactSampleResponse sample = directContacts.getSample(experimentId);
    if (sample.targetContacts() <= 0) {
      return new AgentProductProcessActivityReadiness(
          false, "Defina uma amostra direta maior que zero antes de executar Hermes.");
    }
    if (!sample.readyForHermesReview()) {
      return new AgentProductProcessActivityReadiness(
          false,
          "Aguardando amostra direta: %d de %d contatos consentidos e aderentes registrados; faltam %d."
              .formatted(
                  sample.recordedContacts(), sample.targetContacts(), sample.remainingContacts()));
    }
    return new AgentProductProcessActivityReadiness(
        true,
        "A amostra direta atingiu %d de %d contatos e está pronta para revisão de Hermes."
            .formatted(sample.recordedContacts(), sample.targetContacts()));
  }

  /** Extrai somente referências explícitas no formato experiment:<id>. */
  private Long experimentId(String sourceReference) {
    if (sourceReference == null || !sourceReference.matches("experiment:[1-9][0-9]*")) {
      return null;
    }
    return Long.valueOf(sourceReference.substring(EXPERIMENT_PREFIX.length()));
  }
}
