package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleCommercialPreparation;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleCommercialPreparation.Requirement;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: reutilizar o gate canônico para evitar revisão comercial sem insumos no ciclo
 * Meta.
 */
@Component
public class LearningCycleCommercialReadiness {
  private static final List<String> INPUTS =
      List.of(
          "LANDING_APPROVED",
          "CREATIVE_APPROVED",
          "CHECKOUT_READY",
          "INSTRUMENTATION_READY",
          "TARGETING_READY");
  private final ExperimentRepository experiments;
  private final ExperimentReadinessService readiness;
  private final PdeProductionSlotRepository slots;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.marketinghub.opala.commercial.v1.service.OpalaCommercialRouting opalaRouting;

  /** Resolve o gate sob demanda para preservar a composição dos validadores de tarefas. */
  public LearningCycleCommercialReadiness(
      ExperimentRepository experiments,
      @Lazy ExperimentReadinessService readiness,
      PdeProductionSlotRepository slots) {
    this.experiments = experiments;
    this.readiness = readiness;
    this.slots = slots;
  }

  /**
   * Consulta insumos da ocorrência exata; prontidão para revisão não autoriza publicação ou gasto.
   */
  @Transactional(readOnly = true)
  public LearningCycleCommercialPreparation inspect(LearningSalesCycle cycle) {
    var experiment = experiments.findById(cycle.getExperimentId()).orElseThrow();
    if (experiment.getProduct() == null
        || !Objects.equals(experiment.getProduct().getId(), cycle.getProductId()))
      throw new IllegalStateException("A preparação comercial pertence a outro produto.");
    if (experiment.getPlatform() != ExperimentPlatform.FACEBOOK) return null;
    var summary = readiness.summarize(experiment.getId());
    var requirements = new java.util.ArrayList<Requirement>();
    if (experiment.getExperimentType()
        == com.marketinghub.experiment.ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL) {
      var slot =
          slots.findFirstBySourceExperimentIdOrderByUpdatedAtDesc(experiment.getId()).orElse(null);
      boolean versionReady =
          slot != null
              && Objects.equals(experiment.getProduct().getSlug(), slot.getProductSlug())
              && Objects.equals(cycle.getExperimentId(), slot.getSourceExperimentId())
              && Objects.equals(cycle.getProductVersion(), slot.getExperienceVersion())
              && (slot.getStatus() == PdeProductionSlotStatus.READY
                  || slot.getStatus() == PdeProductionSlotStatus.ACTIVE)
              && "OK".equals(slot.getValidationStatus())
              && slot.getPublishedAt() != null
              && slot.getPublishedExperienceJson() != null
              && !slot.getPublishedExperienceJson().isBlank();
      requirements.add(
          new Requirement(
              "CURRENT_VERSION_READY",
              "Versão comercial do ciclo",
              versionReady,
              versionReady
                  ? "A versão "
                      + cycle.getProductVersion()
                      + " está vinculada ao experimento correto."
                  : "Não existe uma versão comercial publicada e validada do próprio produto, ciclo e experimento.",
              "Conclua a publicação pelo fluxo versionado e valide o vínculo desta versão; a homologação privada não a substitui."));
    }
    if (cycle.getBudgetLimitBrl() == null || cycle.getBudgetLimitBrl().signum() <= 0)
      requirements.add(
          new Requirement(
              "CYCLE_MEDIA_LIMIT",
              "Teto de mídia do ciclo",
              false,
              "O ciclo Meta precisa de um teto positivo antes da autorização.",
              "Defina um limite defensável no planejamento; custo ausente não equivale a zero."));
    requirements.addAll(
        INPUTS.stream()
            .map(
                code -> {
                  var matches =
                      summary.runningGateRequirements().stream()
                          .filter(r -> code.equals(r.code()))
                          .toList();
                  if (matches.size() != 1)
                    return new Requirement(
                        code,
                        "Requisito comercial indisponível",
                        false,
                        "O gate não retornou um requisito único: " + code + ".",
                        "Corrija a fonte de prontidão antes de iniciar avaliações.");
                  var item = matches.getFirst();
                  return new Requirement(
                      item.code(),
                      item.title(),
                      item.ready(),
                      item.detail(),
                      item.recommendation());
                })
            .toList());
    boolean ready = requirements.stream().allMatch(Requirement::ready);
    String guidance =
        ready
            ? "Os insumos da jornada estão prontos para revisão. Homologação, preflight, limites financeiros e autorização final continuam obrigatórios."
            : "Prepare a jornada do experimento #"
                + cycle.getExperimentId()
                + " antes de iniciar revisões comerciais pagas. Pendências: "
                + requirements.stream()
                    .filter(r -> !r.ready())
                    .map(Requirement::title)
                    .collect(java.util.stream.Collectors.joining(", "))
                + ".";
    return new LearningCycleCommercialPreparation(
        ready,
        guidance,
        "/experiments/" + cycle.getExperimentId(),
        List.copyOf(requirements),
        opalaRouting == null ? null : opalaRouting.navigation(cycle),
        "Abrir preparação Opala com os agentes",
        experiment.getProduct().getProductTypeDefinition() != null
            && "PDE".equals(experiment.getProduct().getProductTypeDefinition().getCode()));
  }
}
