package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.VideoProjectStatus;
import com.marketinghub.salesvideo.dto.CreateVideoProjectRequest;
import com.marketinghub.salesvideo.dto.UpdateVideoProjectRequest;
import com.marketinghub.salesvideo.dto.VideoProjectDto;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Componente interno que executa o cadastro editorial de projetos de vídeo do Marketing Hub. */
@Component
public class VideoProjectService {
  public static final String VIDEO_CATEGORY_COMMERCIAL_SHORT = "COMMERCIAL_SHORT";
  public static final String VIDEO_CATEGORY_INSTITUTIONAL_CONTENT = "INSTITUTIONAL_CONTENT";
  public static final String VIDEO_CATEGORY_LONG_FORM = "LONG_FORM";
  public static final int MINIMUM_COMMERCIAL_SHORT_DURATION_SECONDS = 6;
  public static final int MAXIMUM_COMMERCIAL_SHORT_DURATION_SECONDS = 60;
  public static final int MINIMUM_LONG_FORM_DURATION_SECONDS = 180;

  private final VideoProjectRepository repository;

  /** Inicializa o serviço com o repositório canônico de projetos de vídeo. */
  public VideoProjectService(VideoProjectRepository repository) {
    this.repository = repository;
  }

  /** Lista os projetos de vídeo do tenant atual. */
  @Transactional(readOnly = true)
  public List<VideoProjectDto> listProjects() {
    String tenantId = TenantContextHolder.requireTenant();
    return repository.findByTenantIdOrderByUpdatedAtDesc(tenantId).stream()
        .map(VideoProjectService::toDto)
        .toList();
  }

  /** Cria um novo projeto de vídeo editável. */
  @Transactional
  public VideoProjectDto createProject(CreateVideoProjectRequest request) {
    String tenantId = TenantContextHolder.requireTenant();
    VideoProject project =
        VideoProject.builder()
            .tenantId(tenantId)
            .productId(request.productId())
            .experimentId(request.experimentId())
            .salesVideoProfileId(request.salesVideoProfileId())
            .campaignKey(trimToNull(request.campaignKey()))
            .videoCategory(validatedVideoCategory(request.videoCategory()))
            .contextType(required(request.contextType(), "Tipo de contexto"))
            .productionMode(required(request.productionMode(), "Modo de produção"))
            .targetChannel(required(request.targetChannel(), "Canal"))
            .format(required(request.format(), "Formato"))
            .title(required(request.title(), "Título"))
            .objective(required(request.objective(), "Objetivo"))
            .storyText(trimToNull(request.storyText()))
            .funnelStage(trimToNull(request.funnelStage()))
            .primaryMetric(trimToNull(request.primaryMetric()))
            .hookText(trimToNull(request.hookText()))
            .scriptText(trimToNull(request.scriptText()))
            .scenePlan(trimToNull(request.scenePlan()))
            .visualReferences(trimToNull(request.visualReferences()))
            .characterBible(trimToNull(request.characterBible()))
            .environmentBible(trimToNull(request.environmentBible()))
            .objectBible(trimToNull(request.objectBible()))
            .visualStyleGuide(trimToNull(request.visualStyleGuide()))
            .imageGenerationPlan(trimToNull(request.imageGenerationPlan()))
            .continuityRules(trimToNull(request.continuityRules()))
            .voiceoverPlan(trimToNull(request.voiceoverPlan()))
            .soundtrackPlan(trimToNull(request.soundtrackPlan()))
            .captionPlan(trimToNull(request.captionPlan()))
            .ctaText(trimToNull(request.ctaText()))
            .targetDurationSeconds(
                validatedTargetDurationSeconds(
                    validatedVideoCategory(request.videoCategory()),
                    request.targetDurationSeconds()))
            .providerPlan(trimToNull(request.providerPlan()))
            .editingNotes(trimToNull(request.editingNotes()))
            .qualityGate(trimToNull(request.qualityGate()))
            .status(validatedStatus(request.status(), request))
            .createdBy(trimToNull(request.createdBy()))
            .updatedBy(trimToNull(request.createdBy()))
            .build();
    return toDto(repository.save(project));
  }

  /** Consulta um projeto de vídeo do tenant atual. */
  @Transactional(readOnly = true)
  public VideoProjectDto getProject(Long projectId) {
    return toDto(loadProject(projectId));
  }

