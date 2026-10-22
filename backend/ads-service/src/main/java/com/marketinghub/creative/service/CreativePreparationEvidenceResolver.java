package com.marketinghub.creative.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.service.reviewcontext.CreativePreparedCheckoutEvidenceDto;
import com.marketinghub.creative.service.reviewcontext.CreativeStaticUiEvidenceDto;
import com.marketinghub.opala.commercial.v1.service.OpalaCommercialUiProof;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskVisualEvidenceRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: comprovar origem da captura própria e o vínculo preparatório do checkout da
 * mesma versão.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreativePreparationEvidenceResolver {
  private final AssetRepository assets;
  private final AgentTaskRepository tasks;
  private final AgentTaskVisualEvidenceRepository artifacts;
  private final LearningSalesCycleRepository cycles;
  private final PdeProductionSlotRepository slots;
  private final PdeCommercialCheckoutContractResolver checkouts;
  private final ObjectMapper json;

  /**
   * Resolve somente capturas identificadas, sem conceder aprovação à peça ou ativação ao
   * experimento.
   */
  public Projection resolve(Creative creative) {
    if (creative == null
        || creative.getExperiment() == null
        || !"IMAGE".equals(creative.getFormat())
        || creative.getImageUrl() == null) return new Projection(null, null);
    var source =
        assets.findByUrlIn(java.util.List.of(creative.getImageUrl())).stream()
            .filter(a -> OpalaCommercialUiProof.CONTRACT.equals(a.getModel()))
            .findFirst()
            .orElse(null);
    if (source == null) return new Projection(null, null);
    try {
      var audit = json.readTree(source.getPayload());
      var capture = audit.path("capture");
      var experiment = creative.getExperiment();
      var cycle = cycles.findByExperimentId(experiment.getId()).orElseThrow();
      var task = tasks.findById(audit.path("sourceTaskId").asLong()).orElseThrow();
      var artifact =
          artifacts
              .findByIdAndTaskId(audit.path("sourceArtifactId").asLong(), task.getId())
              .orElseThrow();
      boolean valid =
          OpalaCommercialUiProof.CONTRACT.equals(audit.path("contractVersion").asText())
              && audit.path("experimentId").asLong() == experiment.getId()
              && audit.path("cycleId").asLong() == cycle.getId()
              && audit.path("productId").asLong() == cycle.getProductId()
              && cycle.getProductVersion().equals(audit.path("productVersion").asText())
              && ("experiment:" + experiment.getId()).equals(task.getSourceReference())
              && task.getAssignedAgent() != null
              && "landing-generator".equals(task.getAssignedAgent().getAgentKey())
              && task.getProcessDefinition() != null
              && com.marketinghub.opala.commercial.v1.service.OpalaCommercialContext.CODE.equals(
                  task.getProcessDefinition().getProcessCode())
              && "COMPLETED".equals(task.getStatus())
              && "creative".equals(task.getProcessActivityId())
              && OpalaCommercialUiProof.CONTRACT.equals(task.getExecutionModelCode())
              && "DETERMINISTIC".equals(task.getExecutionMode())
              && artifact.getSha256().equals(audit.path("contentSha256").asText())
              && artifact.getSha256().equals(capture.path("sha256").asText())
              && Objects.equals(creative.getDestinationUrl(), audit.path("destinationUrl").asText())
              && Objects.equals(artifact.getSourceUrl(), creative.getDestinationUrl())
              && "OWN_PRODUCT_UI".equals(capture.path("sourceType").asText())
              && capture.path("testData").asBoolean()
              && !capture.path("externalImagesIncluded").asBoolean(true);
      if (!valid) return new Projection(null, null);
      var proof =
          new CreativeStaticUiEvidenceDto(
              OpalaCommercialUiProof.CONTRACT,
              "VERIFIED",
              cycle.getProductId(),
              cycle.getId(),
              experiment.getId(),
              cycle.getProductVersion(),
              task.getId(),
              artifact.getId(),
              audit.path("copySourceTaskId").asLong(),
              audit.path("copySourceArtifactId").asLong(),
              creative.getImageUrl(),
              artifact.getSha256(),
              capture.path("rendererVersion").asText(),
              capture.path("rendererSha256").asText(),
              artifact.getSourceUrl(),
              capture.path("inspectedUrl").asText(),
              artifact.getCapturedAt().toString(),
              "OWN_PRODUCT_UI",
              true,
              false,
              false);
      CreativePreparedCheckoutEvidenceDto prepared = null;
      if (experiment.getStatus() == com.marketinghub.experiment.ExperimentStatus.PLANNED
          && experiment.getFacebookReleaseRequestedAt() == null
          && "OPEN".equals(cycle.getStatus())) {
        try {
          var candidates =
              slots.findByProductSlugOrderBySlotCodeAsc(experiment.getProduct().getSlug()).stream()
                  .filter(
                      s ->
                          Objects.equals(s.getSourceExperimentId(), experiment.getId())
                              && cycle.getProductVersion().equals(s.getExperienceVersion())
                              && Objects.equals(s.getPublicUrl(), creative.getDestinationUrl())
                              && java.util.Set.of("CANDIDATE", "READY", "ACTIVE")
                                  .contains(s.getStatus().name()))
                  .toList();
          if (candidates.size() == 1) {
            var slot = candidates.getFirst();
            var contract = json.readTree(slot.getPublishedExperienceJson());
            var checkout = checkouts.resolve(experiment.getProduct(), contract).orElseThrow();
            var binding = contract.path("commercialBinding");
            if (cycle.getProductVersion().equals(contract.path("experienceVersion").asText())
                && binding.path("experimentId").asLong() == experiment.getId()
                && "ONE_TIME".equals(binding.path("billingModel").asText())
                && binding.path("priceBrl").isNumber()
                && experiment.getUnitPrice().compareTo(checkout.priceBrl()) == 0
                && experiment.getUnitPrice().compareTo(binding.path("priceBrl").decimalValue()) == 0
                && Objects.equals(experiment.getCommercialCheckoutUrl(), checkout.checkoutUrl()))
              prepared =
                  new CreativePreparedCheckoutEvidenceDto(
                      "PDE_PREPARED_CHECKOUT_REVIEW_V1",
                      "VERIFIED",
                      "PLANNED_PDE_PREPARATION",
                      cycle.getProductId(),
                      cycle.getId(),
                      experiment.getId(),
                      cycle.getProductVersion(),
                      slot.getId(),
                      slot.getPublicUrl(),
                      checkout.checkoutUrl(),
                      checkout.priceBrl(),
                      "ONE_TIME",
                      false);
          }
        } catch (Exception ex) {
          log.warn(
              "Checkout candidato não conciliado; origem da captura preservada. creativeId={} experimentId={}",
              creative.getId(),
              experiment.getId(),
              ex);
        }
      }
      return new Projection(proof, prepared);
    } catch (Exception ex) {
      log.warn(
          "Prova de preparação comercial não pôde ser conciliada. creativeId={} experimentId={}",
          creative.getId(),
          creative.getExperiment().getId(),
          ex);
      return new Projection(null, null);
    }
  }

  /** Responsabilidade: transportar provas segregadas de pixels e de configuração comercial. */
  public record Projection(
      CreativeStaticUiEvidenceDto staticUiProof,
      CreativePreparedCheckoutEvidenceDto preparedCheckout) {}
}
