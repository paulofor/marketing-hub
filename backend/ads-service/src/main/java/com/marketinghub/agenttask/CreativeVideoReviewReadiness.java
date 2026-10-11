package com.marketinghub.agenttask;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reabre a atividade de Psique quando o parecer antigo não cobre os vídeos finais atuais. */
@Component
@RequiredArgsConstructor
public class CreativeVideoReviewReadiness implements AgentProductProcessActivityReadinessProvider {
  private final CreativeFinalVideoEvidence evidence;
  private final AgentTaskTargetContextProvider targets;

  /** Limita a extensão à revisão independente da comunicação, preservando os demais gates. */
  @Override
  public boolean supports(BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return "creative-production-approval".equals(process.getProcessCode()) && "customer".equals(activity.getActivityId());
  }

  /** Acrescenta a responsabilidade audiovisual sem substituir a verificação dos predecessores. */
  @Override
  public AgentProductProcessActivityReadiness readiness(BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity, Product product, String reference) {
    return new AgentProductProcessActivityReadiness(true,
        "Inspecionar as peças e, quando presentes, os frames dos vídeos finais: desejo, clareza, ação, prova legível e progressão visual. Não alegar audição nem resultado comercial.");
  }

  /** Permite nova ocorrência somente quando há arquivo novo sem parecer próprio, sem criar tarefa. */
  @Override
  public boolean requiresFreshExecution(BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity, Product product, String reference) {
    return targets.resolve(reference, process.getProcessCode()).map(evidence::needsReview).orElse(false);
  }
}
