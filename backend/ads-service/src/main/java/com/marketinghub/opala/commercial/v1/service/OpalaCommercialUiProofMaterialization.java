package com.marketinghub.opala.commercial.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.media.*;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.storage.*;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: versionar o anúncio com os pixels atuais do PDE, sem herdar parecer ou aceite.
 */
@Component
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
public class OpalaCommercialUiProofMaterialization {
  private final OpalaCommercialUiProof proofs;
  private final AssetStorageService storage;
  private final AssetRepository assets;
  private final CreativeRepository creatives;
  private final CreativeService creativeService;
  private final ObjectMapper json;

  /** Materializa uma única combinação da tarefa e preserva todas as versões anteriores. */
  public void apply(
      OpalaCommercialContext.Scope scope,
      OpalaCommercialContext.Candidate candidate,
      AgentTask task,
      JsonNode receipt,
      JsonNode instruction) {
    var content = proofs.validate(scope, candidate, task, receipt, instruction);
    var existing = creatives.findByExperimentId(scope.experiment().getId());
    String owned = ownedUrl(existing, task.getId(), content.sha256());
    if (owned != null && existing.stream().anyMatch(c -> Objects.equals(c.getImageUrl(), owned)))
      return;
    var previous =
        existing.stream()
            .filter(c -> "IMAGE".equals(c.getFormat()))
            .max(java.util.Comparator.comparing(Creative::getId))
            .orElse(null);
    var stored = store(scope, task, content);
    var audit =
        json.createObjectNode()
            .put("contractVersion", OpalaCommercialUiProof.CONTRACT)
            .put("productId", scope.cycle().getProductId())
            .put("cycleId", scope.cycle().getId())
            .put("experimentId", scope.experiment().getId())
            .put("productVersion", scope.cycle().getProductVersion())
            .put("sourceTaskId", task.getId())
            .put("sourceArtifactId", content.artifactId())
            .put("contentSha256", content.sha256())
            .put("copySourceTaskId", content.input().path("copySourceTaskId").asLong())
            .put("copySourceArtifactId", content.input().path("copySourceArtifactId").asLong())
            .put("destinationUrl", candidate.destinationUrl())
            .put("incrementalProviderCostUsd", BigDecimal.ZERO)
            .put("commercialApprovalGranted", false);
    audit.set("capture", receipt);
    assets.save(
        Asset.builder()
            .type(AssetType.IMAGE)
            .provider(MediaProvider.USER_UPLOAD)
            .status(AssetStatus.READY)
            .url(stored.publicUrl())
            .externalId("opala-ui-task-" + task.getId())
            .model(OpalaCommercialUiProof.CONTRACT)
            .payload(audit.toString())
            .build());
    var request = new CreateCreativeRequest();
    request.setFormat("IMAGE");
    request.setHeadline(instruction.path("headline").asText());
    request.setPrimaryText(instruction.path("primaryText").asText());
    request.setDescription(instruction.path("description").asText(""));
    request.setImageUrl(stored.publicUrl());
    request.setDestinationUrl(candidate.destinationUrl());
    request.setCta("LEARN_MORE");
    request.setStatus(CreativeStatus.DRAFT);
    request.setCostUsd(BigDecimal.ZERO);
    var creative =
        previous == null
            ? creativeService.create(scope.experiment().getId(), request)
            : creativeService.createVersion(previous.getId(), request);
    creative.setAgentReviewStatus(null);
    creatives.save(creative);
    if (scope.experiment().getCommercialCheckoutUrl() != null)
      creativeService.requestAgentReview(creative.getId());
  }

  /** Armazena somente bytes já conferidos e mantém contexto completo em falhas de integração. */
  private AssetStorageService.StoredObject store(
      OpalaCommercialContext.Scope scope, AgentTask task, OpalaCommercialUiProof.Content content) {
    try {
      return storage.storeBytes(
          content.bytes(),
          "opala-ui-" + task.getId() + "-" + content.sha256() + ".png",
          "image/png",
          new AssetUploadContext(
              AssetUploadCategory.EXPERIMENT_CREATIVE, scope.experiment().getId(), null, null));
    } catch (java.io.IOException | RuntimeException ex) {
      log.error(
          "Falha ao armazenar prova comercial. experimentId={} taskId={} artifactId={}",
          scope.experiment().getId(),
          task.getId(),
          content.artifactId(),
          ex);
      throw new IllegalStateException("Não foi possível armazenar a prova comercial.", ex);
    }
  }

  /**
   * Encontra somente o recibo idempotente desta tarefa e destes pixels, sem reaproveitar outro
   * parecer.
   */
  private String ownedUrl(java.util.List<Creative> candidates, Long taskId, String sha) {
    var urls = candidates.stream().map(Creative::getImageUrl).filter(Objects::nonNull).toList();
    for (var asset : assets.findByUrlIn(urls)) {
      if (!OpalaCommercialUiProof.CONTRACT.equals(asset.getModel())) continue;
      try {
        var receipt = json.readTree(asset.getPayload());
        if (receipt.path("sourceTaskId").asLong() == taskId
            && sha.equals(receipt.path("contentSha256").asText())) return asset.getUrl();
      } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .error(
                "Recibo de prova comercial inválido. assetId={} taskId={}",
                asset.getId(),
                taskId,
                ex);
        throw new IllegalStateException("Recibo comercial inválido.", ex);
      }
    }
    return null;
  }
}
