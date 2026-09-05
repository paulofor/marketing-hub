package com.marketinghub.creative.service.video;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.history.ExperimentHistoryEventContracts.CreateRequest;
import com.marketinghub.experiment.history.ExperimentHistoryEventService;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Seleciona mídia aprovada e materializa seu anúncio com substituição e histórico atômicos. */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoCreativeService {
  private final ExperimentRepository experiments;
  private final ExperimentVideoAssetRepository videos;
  private final CreativeRepository creatives;
  private final CreativeService creativeService;
  private final ExperimentHistoryEventService history;

  /** Cria uma única revisão pendente sem publicar, gerar mídia ou conceder aprovação humana. */
  @Transactional
  public Creative create(Long experimentId, Long videoId, VideoCreativeRequest request) {
    try {
      String tenant = TenantContextHolder.requireTenant();
      Experiment experiment =
          experiments
              .findForVideoCreativeSelection(experimentId)
              .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Experimento não encontrado."));
      if (experiment.getStatus() != ExperimentStatus.PLANNED
          || experiment.getFacebookReleaseRequestedAt() != null) {
        throw error(HttpStatus.CONFLICT, "O experimento já foi liberado ou não está planejado.");
      }
      ExperimentVideoAsset video = scopedVideo(experimentId, videoId, tenant);
      if (video.getSlot() != ExperimentVideoSlot.AD
          || video.getStatus() != ExperimentVideoStatus.READY
          || video.getReviewStatus() != ExperimentVideoReviewStatus.APPROVED
          || !Boolean.TRUE.equals(video.getHasAudio())) {
        throw error(
            HttpStatus.CONFLICT, "Selecione um vídeo de anúncio pronto, aprovado e com áudio.");
      }
      requireHttps(video.getAssetUrl(), "O vídeo aprovado precisa de uma URL HTTPS válida.");
      requireHttps(experiment.getFollowUpActionUrl(), "Configure o destino HTTPS do experimento.");
      ExperimentVideoAsset replaced = null;
      if (request.replacesVideoAssetId() != null) {
        replaced = scopedVideo(experimentId, request.replacesVideoAssetId(), tenant);
        if (videoId.equals(replaced.getId())
            || replaced.getSlot() != ExperimentVideoSlot.AD
            || replaced.getReviewStatus() != ExperimentVideoReviewStatus.REJECTED) {
          throw error(
              HttpStatus.CONFLICT, "Somente outro vídeo AD reprovado pode ser substituído.");
        }
      }
      var existing = creatives.findByExperimentIdAndVideoUrl(experimentId, video.getAssetUrl());
      Creative creative = null;
      if (!existing.isEmpty()) {
        creative =
            existing.stream()
                .filter(c -> sameCopy(c, request, experiment))
                .findFirst()
                .orElseThrow(
                    () ->
                        error(
                            HttpStatus.CONFLICT,
                            "Este vídeo já possui anúncio. Crie uma nova versão na aba Criativos para alterar o texto."));
        if (video.isRequiredForRelease()
            && (replaced == null || !replaced.isRequiredForRelease())) {
          return creative;
        }
      }
      if (creative == null) {
        CreateCreativeRequest content = new CreateCreativeRequest();
        content.setFormat("VIDEO");
        content.setHeadline(request.headline().trim());
        content.setPrimaryText(request.primaryText().trim());
        content.setDescription(normalize(request.description()));
        content.setVideoUrl(video.getAssetUrl());
        content.setImageUrl(video.getThumbnailUrl());
        content.setDestinationUrl(experiment.getFollowUpActionUrl().trim());
        content.setCta("LEARN_MORE");
        content.setStatus(CreativeStatus.DRAFT);
        if (experiment.getInstagramAccount() != null) {
          content.setInstagramUserId(experiment.getInstagramAccount().getCode());
        }
        creative = creativeService.create(experimentId, content);
      }
      video.setRequiredForRelease(true);
      videos.save(video);
      if (replaced != null) {
        replaced.setRequiredForRelease(false);
        videos.save(replaced);
      }
      var evidence =
          JsonNodeFactory.instance
              .objectNode()
              .put("videoAssetId", videoId)
              .put("creativeId", creative.getId())
              .put("replacesVideoAssetId", request.replacesVideoAssetId())
              .put("requestedBy", TenantContextHolder.resolveUserEmail(null))
              .put("mediaGenerated", false)
              .put("campaignPublished", false);
      history.create(
          experimentId,
          new CreateRequest(
              "DECISAO",
              "Vídeo aprovado selecionado para anúncio",
              "Vídeo #"
                  + videoId
                  + " vinculado ao anúncio #"
                  + creative.getId()
                  + ". Revisão de Têmis e aprovação final permanecem nos gates próprios do anúncio."
                  + (replaced == null
                      ? ""
                      : " Substitui o vídeo reprovado #"
                          + replaced.getId()
                          + ", preservado no histórico."),
              evidence.toString(),
              "UI_VIDEO_TO_CREATIVE_V1",
              Instant.now()));
      return creative;
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao selecionar vídeo para anúncio. experimentId={} videoAssetId={}",
          experimentId,
          videoId,
          ex);
      throw ex;
    }
  }

  /** Confirma propriedade do experimento e tenant antes de ler ou selecionar um vídeo. */
  private ExperimentVideoAsset scopedVideo(Long experimentId, Long videoId, String tenant) {
    ExperimentVideoAsset video =
        videos
            .findById(videoId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Vídeo não encontrado."));
    String owner =
        video.getSalesVideoProfile() != null
            ? video.getSalesVideoProfile().getTenantId()
            : video.getSalesVideoJob() != null ? video.getSalesVideoJob().getTenantId() : "default";
    if (video.getExperiment() == null
        || !experimentId.equals(video.getExperiment().getId())
        || !tenant.equals(owner)) {
      throw error(HttpStatus.NOT_FOUND, "Vídeo não encontrado neste experimento e tenant.");
    }
    return video;
  }

  /** Reconhece repetição do comando sem reiniciar o parecer nem duplicar o anúncio. */
  private boolean sameCopy(Creative creative, VideoCreativeRequest request, Experiment experiment) {
    return Objects.equals(creative.getHeadline(), request.headline().trim())
        && Objects.equals(creative.getPrimaryText(), request.primaryText().trim())
        && Objects.equals(normalize(creative.getDescription()), normalize(request.description()))
        && Objects.equals(creative.getDestinationUrl(), experiment.getFollowUpActionUrl().trim());
  }

  /** Exige HTTPS com host real sem aceitar caminhos ou esquemas impróprios para publicação. */
  private void requireHttps(String value, String message) {
    URI uri;
    try {
      uri = URI.create(value == null ? "" : value.trim());
    } catch (IllegalArgumentException ex) {
      log.warn("URL inválida na seleção de vídeo para anúncio. operacao=requireHttps", ex);
      throw error(HttpStatus.BAD_REQUEST, message);
    }
    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
      throw error(HttpStatus.BAD_REQUEST, message);
    }
  }

  /** Normaliza descrição opcional para comparação idempotente. */
  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** Mantém erros de contrato compreensíveis para o operador. */
  private ResponseStatusException error(HttpStatus status, String message) {
    return new ResponseStatusException(status, message);
  }
}
