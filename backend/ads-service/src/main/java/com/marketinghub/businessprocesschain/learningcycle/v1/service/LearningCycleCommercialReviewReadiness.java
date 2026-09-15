package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: impedir tarefas dos revisores comerciais antes da existência dos insumos
 * avaliáveis.
 */
@Component
@RequiredArgsConstructor
public class LearningCycleCommercialReviewReadiness
    implements AgentProductProcessActivityReadinessProvider {
  private final LearningSalesCycleRepository cycles;
  private final LearningCycleCommercialReadiness readiness;

  /** Aplica-se às atividades de agente do processo comercial, sem alterar a homologação privada. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && "pde-commercial-homologation-activation".equals(process.getProcessCode());
  }

  /** Verifica produto e fonte antes de reutilizar os requisitos determinísticos da tela. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String sourceReference) {
    if (sourceReference == null || !sourceReference.matches("experiment:[1-9][0-9]{0,17}"))
      return new AgentProductProcessActivityReadiness(
          true, "O contexto mantém os gates comerciais próprios.");
    var cycle =
        cycles.findByExperimentId(Long.parseLong(sourceReference.substring(11))).orElse(null);
    if (cycle == null)
      return new AgentProductProcessActivityReadiness(
          true, "Experimento sem ciclo; gates comerciais próprios preservados.");
    if (product == null || !Objects.equals(product.getId(), cycle.getProductId()))
      return new AgentProductProcessActivityReadiness(
          false, "O ciclo comercial pertence a outro produto.");
    var preparation = readiness.inspect(cycle);
    return preparation == null
        ? new AgentProductProcessActivityReadiness(
            true, "O canal mantém seus requisitos próprios de homologação.")
        : new AgentProductProcessActivityReadiness(
            preparation.readyForReview(), preparation.guidance());
  }
}
