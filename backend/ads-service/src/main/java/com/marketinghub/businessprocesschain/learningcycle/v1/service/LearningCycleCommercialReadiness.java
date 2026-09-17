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
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    boolean exactCandidateReadyForReview = false;
    if (experiment.getExperimentType()
        == com.marketinghub.experiment.ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL) {
      var exactSlots = exactVersionSlots(cycle, experiment);
      var slot = exactSlots.size() == 1 ? exactSlots.getFirst() : null;
      exactCandidateReadyForReview = reviewCandidateReady(experiment, slot);
      requirements.add(
          new Requirement(
              "CURRENT_VERSION_READY",
              "Versão comercial do ciclo",
              exactCandidateReadyForReview,
              exactCandidateReadyForReview
                  ? "A versão "
                      + cycle.getProductVersion()
                      + " está vinculada e validada no preflight do experimento correto."
                  : "Não existe uma candidata única e validada do próprio produto, ciclo e experimento.",
              "Valide a URL da candidata exata antes da revisão; a publicação continua posterior à homologação."));
    }
    if (cycle.getBudgetLimitBrl() == null || cycle.getBudgetLimitBrl().signum() <= 0)
      requirements.add(
          new Requirement(
              "CYCLE_MEDIA_LIMIT",
              "Teto de mídia do ciclo",
              false,
              "O ciclo Meta precisa de um teto positivo antes da autorização.",
              "Defina um limite defensável no planejamento; custo ausente não equivale a zero."));
    boolean candidateReady = exactCandidateReadyForReview;
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
                  if ("LANDING_APPROVED".equals(code) && candidateReady)
                    return new Requirement(
                        item.code(),
                        item.title(),
                        true,
                        "A entrada da versão exata respondeu ao preflight comercial e está disponível para revisão sem antecipar a publicação.",
                        "Execute as revisões de Psique e Têmis sobre a candidata; publique somente depois dos gates e da autorização humana.");
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

  /** Seleciona por identidade completa sem inferir a versão pela atualização mais recente. */
  private List<com.marketinghub.pde.PdeProductionSlot> exactVersionSlots(
      LearningSalesCycle cycle, com.marketinghub.experiment.Experiment experiment) {
    String productSlug = experiment.getProduct().getSlug();
    if (!StringUtils.hasText(productSlug)) return List.of();
    return slots.findByProductSlugOrderBySlotCodeAsc(productSlug).stream()
        .filter(slot -> Objects.equals(productSlug, slot.getProductSlug()))
        .filter(slot -> Objects.equals(cycle.getExperimentId(), slot.getSourceExperimentId()))
        .filter(slot -> Objects.equals(cycle.getProductVersion(), slot.getExperienceVersion()))
        .toList();
  }

  /**
   * Aceita a própria candidata no preflight, preservando publicação e autorização como gates
   * posteriores.
   */
  private boolean reviewCandidateReady(
      com.marketinghub.experiment.Experiment experiment,
      com.marketinghub.pde.PdeProductionSlot slot) {
    if (slot == null
        || !Set.of(
                PdeProductionSlotStatus.CANDIDATE,
                PdeProductionSlotStatus.READY,
                PdeProductionSlotStatus.ACTIVE)
            .contains(slot.getStatus())) return false;
    String reviewContract =
        StringUtils.hasText(slot.getDraftExperienceJson())
            ? slot.getDraftExperienceJson()
            : slot.getPublishedExperienceJson();
    return "OK".equals(slot.getValidationStatus())
        && slot.getValidationCheckedAt() != null
        && Objects.equals(200, slot.getValidationHttpStatus())
        && Objects.equals(experiment.getProduct().getSlug(), slot.getValidationContractSlug())
        && sameUrl(slot.getPublicUrl(), slot.getValidationResolvedUrl())
        && StringUtils.hasText(reviewContract);
  }

  /** Compara a URL validada sem transformar uma barra final em outra identidade comercial. */
  private boolean sameUrl(String expected, String observed) {
    return StringUtils.hasText(expected)
        && StringUtils.hasText(observed)
        && trimTrailingSlash(expected).equals(trimTrailingSlash(observed));
  }

  /** Remove somente barras finais usadas como variação de transporte da mesma URL. */
  private String trimTrailingSlash(String value) {
    String normalized = value.trim();
    while (normalized.endsWith("/") && normalized.length() > 1)
      normalized = normalized.substring(0, normalized.length() - 1);
    return normalized;
  }
}