  /** Atualiza a definição editorial de um projeto de vídeo. */
  @Transactional
  public VideoProjectDto updateProject(Long projectId, UpdateVideoProjectRequest request) {
    VideoProject project = loadProject(projectId);
    project.setProductId(request.productId());
    project.setExperimentId(request.experimentId());
    project.setSalesVideoProfileId(request.salesVideoProfileId());
    project.setCampaignKey(trimToNull(request.campaignKey()));
    project.setVideoCategory(validatedVideoCategory(request.videoCategory()));
    project.setContextType(required(request.contextType(), "Tipo de contexto"));
    project.setProductionMode(required(request.productionMode(), "Modo de produção"));
    project.setTargetChannel(required(request.targetChannel(), "Canal"));
    project.setFormat(required(request.format(), "Formato"));
    project.setTitle(required(request.title(), "Título"));
    project.setObjective(required(request.objective(), "Objetivo"));
    project.setStoryText(trimToNull(request.storyText()));
    project.setFunnelStage(trimToNull(request.funnelStage()));
    project.setPrimaryMetric(trimToNull(request.primaryMetric()));
    project.setHookText(trimToNull(request.hookText()));
    project.setScriptText(trimToNull(request.scriptText()));
    project.setScenePlan(trimToNull(request.scenePlan()));
    project.setVisualReferences(trimToNull(request.visualReferences()));
    project.setCharacterBible(trimToNull(request.characterBible()));
    project.setEnvironmentBible(trimToNull(request.environmentBible()));
    project.setObjectBible(trimToNull(request.objectBible()));
    project.setVisualStyleGuide(trimToNull(request.visualStyleGuide()));
    project.setImageGenerationPlan(trimToNull(request.imageGenerationPlan()));
    project.setContinuityRules(trimToNull(request.continuityRules()));
    project.setVoiceoverPlan(trimToNull(request.voiceoverPlan()));
    project.setSoundtrackPlan(trimToNull(request.soundtrackPlan()));
    project.setCaptionPlan(trimToNull(request.captionPlan()));
    project.setCtaText(trimToNull(request.ctaText()));
    project.setTargetDurationSeconds(
        validatedTargetDurationSeconds(
            validatedVideoCategory(request.videoCategory()), request.targetDurationSeconds()));
    project.setProviderPlan(trimToNull(request.providerPlan()));
    project.setEditingNotes(trimToNull(request.editingNotes()));
    project.setQualityGate(trimToNull(request.qualityGate()));
    project.setStatus(validatedStatus(request.status(), request));
    project.setUpdatedBy(trimToNull(request.updatedBy()));
    return toDto(repository.save(project));
  }

  /** Carrega um projeto garantindo isolamento por tenant. */
  private VideoProject loadProject(Long projectId) {
    String tenantId = TenantContextHolder.requireTenant();
    VideoProject project =
        repository
            .findById(projectId)
            .orElseThrow(
                () ->
                    VideoModuleException.notFound(
                        VideoModuleErrorCode.PROFILE_NOT_FOUND,
                        "Projeto de vídeo não encontrado: " + projectId));
    if (!tenantId.equals(project.getTenantId())) {
      throw VideoModuleException.notFound(
          VideoModuleErrorCode.PROFILE_NOT_FOUND, "Projeto de vídeo não encontrado: " + projectId);
    }
    return project;
  }

