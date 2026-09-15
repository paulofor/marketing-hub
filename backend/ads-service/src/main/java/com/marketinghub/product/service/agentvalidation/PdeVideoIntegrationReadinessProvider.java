package com.marketinghub.product.service.agentvalidation;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleVideoBinding;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: invalidar somente as revisões anteriores ao conjunto audiovisual integrado. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PdeVideoIntegrationReadinessProvider
    implements AgentProductProcessActivityReadinessProvider {
  private final LearningSalesCycleRepository cycles;
  private final AgentTaskRepository tasks;
  private final LearningCycleVideoBinding binding;

  /** Restringe a política aos revisores da homologação canônica. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && "pde-construction-approval".equals(process.getProcessCode())
        && LearningCycleVideoBinding.REVIEWS.contains(activity.getActivityId());
  }

  /**
   * Mantém as pré-condições dos demais provedores e explica uma mídia revogada sem quebrar a tela.
   */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    try {
      binding.forReference(reference);
      return new AgentProductProcessActivityReadiness(
          true, "A revisão conserva a identidade do conjunto audiovisual atual.");
    } catch (RuntimeException ex) {
      log.error(
          "Integração audiovisual inválida productId={} sourceReference={} activity={}",
          product.getId(),
          reference,
          activity.getActivityId(),
          ex);
      return new AgentProductProcessActivityReadiness(
          false,
          "A mídia, sua aprovação ou o destino mudou. Corrija a integração antes de repetir a homologação.");
    }
  }

  /**
   * Pede nova ocorrência uma única vez após a integração, mantendo falhas atuais sem retry
   * implícito.
   */
  @Override
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    if (!supports(process, activity)
        || reference == null
        || !reference.matches("experiment:[1-9][0-9]{0,17}")) return false;
    var cycle = cycles.findByExperimentId(Long.parseLong(reference.substring(11))).orElse(null);
    if (cycle == null
        || !Objects.equals(product.getId(), cycle.getProductId())
        || binding.receipt(cycle).isEmpty()) return false;
    var task =
        tasks
            .findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
                process.getId(), reference)
            .stream()
            .filter(t -> activity.getActivityId().equals(t.getProcessActivityId()))
            .reduce((a, b) -> b)
            .orElse(null);
    return !binding.currentTask(cycle, task);
  }
}
