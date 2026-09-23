package com.marketinghub.safira.commercial.v1.service;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.agentactivity.AgentProductProcessActivityReadinessProvider;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: impedir revisão paga prematura e invalidar comprovações Safira desatualizadas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SafiraCommercialAgentReadiness
    implements AgentProductProcessActivityReadinessProvider {
  private final SafiraCommercialContext context;
  private final SafiraCommercialService preparation;
  private final BusinessProcessActivityInstanceRepository instances;

  /** Governa a preparação Safira e os pontos reaproveitados no processo comercial pai. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && (SafiraCommercialContext.CODE.equals(process.getProcessCode())
            || ("pde-commercial-homologation-activation".equals(process.getProcessCode())
                && process.getVersionNumber() >= 9
                && Set.of(
                        "commercialPreparation",
                        "humanExperienceReview",
                        "commercialIntegrityReview")
                    .contains(activity.getActivityId())));
  }

  /** Libera os agentes somente após as fontes determinísticas da mesma candidata comercial. */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    if (!SafiraCommercialContext.CODE.equals(process.getProcessCode()))
      return new AgentProductProcessActivityReadiness(
          true, "O executor do processo pai valida a rota Safira pelo tipo cadastrado.");
    try {
      var scope = context.scope(source, product.getId(), true);
      if (SafiraCommercialService.REVIEWS.contains(activity.getActivityId()))
        preparation.prepared(process, scope, source, context.snapshot(source));
      return new AgentProductProcessActivityReadiness(
          true, "Contrato Safira e fontes comerciais atuais identificados.");
    } catch (RuntimeException ex) {
      log.warn(
          "Safira: revisão bloqueada productId={} source={} activity={}",
          product == null ? null : product.getId(),
          source,
          activity == null ? null : activity.getActivityId(),
          ex);
      return new AgentProductProcessActivityReadiness(false, ex.getMessage());
    }
  }

  /** Invalida apenas a comprovação cuja impressão deixou de representar os ativos atuais. */
  @Override
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    if (!context.applies(product)) return false;
    var previous =
        instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), source);
    if (previous.isEmpty() || !"COMPLETED".equals(previous.get().getStatus())) return false;
    try {
      if (!SafiraCommercialContext.CODE.equals(process.getProcessCode()))
        return !preparation.completed(product, source);
      var snapshot = context.snapshot(source);
      if (SafiraCommercialService.REVIEWS.contains(activity.getActivityId())) {
        preparation.currentReview(process, source, activity.getActivityId(), snapshot);
        return false;
      }
      return !preparation.current(previous.get(), snapshot);
    } catch (RuntimeException ex) {
      log.warn(
          "Safira: comprovação anterior exige renovação productId={} source={} activity={}",
          product == null ? null : product.getId(),
          source,
          activity == null ? null : activity.getActivityId(),
          ex);
      return true;
    }
  }
}
