package com.marketinghub.opala.commercial.v1.service;

import static com.marketinghub.opala.commercial.v1.service.OpalaCommercialContext.require;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.creative.service.video.VideoCreativeRequest;
import com.marketinghub.creative.service.video.VideoCreativeService;
import com.marketinghub.experiment.dto.SaveExperimentTargetingSelectionsRequest;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import com.marketinghub.experiment.service.ExperimentTargetingSelectionService;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElementStatus;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Responsabilidade: materializar instruções de preparação sem publicar nem aprovar ativos. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpalaCommercialMaterialization {
  private static final Set<PdeProductionSlotStatus> PREPARABLE_SLOT_STATUSES =
      Set.of(
          PdeProductionSlotStatus.PLANNED,
          PdeProductionSlotStatus.CANDIDATE,
          PdeProductionSlotStatus.READY,
          PdeProductionSlotStatus.ACTIVE);

  private final OpalaCommercialContext context;
  private final PdeProductionSlotRepository slots;
  private final PdeProductionSlotService slotService;
  private final VideoCreativeService creatives;
  private final com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository
      videos;
  private final OpalaCommercialVersionContract versionContract;
  private final ExperimentRepository experiments;
  private final ExperimentTargetingSelectionService selections;
  private final TargetingElementRepository targeting;
  private final OpalaPrivateCreativeMaterialization privateCreatives;

  /** Aplica somente operações internas explicitamente previstas pelo contrato da atividade. */
  public void apply(String activity, OpalaCommercialContext.Scope scope, JsonNode instruction) {
    switch (activity) {
      case "entry" -> entry(scope);
      case "creative" -> creative(scope, instruction);
      case "checkout" -> checkout(scope);
      case "targeting" -> audience(scope, instruction);
      default -> throw new IllegalArgumentException("Atividade de preparação desconhecida.");
    }
  }

  /**
   * Reconfere os vínculos materializados para reabrir a atividade certa quando perderem validade.
   */
  public boolean current(
      String activity, OpalaCommercialContext.Scope scope, JsonNode instruction) {
    var snapshot = context.snapshot("experiment:" + scope.experiment().getId());
    return switch (activity) {
      case "entry" ->
          snapshot.path("slots").size() == 1
              && Objects.equals(
                  snapshot.path("slots").get(0).path("publicUrl").asText(),
                  scope.experiment().getFollowUpActionUrl());
      case "checkout" -> currentCheckout(scope);
      case "targeting" ->
          !snapshot.path("savedAudience").isEmpty()
              && snapshot.path("approvedAudienceElements").findValues("id").stream()
                  .map(JsonNode::asLong)
                  .collect(java.util.stream.Collectors.toSet())
                  .containsAll(
                      snapshot.path("savedAudience").findValues("targetingElementId").stream()
                          .map(JsonNode::asLong)
                          .toList())
              && snapshot.path("savedAudience").findValues("targetingElementId").stream()
                  .map(JsonNode::asLong)
                  .collect(java.util.stream.Collectors.toSet())
                  .equals(
                      java.util.stream.StreamSupport.stream(
                              instruction.path("targetingElementIds").spliterator(), false)
                          .map(JsonNode::asLong)
                          .collect(java.util.stream.Collectors.toSet()));
      case "creative" -> {
        if (instruction.path("videoAssetId").isNull())
          yield staticCreative(snapshot, scope, instruction).isPresent();
        String media = null;
        for (var video : snapshot.path("approvedVideos"))
          if (video.path("id").asLong() == instruction.path("videoAssetId").asLong())
            media = video.path("assetUrl").asText();
        boolean found = false;
        for (var creative : snapshot.path("creatives"))
          if (media != null
              && media.equals(creative.path("videoUrl").asText())
              && instruction
                  .path("headline")
                  .asText()
                  .trim()
                  .equals(creative.path("headline").asText())
              && instruction
                  .path("primaryText")
                  .asText()
                  .trim()
                  .equals(creative.path("primaryText").asText())
              && instruction
                  .path("description")
                  .asText("")
                  .trim()
                  .equals(creative.path("description").asText(""))
              && Objects.equals(
                  scope.experiment().getFollowUpActionUrl(),
                  creative.path("destinationUrl").asText())) found = true;
        yield found;
      }
      default -> false;
    };
  }

  /** Prepara o destino e, quando ausente, o anúncio pendente derivado da peça privada aceita. */
  private void entry(OpalaCommercialContext.Scope scope) {
    var cycle = scope.cycle();
    var experiment = scope.experiment();
    var product = experiment.getProduct();
    var candidate = context.candidate(scope);
    var contract = candidate.productContract();
    require(
        cycle.getProductVersion().equals(contract.path("experienceVersion").asText()),
        "O contrato PDE precisa corresponder à versão exata do ciclo antes de preparar a entrada.");
    String destination = candidate.destinationUrl();
    URI uri = URI.create(destination == null ? "" : destination);
    require(
        "https".equals(uri.getScheme())
            && uri.getHost() != null
            && uri.getUserInfo() == null
            && uri.getFragment() == null,
        "Configure o destino HTTPS aprovado do próprio PDE.");
    var existing = candidate.slots();
    require(
        existing.size() <= 1,
        "Há mais de um slot para esta versão e experimento; resolva a ambiguidade.");
    if (!existing.isEmpty()) {
      require(
          PREPARABLE_SLOT_STATUSES.contains(existing.getFirst().getStatus()),
          "O slot exato está pausado ou retirado e não pode receber uma nova preparação.");
      require(
          Objects.equals(destination, existing.getFirst().getPublicUrl()),
          "O destino do slot diverge do experimento.");
      if (experiment.getFollowUpActionUrl() == null
          || experiment.getFollowUpActionUrl().isBlank()) {
        experiment.setFollowUpActionUrl(destination);
        experiments.save(experiment);
      }
      privateCreatives.prepare(scope, destination);
      return;
    }
    String code = "opala-e" + experiment.getId() + "-c" + cycle.getId();
    require(
        slots.findByProductSlugAndSlotCode(product.getSlug(), code).isEmpty(),
        "O código do slot já pertence a outra versão; preserve o histórico.");
    slotService.saveProductionSlot(
        product.getSlug(),
        experiment.getId(),
        new PostDeployPdeProductionSlotRequestDto(
            code,
            product.getSlug(),
            uri.getHost(),
            destination,
            null,
            cycle.getProductVersion(),
            contract.path("layoutKey").asText(null),
            "production",
            PdeProductionSlotStatus.PLANNED,
            experiment.getId(),
            "Preparado pelo subprocesso Opala; publicação e homologação pendentes.",
            contract.toString(),
            null));
    privateCreatives.prepare(scope, destination);
  }

  /** Reutiliza a peça estática exata ou cria o rascunho do vídeo, sem conceder aprovação. */
  private void creative(OpalaCommercialContext.Scope scope, JsonNode instruction) {
    if (instruction.path("videoAssetId").isNull()) {
      require(
          staticCreative(
                  context.snapshot("experiment:" + scope.experiment().getId()), scope, instruction)
              .isPresent(),
          "Selecione uma única peça IMAGE vigente deste experimento, com imagem HTTPS, destino e copy exatos; a revisão comercial continua obrigatória.");
      return;
    }
    long videoId = instruction.path("videoAssetId").asLong();
    require(
        context
            .snapshot("experiment:" + scope.experiment().getId())
            .path("approvedVideos")
            .findValues("id")
            .stream()
            .anyMatch(id -> id.asLong() == videoId),
        "O vídeo precisa estar aprovado neste experimento.");
    String headline = instruction.path("headline").asText();
    String text = instruction.path("primaryText").asText();
    require(
        !headline.isBlank() && headline.length() <= 255 && !text.isBlank() && text.length() <= 5000,
        "Copy do anúncio inválida.");
    String description = instruction.path("description").asText("");
    require(description.length() <= 255, "Descrição do anúncio muito longa.");
    var video = videos.findById(videoId).orElseThrow();
    require(
        video.getExperiment() != null
            && Objects.equals(video.getExperiment().getId(), scope.experiment().getId()),
        "Vídeo de outro experimento.");
    String tenant =
        video.getSalesVideoProfile() != null
            ? video.getSalesVideoProfile().getTenantId()
            : video.getSalesVideoJob() != null ? video.getSalesVideoJob().getTenantId() : "default";
    var original = com.marketinghub.salesvideo.tenant.TenantContextHolder.getContext();
    try {
      com.marketinghub.salesvideo.tenant.TenantContextHolder.assertTenant(tenant);
      com.marketinghub.salesvideo.tenant.TenantContextHolder.set(
          new com.marketinghub.salesvideo.tenant.TenantContext(
              tenant, "opala-preparation@marketinghub.internal", false));
      creatives.create(
          scope.experiment().getId(),
          videoId,
          new VideoCreativeRequest(headline, text, description, null));
    } finally {
      com.marketinghub.salesvideo.tenant.TenantContextHolder.set(original);
    }
  }

  /**
   * Identifica a peça estática por conteúdo exato, recusando ambiguidade e versões substituídas.
   */
  private java.util.Optional<JsonNode> staticCreative(
      JsonNode snapshot, OpalaCommercialContext.Scope scope, JsonNode instruction) {
    var matches = new java.util.ArrayList<JsonNode>();
    var replaced =
        snapshot.path("creatives").findValues("sourceCreativeId").stream()
            .filter(JsonNode::isIntegralNumber)
            .map(JsonNode::asLong)
            .collect(java.util.stream.Collectors.toSet());
    for (var creative : snapshot.path("creatives")) {
      if (!"IMAGE".equals(creative.path("format").asText())
          || !creative.path("id").isIntegralNumber()
          || creative.path("id").asLong() <= 0
          || replaced.contains(creative.path("id").asLong())
          || !Set.of("DRAFT", "READY").contains(creative.path("status").asText())
          || !(Set.of("PENDING", "PROCESSING", "APPROVED")
                  .contains(creative.path("agentReviewStatus").asText())
              || ("DRAFT".equals(creative.path("status").asText())
                  && creative.path("agentReviewStatus").asText().isBlank()))
          || !creative.path("videoUrl").asText("").isBlank()
          || !secureImage(creative.path("imageUrl").asText(""))
          || scope.experiment().getFollowUpActionUrl() == null
          || !scope
              .experiment()
              .getFollowUpActionUrl()
              .equals(creative.path("destinationUrl").asText())
          || !sameCopy(instruction, creative)) continue;
      matches.add(creative);
    }
    return matches.size() == 1
        ? java.util.Optional.of(matches.getFirst())
        : java.util.Optional.empty();
  }

  /** Exige copy integral já persistida; preparar a peça não reescreve promessa nem oferta. */
  private boolean sameCopy(JsonNode instruction, JsonNode creative) {
    String headline = instruction.path("headline").asText("").trim();
    String text = instruction.path("primaryText").asText("").trim();
    String description = instruction.path("description").asText("").trim();
    return !headline.isBlank()
        && headline.length() <= 255
        && !text.isBlank()
        && text.length() <= 5000
        && description.length() <= 255
        && headline.equals(creative.path("headline").asText())
        && text.equals(creative.path("primaryText").asText())
        && description.equals(creative.path("description").asText(""));
  }

  /** Aceita somente URL absoluta HTTPS sem credencial embutida para a imagem existente. */
  private boolean secureImage(String image) {
    try {
      URI uri = URI.create(image);
      return "https".equals(uri.getScheme())
          && uri.getHost() != null
          && uri.getUserInfo() == null
          && uri.getFragment() == null;
    } catch (IllegalArgumentException ex) {
      // A mensagem de validação de URI pode conter assinatura ou credencial da URL recebida.
      log.warn(
          "Imagem inválida na preparação estática Opala. validationType={}",
          ex.getClass().getSimpleName());
      return false;
    }
  }

  /** Vincula o checkout canônico e libera a revisão do rascunho, sem cobrança ou autoaprovação. */
  private void checkout(OpalaCommercialContext.Scope scope) {
    var experiment = scope.experiment();
    var canonical = versionContract.resolve(scope, context.candidate(scope)).checkout();
    experiment.setCommercialCheckoutUrl(canonical.checkoutUrl());
    experiments.save(experiment);
    privateCreatives.requestReview(scope);
  }

  /** Revalida checkout e acesso da candidata antes de reaproveitar uma conclusão anterior. */
  private boolean currentCheckout(OpalaCommercialContext.Scope scope) {
    try {
      var canonical = versionContract.resolve(scope, context.candidate(scope)).checkout();
      return Objects.equals(canonical.checkoutUrl(), scope.experiment().getCommercialCheckoutUrl())
          && canonical.priceBrl().compareTo(scope.experiment().getUnitPrice()) == 0;
    } catch (RuntimeException ex) {
      log.warn(
          "Checkout candidato deixou de ser atual. productId={} experimentId={} cycleId={}",
          scope.cycle().getProductId(),
          scope.experiment().getId(),
          scope.cycle().getId(),
          ex);
      return false;
    }
  }

  /** Preserva escolhas existentes e aceita somente elementos aprovados do contexto oferecido. */
  private void audience(OpalaCommercialContext.Scope scope, JsonNode instruction) {
    long experimentId = scope.experiment().getId();
    var requested = new java.util.LinkedHashSet<Long>();
    instruction
        .path("targetingElementIds")
        .forEach(
            id -> {
              require(
                  id.isIntegralNumber() && id.asLong() > 0, "Identificador de público inválido.");
              requested.add(id.asLong());
            });
    require(!requested.isEmpty(), "Escolha elementos aprovados compatíveis com a estratégia.");
    var offered =
        context
            .snapshot("experiment:" + experimentId)
            .path("approvedAudienceElements")
            .findValues("id")
            .stream()
            .map(JsonNode::asLong)
            .collect(java.util.stream.Collectors.toSet());
    require(
        offered.containsAll(requested),
        "O público contém elementos fora da estratégia/nicho/hipótese aprovados.");
    var existing = selections.list(experimentId);
    if (!existing.isEmpty()) {
      require(
          existing.stream()
              .map(s -> s.targetingElementId())
              .collect(java.util.stream.Collectors.toSet())
              .equals(requested),
          "Preserve o público já salvo; mudar a segmentação exige revisão da estratégia.");
      return;
    }
    var request = new SaveExperimentTargetingSelectionsRequest();
    for (long id : requested) {
      var element = targeting.findById(id).orElseThrow();
      require(
          element.getStatus() == TargetingElementStatus.APPROVED,
          "O elemento perdeu sua aprovação.");
      var item = new SaveExperimentTargetingSelectionsRequest.Item();
      item.setTargetingElementId(id);
      item.setTerm(element.getTerm());
      item.setCandidateType(
          switch (element.getType()) {
            case INTEREST -> TargetingCandidateType.INTEREST;
            case BEHAVIOR -> TargetingCandidateType.BEHAVIOR;
            case JOB_TITLE -> TargetingCandidateType.WORK_POSITION;
          });
      request.getItems().add(item);
    }
    selections.save(experimentId, request);
  }
}
