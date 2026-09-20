package com.marketinghub.quartzo.commercial.v1.service;

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
 * Responsabilidade: impedir revisão paga prematura e invalidar comprovações Quartzo desatualizadas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuartzoCommercialAgentReadiness
    implements AgentProductProcessActivityReadinessProvider {
  private final QuartzoCommercialContext context;
  private final QuartzoCommercialService preparation;
  private final BusinessProcessActivityInstanceRepository instances;

  /** Governa a preparação e seus três pontos de comprovação no pai. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return process != null
        && activity != null
        && (QuartzoCommercialContext.CODE.equals(process.getProcessCode())
            || ("pde-commercial-homologation-activation".equals(process.getProcessCode())
                && process.getVersionNumber() >= 8
                && Set.of(
                        "commercialPreparation",
                        "humanExperienceReview",
                        "commercialIntegrityReview")
                    .contains(activity.getActivityId())));
  }

  /**
   * Libera agentes somente depois das fontes e atividades determinísticas da mesma configuração.
   */
  @Override
  public AgentProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    if (!QuartzoCommercialContext.CODE.equals(process.getProcessCode()))
      return new AgentProductProcessActivityReadiness(
          true, "O executor do pai valida a rota do tipo.");
    try {
      var scope = context.scope(source, product.getId(), true);
      if (QuartzoCommercialService.REVIEWS.contains(activity.getActivityId()))
        preparation.prepared(process, scope, source, context.snapshot(source));
      return new AgentProductProcessActivityReadiness(
          true, "Contrato Quartzo e fontes atuais identificados.");
    } catch (RuntimeException ex) {
      log.warn(
          "Quartzo: revisão bloqueada productId={} source={} activity={}",
          product.getId(),
          source,
          activity.getActivityId(),
          ex);
      return new AgentProductProcessActivityReadiness(false, ex.getMessage());
    }
  }

  /** Mudanças de versão, ativos ou parecer invalidam somente a comprovação afetada e seus gates. */
  @Override
  public boolean requiresFreshExecution(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String source) {
    if (!context.applies(product)) return false;
    var previous =
        instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), source);
    if (previous.isEmpty() || !"COMPLETED".equals(previous.get().getStatus())) return false;
    try {
      if (!QuartzoCommercialContext.CODE.equals(process.getProcessCode()))
        return !preparation.completed(product, source);
      var snapshot = context.snapshot(source);
      if (QuartzoCommercialService.REVIEWS.contains(activity.getActivityId())) {
        preparation.currentReview(process, source, activity.getActivityId(), snapshot);
        return false;
      }
      return !preparation.current(previous.get(), snapshot);
    } catch (RuntimeException ex) {
      log.warn(
          "Quartzo: comprovação anterior exige renovação productId={} source={} activity={}",
          product.getId(),
          source,
          activity.getActivityId(),
          ex);
      return true;
    }
  }
}
