package com.marketinghub.opala.commercial.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import com.marketinghub.targeting.TargetingElementStatus;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: fornecer entradas comerciais do Opala com identidade e versão verificadas. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpalaCommercialContext {
  public static final String CODE = "opala-commercial-preparation-v1";
  private final LearningSalesCycleRepository cycles;
  private final ExperimentRepository experiments;
  private final PdeProductionSlotRepository slots;
  private final ExperimentVideoAssetRepository videos;
  private final TargetingElementRepository targeting;
  private final ObjectMapper json;
  private final com.marketinghub.repository.jpa.creative.CreativeRepository creatives;
  private final com.marketinghub.experiment.service.ExperimentTargetingSelectionService selections;

  /** Mantém o ciclo e o experimento como uma única identidade de preparação. */
  public record Scope(LearningSalesCycle cycle, Experiment experiment) {}

  /** Expõe somente a candidata exata e a origem determinística de seu destino e contrato. */
  public record Candidate(
      List<PdeProductionSlot> slots,
      String destinationUrl,
      String destinationSource,
      JsonNode productContract) {}

  /** Recusa fonte genérica, outro tipo, ciclo encerrado e experimento já liberado. */
  public Scope scope(String source) {
    var scope = readScope(source);
    require(
        "OPEN".equals(scope.cycle().getStatus())
            && java.util.Set.of("AUTHORIZATION", "PUBLICATION").contains(scope.cycle().getStage()),
        "O ciclo não está em preparação comercial.");
    require(
        scope.experiment().getStatus() == com.marketinghub.experiment.ExperimentStatus.PLANNED
            && scope.experiment().getFacebookReleaseRequestedAt() == null,
        "A preparação exige experimento planejado e ainda não liberado.");
    return scope;
  }

  /** Valida identidade para leitura do histórico, inclusive após publicação ou encerramento. */
  private Scope readScope(String source) {
    require(
        source != null && source.matches("experiment:[1-9][0-9]{0,17}"),
        "Informe o experimento exato do ciclo Opala.");
    long id = Long.parseLong(source.substring(11));
    var cycle =
        cycles
            .findByExperimentId(id)
            .orElseThrow(() -> new IllegalStateException("Experimento sem ciclo de vendas."));
    var experiment = experiments.findById(id).orElseThrow();
    var product = experiment.getProduct();
    require(
        product != null && Objects.equals(product.getId(), cycle.getProductId()),
        "O experimento pertence a outro produto.");
    require(
        product.getProductTypeDefinition() != null
            && "PDE".equals(product.getProductTypeDefinition().getCode()),
        "O subprocesso exige o tipo cadastrado Opala (PDE).");
    require(
        cycle.getProductVersion() != null && !cycle.getProductVersion().isBlank(),
        "Versão do ciclo ausente.");
    return new Scope(cycle, experiment);
  }

  /** Entrega somente fontes do mesmo produto, nicho e experimento para os agentes. */
  public ObjectNode snapshot(String source) {
    var scope = readScope(source);
    var cycle = scope.cycle();
    var experiment = scope.experiment();
    var product = experiment.getProduct();
    var candidate = candidate(scope);
    var result = json.createObjectNode();
    result.put("contractVersion", "OPALA_COMMERCIAL_PREPARATION_V1");
    result.put("productId", product.getId());
    result.put("experimentId", experiment.getId());
    result.put("cycleId", cycle.getId());
    result.put("productVersion", cycle.getProductVersion());
    result.put("destinationUrl", candidate.destinationUrl());
    result.put("destinationSource", candidate.destinationSource());
    result.put("priceBrl", experiment.getUnitPrice());
    result.put("checkoutUrl", experiment.getCommercialCheckoutUrl());
    result.set("savedAudience", json.valueToTree(selections.list(experiment.getId())));
    result.put("budgetLimitBrl", cycle.getBudgetLimitBrl());
    result.put("windowEnd", Objects.toString(cycle.getWindowEnd(), ""));
    result.set("productContract", candidate.productContract());
    var allowedSlots = result.putArray("slots");
    candidate.slots().stream()
        .forEach(
            s ->
                allowedSlots
                    .addObject()
                    .put("id", s.getId())
                    .put("slotCode", s.getSlotCode())
                    .put("publicUrl", s.getPublicUrl())
                    .put("status", s.getStatus().name())
                    .put("validationStatus", s.getValidationStatus())
                    .put("publishedContract", s.getPublishedExperienceJson()));
    var announcements = result.putArray("creatives");
    creatives.findByExperimentId(experiment.getId()).stream()
        .sorted(java.util.Comparator.comparing(com.marketinghub.creative.Creative::getId))
        .forEach(
            c ->
                announcements
                    .addObject()
                    .put("id", c.getId())
                    .put("version", c.getVersionNumber())
                    .put("status", Objects.toString(c.getStatus(), ""))
                    .put("headline", c.getHeadline())
                    .put("primaryText", c.getPrimaryText())
                    .put("description", c.getDescription())
                    .put("videoUrl", c.getVideoUrl())
                    .put("destinationUrl", c.getDestinationUrl()));
    var media = result.putArray("approvedVideos");
    videos.findByExperimentIdOrderByCreatedAtDesc(experiment.getId()).stream()
        .filter(
            v ->
                v.getReviewStatus()
                        == com.marketinghub.experiment.video.ExperimentVideoReviewStatus.APPROVED
                    && v.getStatus()
                        == com.marketinghub.experiment.video.ExperimentVideoStatus.READY)
        .forEach(
            v ->
                media
                    .addObject()
                    .put("id", v.getId())
                    .put("slot", v.getSlot().name())
                    .put("assetUrl", v.getAssetUrl()));
    var audience = result.putArray("approvedAudienceElements");
    if (experiment.getNiche() != null)
      targeting
          .findByFiltersAndHypothesis(
              experiment.getNiche().getId(),
              null,
              TargetingElementStatus.APPROVED,
              experiment.getHypothesisRef() == null ? null : experiment.getHypothesisRef().getId())
          .stream()
          .filter(t -> t.getMetaId() != null && !t.getMetaId().isBlank())
          .forEach(
              t ->
                  audience
                      .addObject()
                      .put("id", t.getId())
                      .put("term", t.getTerm())
                      .put("type", t.getType().name()));
    result.put("publicationAuthorized", false);
    result.put("mediaSpendAuthorized", false);
    return result;
  }

  /**
   * Resolve a candidata pela identidade completa; nunca escolhe a versão mais recente nem herda o
   * destino de outro experimento.
   */
  public Candidate candidate(Scope scope) {
    var cycle = scope.cycle();
    var experiment = scope.experiment();
    var product = experiment.getProduct();
    var matches =
        slots.findByProductSlugOrderBySlotCodeAsc(product.getSlug()).stream()
            .filter(
                s ->
                    Objects.equals(s.getSourceExperimentId(), experiment.getId())
                        && Objects.equals(s.getExperienceVersion(), cycle.getProductVersion()))
            .toList();
    String explicit = text(experiment.getFollowUpActionUrl());
    String inferred =
        matches.size() == 1 && bindable(matches.getFirst())
            ? text(matches.getFirst().getPublicUrl())
            : null;
    String destination = explicit != null ? explicit : inferred;
    String source =
        explicit != null
            ? "EXPERIMENT"
            : inferred != null
                ? "VERSION_SLOT"
                : matches.size() > 1
                    ? "AMBIGUOUS"
                    : matches.size() == 1 ? "INELIGIBLE_SLOT" : "MISSING";
    String candidateContract = null;
    if (matches.size() == 1) {
      candidateContract = text(matches.getFirst().getDraftExperienceJson());
      if (candidateContract == null)
        candidateContract = text(matches.getFirst().getPublishedExperienceJson());
    }
    if (candidateContract == null) candidateContract = product.getPdeExperienceJson();
    return new Candidate(List.copyOf(matches), destination, source, read(candidateContract));
  }

  /** Aceita somente versões ainda preparáveis ou já publicadas, nunca pausadas ou retiradas. */
  private boolean bindable(PdeProductionSlot slot) {
    return slot.getStatus() != null
        && Set.of(
                PdeProductionSlotStatus.PLANNED,
                PdeProductionSlotStatus.CANDIDATE,
                PdeProductionSlotStatus.READY,
                PdeProductionSlotStatus.ACTIVE)
            .contains(slot.getStatus());
  }

  /** Normaliza texto opcional sem converter ausência em valor utilizável. */
  private String text(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** Lê contratos persistidos sem transformar corrupção em ausência silenciosa. */
  public JsonNode read(String value) {
    try {
      return json.readTree(value == null || value.isBlank() ? "{}" : value);
    } catch (Exception ex) {
      log.error("Contrato inválido na preparação comercial Opala", ex);
      throw new IllegalStateException("Contrato comercial ilegível.", ex);
    }
  }

  /** Mantém falhas de contrato acionáveis e impede avanço sem evidência. */
  public static void require(boolean valid, String reason) {
    if (!valid) throw new IllegalStateException(reason);
  }
}
