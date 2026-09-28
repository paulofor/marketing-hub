package com.marketinghub.creative.service.visual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.service.CreativePublicationCopyPolicy;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.history.ExperimentHistoryEventContracts.CreateRequest;
import com.marketinghub.experiment.history.ExperimentHistoryEventService;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Promove imagens aprovadas do plano a controles estáticos sem recriar mídia nem pular gates. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovedVisualAssetCreativeService {
  private final ExperimentRepository experiments;
  private final CommercialPlanRepository plans;
  private final CommercialPlanVisualAssetRepository assets;
  private final CreativeRepository creatives;
  private final CreativeService creativeService;
  private final ExperimentHistoryEventService history;
  private final ObjectMapper objectMapper;

  /**
   * Lista somente imagens do plano com integridade e dois pareceres aprovados para o experimento.
   */
  @Transactional(readOnly = true)
  public List<ApprovedVisualAssetOption> listEligible(Long experimentId) {
    requireExperiment(experimentId);
    Map<Long, ApprovedVisualAssetOption> eligible = new LinkedHashMap<>();
    plans.findByExperimentReference(experimentId).stream().findFirst().stream()
        .flatMap(
            plan ->
                assets
                    .findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
                        plan.getId(), CommercialPlanVisualAssetStatus.APPROVED)
                    .stream())
        .filter(this::isEligible)
        .map(this::toOption)
        .forEach(option -> eligible.putIfAbsent(option.id(), option));
    return List.copyOf(eligible.values());
  }

  /** Cria uma única peça pendente usando os pixels e o SHA já aprovados no plano vigente. */
  @Transactional
  public Creative create(
      Long experimentId, Long assetId, ApprovedVisualAssetCreativeRequest request) {
    try {
      Experiment experiment = requireExperimentForSelection(experimentId);
      if (experiment.getStatus() != ExperimentStatus.PLANNED
          || experiment.getFacebookReleaseRequestedAt() != null) {
        throw error(HttpStatus.CONFLICT, "O experimento já foi liberado ou não está planejado.");
      }
      requirePublicationCopy(request);
      if (experiment.getInstagramAccount() == null) {
        throw error(
            HttpStatus.CONFLICT,
            "Cadastre a identidade oficial do Instagram antes de selecionar o controle estático.");
      }
      requireHttps(experiment.getFollowUpActionUrl(), "Configure o destino HTTPS do experimento.");
      CommercialPlanVisualAsset asset =
          assets
              .findByIdForUpdate(assetId)
              .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Ativo visual não encontrado."));
      validateBelongsToExperiment(experimentId, asset);
      if (!isEligible(asset)) {
        throw error(
            HttpStatus.CONFLICT,
            "Selecione uma imagem ADS aprovada por Têmis e Psique, com SHA-256 persistido.");
      }
      requireHttps(asset.getAssetUrl(), "O controle estático precisa de uma URL HTTPS válida.");

      List<Creative> existing =
          creatives.findByExperimentIdAndImageUrl(experimentId, asset.getAssetUrl().trim());
      if (!existing.isEmpty()) {
        return existing.stream()
            .filter(candidate -> sameCopy(candidate, request, experiment))
            .findFirst()
            .orElseThrow(
                () ->
                    error(
                        HttpStatus.CONFLICT,
                        "Este ativo já possui anúncio. Crie uma nova versão na aba Criativos para alterar o texto."));
      }

      CreateCreativeRequest content = new CreateCreativeRequest();
      content.setFormat("IMAGE");
      content.setHeadline(request.headline().trim());
      content.setPrimaryText(request.primaryText().trim());
      content.setDescription(normalize(request.description()));
      content.setImageUrl(asset.getAssetUrl().trim());
      content.setDestinationUrl(experiment.getFollowUpActionUrl().trim());
      content.setCta("LEARN_MORE");
      content.setInstagramUserId(experiment.getInstagramAccount().getCode());
      content.setStatus(CreativeStatus.DRAFT);
      Creative creative = creativeService.create(experimentId, content);

      var evidence =
          JsonNodeFactory.instance
              .objectNode()
              .put("commercialPlanId", asset.getCommercialPlan().getId())
              .put("visualAssetId", asset.getId())
              .put("contentSha256", asset.getContentSha256())
              .put("creativeId", creative.getId())
              .put("requestedBy", TenantContextHolder.resolveUserEmail(null))
              .put("mediaGenerated", false)
              .put("campaignPublished", false)
              .put("approvedAssetAgentReviewVerified", true)
              .put("approvedAssetCustomerReviewVerified", true)
              .put("creativeAgentReviewGranted", false)
              .put("creativeHumanApprovalGranted", false);
      history.create(
          experimentId,
          new CreateRequest(
              "DECISAO",
              "Controle estático aprovado selecionado para anúncio",
              "Ativo visual #"
                  + asset.getId()
                  + " do plano #"
                  + asset.getCommercialPlan().getId()
                  + " vinculado ao anúncio #"
                  + creative.getId()
                  + ". O arquivo aprovado foi preservado e a nova combinação de copy exige revisão de Têmis e aprovação humana.",
              evidence.toString(),
              "UI_APPROVED_VISUAL_TO_CREATIVE_V1",
              Instant.now()));
      return creative;
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao selecionar controle estático aprovado. experimentId={} visualAssetId={}",
          experimentId,
          assetId,
          ex);
      throw ex;
    }
  }

  /** Confirma que o plano dono do ativo é o governante mais recente do experimento solicitado. */
  private void validateBelongsToExperiment(Long experimentId, CommercialPlanVisualAsset asset) {
    Long governingPlanId =
        plans.findByExperimentReference(experimentId).stream()
            .findFirst()
            .map(plan -> plan.getId())
            .orElse(null);
    if (asset.getCommercialPlan() == null
        || !Objects.equals(governingPlanId, asset.getCommercialPlan().getId())) {
      throw error(HttpStatus.NOT_FOUND, "Ativo visual não pertence a este experimento.");
    }
  }

  /** Exige todos os sinais persistidos que autorizam reutilizar os mesmos pixels como anúncio. */
  private boolean isEligible(CommercialPlanVisualAsset asset) {
    return asset != null
        && asset.getStatus() == CommercialPlanVisualAssetStatus.APPROVED
        && asset.getAgentReviewStatus() == CommercialPlanVisualAssetReviewStatus.APPROVED
        && asset.getCustomerReviewStatus() == CommercialPlanVisualAssetReviewStatus.APPROVED
        && "IMAGE".equalsIgnoreCase(asset.getMediaType())
        && hasAdsPurpose(asset)
        && StringUtils.hasText(asset.getAssetUrl())
        && StringUtils.hasText(asset.getContentSha256())
        && asset.getContentSha256().trim().matches("(?i)[0-9a-f]{64}");
  }

  /** Reconhece ADS no papel singular legado ou na lista versionada de finalidades. */
  private boolean hasAdsPurpose(CommercialPlanVisualAsset asset) {
    if ("ADS".equalsIgnoreCase(asset.getPurpose())) {
      return true;
    }
    if (!StringUtils.hasText(asset.getPurposesJson())) {
      return false;
    }
    try {
      JsonNode values = objectMapper.readTree(asset.getPurposesJson());
      if (!values.isArray()) {
        return false;
      }
      for (JsonNode value : values) {
        if ("ADS".equalsIgnoreCase(value.asText())) {
          return true;
        }
      }
      return false;
    } catch (Exception ex) {
      log.error(
          "Falha ao ler finalidades do controle estático. visualAssetId={}", asset.getId(), ex);
      return false;
    }
  }

  /** Converte o registro aprovado no contrato mínimo consumido pela interface. */
  private ApprovedVisualAssetOption toOption(CommercialPlanVisualAsset asset) {
    return new ApprovedVisualAssetOption(
        asset.getId(),
        asset.getCommercialPlan().getId(),
        asset.getAssetUrl(),
        asset.getLabel(),
        asset.getContentSha256(),
        asset.getOrigin(),
        asset.getRightsStatement());
  }

  /** Carrega o experimento ou devolve um erro estável para a interface. */
  private Experiment requireExperiment(Long experimentId) {
    return experiments
        .findById(experimentId)
        .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Experimento não encontrado."));
  }

  /** Bloqueia o experimento durante a seleção para impedir corrida com a liberação de mídia. */
  private Experiment requireExperimentForSelection(Long experimentId) {
    return experiments
        .findForApprovedVisualAssetCreativeSelection(experimentId)
        .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Experimento não encontrado."));
  }

  /** Reconhece repetição do mesmo comando sem duplicar o controle no portfólio. */
  private boolean sameCopy(
      Creative creative, ApprovedVisualAssetCreativeRequest request, Experiment experiment) {
    return Objects.equals(creative.getHeadline(), request.headline().trim())
        && Objects.equals(creative.getPrimaryText(), request.primaryText().trim())
        && Objects.equals(normalize(creative.getDescription()), normalize(request.description()))
        && Objects.equals(creative.getDestinationUrl(), experiment.getFollowUpActionUrl().trim());
  }

  /** Recusa excessos antes de abrir uma revisão, contando emojis como a tela e o publicador. */
  private void requirePublicationCopy(ApprovedVisualAssetCreativeRequest request) {
    var violations =
        CreativePublicationCopyPolicy.violations(
            request.primaryText().trim(),
            request.headline().trim(),
            normalize(request.description()));
    if (!violations.isEmpty()) {
      throw error(
          HttpStatus.BAD_REQUEST,
          "Copy incompatível com publicação: " + String.join(" ", violations));
    }
  }

  /** Exige HTTPS com host real antes de reutilizar mídia ou destino em publicação. */
  private void requireHttps(String value, String message) {
    URI uri;
    try {
      uri = URI.create(value == null ? "" : value.trim());
    } catch (IllegalArgumentException ex) {
      log.warn("URL inválida na seleção do controle estático. operacao=requireHttps", ex);
      throw error(HttpStatus.BAD_REQUEST, message);
    }
    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
      throw error(HttpStatus.BAD_REQUEST, message);
    }
  }

  /** Normaliza descrição opcional para persistência e comparação idempotente. */
  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** Mantém respostas de contrato compreensíveis para a operação pela interface. */
  private ResponseStatusException error(HttpStatus status, String message) {
    return new ResponseStatusException(status, message);
  }
}
