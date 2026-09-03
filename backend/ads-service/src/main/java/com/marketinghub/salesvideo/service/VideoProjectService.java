package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.VideoProjectStatus;
import com.marketinghub.salesvideo.dto.CreateVideoProjectRequest;
import com.marketinghub.salesvideo.dto.UpdateVideoProjectRequest;
import com.marketinghub.salesvideo.dto.VideoProjectDto;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.mapper.VideoProjectResearchIntelligenceMapper;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
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
  private VideoProjectResearchIntelligenceMapper researchIntelligenceMapper;

  /** Inicializa o serviço com o repositório canônico de projetos de vídeo. */
  public VideoProjectService(VideoProjectRepository repository) {
    this.repository = repository;
  }

  /**
   * Conecta a biblioteca comum sem quebrar testes unitários que instanciam o serviço diretamente.
   */
  @Autowired
  public void setResearchIntelligenceMapper(
      VideoProjectResearchIntelligenceMapper researchIntelligenceMapper) {
    this.researchIntelligenceMapper = researchIntelligenceMapper;
  }

  /** Lista os projetos de vídeo do tenant atual. */
  @Transactional(readOnly = true)
  public List<VideoProjectDto> listProjects() {
    String tenantId = TenantContextHolder.requireTenant();
    return repository.findByTenantIdOrderByUpdatedAtDesc(tenantId).stream()
        .map(project -> toDto(project, false))
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
            .commercialPlanId(request.commercialPlanId())
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
            .strategyGroupKey(trimToNull(request.strategyGroupKey()))
            .strategyRole(trimToNull(request.strategyRole()))
            .commercialHypothesis(trimToNull(request.commercialHypothesis()))
            .persuasionFramework(trimToNull(request.persuasionFramework()))
            .scientificBasis(trimToNull(request.scientificBasis()))
            .measurementPlan(trimToNull(request.measurementPlan()))
            .resultsSnapshot(trimToNull(request.resultsSnapshot()))
            .learningDecision(validatedLearningDecision(request.learningDecision()))
            .confirmedLearning(trimToNull(request.confirmedLearning()))
            .nextVersionRecommendation(trimToNull(request.nextVersionRecommendation()))
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
            .characterPerformanceType(
                validatedCharacterPerformanceType(request.characterPerformanceType()))
            .characterPerformanceUri(
                validatedHttpsOptional(
                    request.characterPerformanceUri(), "Personagem da performance"))
            .referencePerformanceUri(
                validatedHttpsOptional(
                    request.referencePerformanceUri(), "Performance de referência"))
            .referencePerformanceDurationSeconds(
                validatedReferencePerformanceDuration(
                    request.referencePerformanceDurationSeconds()))
            .performanceConsentEvidence(trimToNull(request.performanceConsentEvidence()))
            .performanceRightsEvidence(trimToNull(request.performanceRightsEvidence()))
            .editingNotes(trimToNull(request.editingNotes()))
            .qualityGate(trimToNull(request.qualityGate()))
            .status(validatedStatus(request.status(), request))
            .createdBy(trimToNull(request.createdBy()))
            .updatedBy(trimToNull(request.createdBy()))
            .build();
    return toDto(repository.save(project), true);
  }

  /** Consulta um projeto de vídeo do tenant atual. */
  @Transactional(readOnly = true)
  public VideoProjectDto getProject(Long projectId) {
    return toDto(loadProject(projectId), true);
  }

  /** Atualiza a definição editorial de um projeto de vídeo. */
  @Transactional
  public VideoProjectDto updateProject(Long projectId, UpdateVideoProjectRequest request) {
    VideoProject project = loadProject(projectId);
    project.setProductId(request.productId());
    project.setCommercialPlanId(request.commercialPlanId());
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
    project.setStrategyGroupKey(trimToNull(request.strategyGroupKey()));
    project.setStrategyRole(trimToNull(request.strategyRole()));
    project.setCommercialHypothesis(trimToNull(request.commercialHypothesis()));
    project.setPersuasionFramework(trimToNull(request.persuasionFramework()));
    project.setScientificBasis(trimToNull(request.scientificBasis()));
    project.setMeasurementPlan(trimToNull(request.measurementPlan()));
    project.setResultsSnapshot(trimToNull(request.resultsSnapshot()));
    project.setLearningDecision(validatedLearningDecision(request.learningDecision()));
    project.setConfirmedLearning(trimToNull(request.confirmedLearning()));
    project.setNextVersionRecommendation(trimToNull(request.nextVersionRecommendation()));
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
    project.setCharacterPerformanceType(
        validatedCharacterPerformanceType(request.characterPerformanceType()));
    project.setCharacterPerformanceUri(
        validatedHttpsOptional(request.characterPerformanceUri(), "Personagem da performance"));
    project.setReferencePerformanceUri(
        validatedHttpsOptional(request.referencePerformanceUri(), "Performance de referência"));
    project.setReferencePerformanceDurationSeconds(
        validatedReferencePerformanceDuration(request.referencePerformanceDurationSeconds()));
    project.setPerformanceConsentEvidence(trimToNull(request.performanceConsentEvidence()));
    project.setPerformanceRightsEvidence(trimToNull(request.performanceRightsEvidence()));
    project.setEditingNotes(trimToNull(request.editingNotes()));
    project.setQualityGate(trimToNull(request.qualityGate()));
    project.setStatus(validatedStatus(request.status(), request));
    project.setUpdatedBy(trimToNull(request.updatedBy()));
    return toDto(repository.save(project), true);
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

  /** Aceita somente os tipos de personagem previstos pelo contrato do provider de performance. */
  private static String validatedCharacterPerformanceType(String value) {
    String normalized = trimToNull(value);
    if (normalized == null || normalized.equals("image") || normalized.equals("video")) {
      return normalized;
    }
    throw VideoModuleException.badRequest(
        VideoModuleErrorCode.BAD_REQUEST,
        "Tipo de personagem da performance deve ser image ou video");
  }

  /** Valida URL HTTPS opcional antes de persistir uma referência de mídia externa. */
  private static String validatedHttpsOptional(String value, String fieldName) {
    String normalized = trimToNull(value);
    if (normalized == null || normalized.startsWith("https://")) {
      return normalized;
    }
    throw VideoModuleException.badRequest(
        VideoModuleErrorCode.BAD_REQUEST, fieldName + " deve usar URL HTTPS");
  }

  /** Preserva ausência ou exige o intervalo oficial de 3 a 30 segundos da performance. */
  private static Integer validatedReferencePerformanceDuration(Integer value) {
    if (value == null || (value >= 3 && value <= 30)) {
      return value;
    }
    throw VideoModuleException.badRequest(
        VideoModuleErrorCode.BAD_REQUEST,
        "Performance de referência deve ter duração medida entre 3 e 30 segundos");
  }

  /** Valida a decisao de aprendizado sem inferir resultado comercial no backend. */
  private static String validatedLearningDecision(String value) {
    String normalized = trimToNull(value);
    if (normalized == null
        || normalized.equals("COLLECTING")
        || normalized.equals("CONTINUE")
        || normalized.equals("ADJUST")
        || normalized.equals("STOP")) {
      return normalized;
    }
    throw VideoModuleException.badRequest(
        VideoModuleErrorCode.BAD_REQUEST, "Decisão de aprendizado inválida: " + normalized);
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

  /** Converte o projeto para REST e evita ampliar a listagem com contexto usado só no detalhe. */
  private VideoProjectDto toDto(VideoProject project, boolean includeResearchIntelligence) {
    return new VideoProjectDto(
        project.getId(),
        project.getTenantId(),
        project.getProductId(),
        project.getCommercialPlanId(),
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
        project.getStrategyGroupKey(),
        project.getStrategyRole(),
        project.getCommercialHypothesis(),
        project.getPersuasionFramework(),
        project.getScientificBasis(),
        project.getMeasurementPlan(),
        project.getResultsSnapshot(),
        project.getLearningDecision(),
        project.getConfirmedLearning(),
        project.getNextVersionRecommendation(),
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
        project.getCharacterPerformanceType(),
        project.getCharacterPerformanceUri(),
        project.getReferencePerformanceUri(),
        project.getReferencePerformanceDurationSeconds(),
        project.getPerformanceConsentEvidence(),
        project.getPerformanceRightsEvidence(),
        project.getEditingNotes(),
        project.getQualityGate(),
        project.getStatus(),
        project.getCreatedBy(),
        project.getUpdatedBy(),
        project.getCreatedAt(),
        project.getUpdatedAt(),
        !includeResearchIntelligence || researchIntelligenceMapper == null
            ? null
            : researchIntelligenceMapper.selectForVideoProject(project));
  }
}
