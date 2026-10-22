package com.marketinghub.opala.commercial.v1.service;

import static com.marketinghub.opala.commercial.v1.service.OpalaCommercialContext.require;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskVisualEvidenceService;
import com.marketinghub.communication.v1.PrivateCommunicationCreativeProof;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.history.ExperimentHistoryEventContracts.CreateRequest;
import com.marketinghub.experiment.history.ExperimentHistoryEventService;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import java.math.BigDecimal;
import java.net.URI;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: preparar um anúncio pendente com a peça privada exata, sem herdar aprovação
 * comercial.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpalaPrivateCreativeMaterialization {
  private final BusinessProcessActivityInstanceRepository instances;
  private final PrivateCommunicationCreativeProof proofs;
  private final AgentTaskRepository tasks;
  private final AgentTaskVisualEvidenceService visuals;
  private final AssetStorageService storage;
  private final AssetRepository assets;
  private final CreativeRepository creatives;
  private final CreativeService creativeService;
  private final ExperimentHistoryEventService history;
  private final ObjectMapper json;

  /**
   * Reutiliza os pixels aprovados uma única vez; callbacks do backend serializam a materialização.
   */
  public void prepare(OpalaCommercialContext.Scope scope, String destination) {
    var experiment = scope.experiment();
    if (!creatives.findByExperimentId(experiment.getId()).isEmpty()) return;
    String reference = "experiment:" + experiment.getId();
    try {
      require(
          experiment.getStatus() == ExperimentStatus.PLANNED
              && experiment.getFacebookReleaseRequestedAt() == null,
          "O anúncio só pode ser preparado antes da liberação comercial.");
      var calls =
          instances
              .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                  "pde-communication-sales-journey", reference);
      var call =
          calls.stream()
              .filter(i -> "creatives".equals(i.getActivityDefinition().getActivityId()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Falta a passagem criativa privada deste experimento."));
      require(
          "COMPLETED".equals(call.getStatus()) && call.isObjectiveAchieved(),
          "A passagem criativa privada ainda não foi aceita.");
      var proof = proofs.resolve(call, reference, scope.cycle().getProductVersion());
      var renders = proof.path("renderedAssets");
      require(
          renders.isArray() && renders.size() == 1,
          "Selecione uma única peça privada para preparar o anúncio.");
      var render = renders.get(0);
      long producerId = proof.path("producerTaskId").asLong();
      long artifactId = render.path("artifactId").asLong();
      var producer = tasks.findById(producerId).orElseThrow();
      require(
          reference.equals(producer.getSourceReference()), "A peça pertence a outra ocorrência.");
      var copy = json.readTree(producer.getResultJson()).path("functionalOutput").path("copy");
      require(
          !copy.path("headline").asText().isBlank() && !copy.path("primaryText").asText().isBlank(),
          "A peça não contém a copy de Íris.");
      var content = visuals.read(producerId, artifactId);
      String sha =
          HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.bytes()));
      require(
          "image/png".equals(content.contentType()) && sha.equals(render.path("sha256").asText()),
          "Os pixels não correspondem à peça privada revisada.");
      var stored =
          storage.storeBytes(
              content.bytes(),
              "iris-task-" + producerId + "-" + sha + ".png",
              content.contentType(),
              new AssetUploadContext(
                  AssetUploadCategory.EXPERIMENT_CREATIVE, experiment.getId(), null, null));
      var uri = URI.create(stored.publicUrl());
      require(
          "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null,
          "A mídia preparada precisa de armazenamento HTTPS.");
      var audit =
          json.createObjectNode()
              .put("contractVersion", "OPALA_PRIVATE_CREATIVE_DRAFT_V1")
              .put("productId", scope.cycle().getProductId())
              .put("cycleId", scope.cycle().getId())
              .put("experimentId", experiment.getId())
              .put("productVersion", scope.cycle().getProductVersion())
              .put("sourceTaskId", producerId)
              .put("sourceArtifactId", artifactId)
              .put("contentSha256", sha)
              .put("mediaGenerated", false)
              .put("creativeAgentReviewGranted", false)
              .put("creativeHumanApprovalGranted", false)
              .put("campaignPublished", false)
              .put("incrementalProviderCostUsd", BigDecimal.ZERO);
      audit.set("privateCreativeProof", proof);
      assets.save(
          Asset.builder()
              .type(AssetType.IMAGE)
              .provider(MediaProvider.USER_UPLOAD)
              .status(AssetStatus.READY)
              .url(stored.publicUrl())
              .externalId("iris-task-" + producerId + "-artifact-" + artifactId)
              .model("IRIS_EXISTING_RENDER_NO_NEW_GENERATION")
              .payload(audit.toString())
              .build());
      var request = new CreateCreativeRequest();
      request.setFormat("IMAGE");
      request.setHeadline(copy.path("headline").asText());
      request.setPrimaryText(copy.path("primaryText").asText());
      request.setDescription(copy.path("description").asText(""));
      request.setImageUrl(stored.publicUrl());
      request.setDestinationUrl(destination);
      request.setCta("LEARN_MORE");
      request.setStatus(CreativeStatus.DRAFT);
      request.setCostUsd(BigDecimal.ZERO);
      if (experiment.getInstagramAccount() != null)
        request.setInstagramUserId(experiment.getInstagramAccount().getCode());
      var creative = creativeService.create(experiment.getId(), request);
      creative.setAgentReviewStatus(null);
      creatives.save(creative);
      audit.put("creativeId", creative.getId());
      history.create(
          experiment.getId(),
          new CreateRequest(
              "OBSERVACAO",
              "Peça privada preparada como anúncio pendente",
              "Pixels e copy de Íris preservados. A nova combinação com o destino comercial exige revisão independente e decisão humana próprias.",
              audit.toString(),
              "OPALA_PRIVATE_CREATIVE_DRAFT_V1",
              Instant.now()));
    } catch (Exception ex) {
      log.error(
          "Falha ao preparar anúncio Opala. productId={} experimentId={} cycleId={}",
          scope.cycle().getProductId(),
          experiment.getId(),
          scope.cycle().getId(),
          ex);
      throw new IllegalStateException(
          "Não foi possível preparar a peça privada como anúncio pendente.", ex);
    }
  }

  /** Enfileira a revisão independente somente após o backend vincular o checkout da candidata. */
  public void requestReview(OpalaCommercialContext.Scope scope) {
    require(
        scope.experiment().getCommercialCheckoutUrl() != null
            && !scope.experiment().getCommercialCheckoutUrl().isBlank(),
        "Vincule o checkout antes da revisão do anúncio.");
    creatives.findByExperimentId(scope.experiment().getId()).stream()
        .filter(
            c ->
                "IMAGE".equals(c.getFormat())
                    && c.getStatus() == CreativeStatus.DRAFT
                    && c.getAgentReviewStatus() == null)
        .forEach(c -> creativeService.requestAgentReview(c.getId()));
  }
}