  /** Normaliza texto obrigatório para gravação. */
  private static String required(String value, String fieldName) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST, fieldName + " é obrigatório no projeto de vídeo");
    }
    return normalized;
  }

  /** Normaliza strings vazias recebidas do frontend. */
  private static String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  /** Valida a categoria comercial do vídeo e preserva compatibilidade com projetos antigos. */
  private static String validatedVideoCategory(String videoCategory) {
    String normalized =
        Objects.requireNonNullElse(trimToNull(videoCategory), VIDEO_CATEGORY_LONG_FORM);
    if (VIDEO_CATEGORY_COMMERCIAL_SHORT.equals(normalized)
        || VIDEO_CATEGORY_LONG_FORM.equals(normalized)
        || VIDEO_CATEGORY_INSTITUTIONAL_CONTENT.equals(normalized)) {
      return normalized;
    }
    throw VideoModuleException.badRequest(
        VideoModuleErrorCode.BAD_REQUEST, "Categoria de vídeo inválida: " + normalized);
  }

  /** Valida a duração mínima e máxima conforme a categoria comercial do projeto. */
  private static Integer validatedTargetDurationSeconds(
      String videoCategory, Integer targetDurationSeconds) {
    if (targetDurationSeconds == null) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "Duração alvo é obrigatória para projeto do Estúdio de Audio e Video");
    }
    if (VIDEO_CATEGORY_COMMERCIAL_SHORT.equals(videoCategory)) {
      if (targetDurationSeconds < MINIMUM_COMMERCIAL_SHORT_DURATION_SECONDS
          || targetDurationSeconds > MAXIMUM_COMMERCIAL_SHORT_DURATION_SECONDS) {
        throw VideoModuleException.badRequest(
            VideoModuleErrorCode.BAD_REQUEST,
            "Vídeo comercial curto deve ter entre 6 e 60 segundos");
      }
      return targetDurationSeconds;
    }
    if (VIDEO_CATEGORY_INSTITUTIONAL_CONTENT.equals(videoCategory)) {
      return targetDurationSeconds;
    }
    if (targetDurationSeconds < MINIMUM_LONG_FORM_DURATION_SECONDS) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST, "Vídeo longo ou VSL deve ter 180 segundos ou mais");
    }
    return targetDurationSeconds;
  }

  /** Bloqueia avanço para renderização quando a bíblia visual premium ainda não foi definida. */
  private static VideoProjectStatus validatedStatus(
      VideoProjectStatus requestedStatus, CreateVideoProjectRequest request) {
    VideoProjectStatus status =
        Objects.requireNonNullElse(requestedStatus, VideoProjectStatus.DRAFT);
    validateVisualBibleForProduction(
        status,
        request.characterBible(),
        request.environmentBible(),
        request.objectBible(),
        request.visualStyleGuide(),
        request.imageGenerationPlan(),
        request.continuityRules());
    return status;
  }

  /** Bloqueia avanço para renderização quando a bíblia visual premium ainda não foi definida. */
  private static VideoProjectStatus validatedStatus(
      VideoProjectStatus requestedStatus, UpdateVideoProjectRequest request) {
    VideoProjectStatus status =
        Objects.requireNonNullElse(requestedStatus, VideoProjectStatus.DRAFT);
    validateVisualBibleForProduction(
        status,
        request.characterBible(),
        request.environmentBible(),
        request.objectBible(),
        request.visualStyleGuide(),
        request.imageGenerationPlan(),
        request.continuityRules());
    return status;
  }

  /** Valida os blocos mínimos de continuidade antes de qualquer produção de vídeo premium. */
  private static void validateVisualBibleForProduction(
      VideoProjectStatus status,
      String characterBible,
      String environmentBible,
      String objectBible,
      String visualStyleGuide,
      String imageGenerationPlan,
      String continuityRules) {
    if (!requiresVisualBible(status)) {
      return;
    }
    if (trimToNull(characterBible) == null
        || trimToNull(environmentBible) == null
        || trimToNull(objectBible) == null
        || trimToNull(visualStyleGuide) == null
        || trimToNull(imageGenerationPlan) == null
        || trimToNull(continuityRules) == null) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "Bíblia visual completa é obrigatória antes de renderizar vídeo no Estúdio");
    }
  }

  /** Informa se o status representa construção ou liberação de vídeo. */
  private static boolean requiresVisualBible(VideoProjectStatus status) {
    return status == VideoProjectStatus.READY_FOR_RENDER
        || status == VideoProjectStatus.IN_PRODUCTION
        || status == VideoProjectStatus.READY_FOR_REVIEW
        || status == VideoProjectStatus.APPROVED;
  }

  /** Converte a entidade do projeto de vídeo para contrato REST. */
  private static VideoProjectDto toDto(VideoProject project) {
    return new VideoProjectDto(
        project.getId(),
        project.getTenantId(),
        project.getProductId(),
        project.getExperimentId(),
        project.getSalesVideoProfileId(),
        project.getCampaignKey(),
        validatedVideoCategory(project.getVideoCategory()),
        project.getContextType(),
        project.getProductionMode(),
        project.getTargetChannel(),
        project.getFormat(),
        project.getTitle(),
        project.getObjective(),
        project.getStoryText(),
        project.getFunnelStage(),
        project.getPrimaryMetric(),
        project.getHookText(),
        project.getScriptText(),
        project.getScenePlan(),
        project.getVisualReferences(),
        project.getCharacterBible(),
        project.getEnvironmentBible(),
        project.getObjectBible(),
        project.getVisualStyleGuide(),
        project.getImageGenerationPlan(),
        project.getContinuityRules(),
        project.getVoiceoverPlan(),
        project.getSoundtrackPlan(),
        project.getCaptionPlan(),
        project.getCtaText(),
        project.getTargetDurationSeconds(),
        project.getProviderPlan(),
        project.getEditingNotes(),
        project.getQualityGate(),
        project.getStatus(),
        project.getCreatedBy(),
        project.getUpdatedBy(),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }
}
