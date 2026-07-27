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
            .contextType(required(request.contextType(), "Tipo de contexto"))
            .productionMode(required(request.productionMode(), "Modo de produção"))
            .targetChannel(required(request.targetChannel(), "Canal"))
            .format(required(request.format(), "Formato"))
            .title(required(request.title(), "Título"))
            .objective(required(request.objective(), "Objetivo"))
            .funnelStage(trimToNull(request.funnelStage()))
            .primaryMetric(trimToNull(request.primaryMetric()))
            .hookText(trimToNull(request.hookText()))
            .scriptText(trimToNull(request.scriptText()))
            .scenePlan(trimToNull(request.scenePlan()))
            .visualReferences(trimToNull(request.visualReferences()))
            .voiceoverPlan(trimToNull(request.voiceoverPlan()))
            .soundtrackPlan(trimToNull(request.soundtrackPlan()))
            .captionPlan(trimToNull(request.captionPlan()))
            .ctaText(trimToNull(request.ctaText()))
            .targetDurationSeconds(request.targetDurationSeconds())
            .providerPlan(trimToNull(request.providerPlan()))
            .editingNotes(trimToNull(request.editingNotes()))
            .qualityGate(trimToNull(request.qualityGate()))
            .status(Objects.requireNonNullElse(request.status(), VideoProjectStatus.DRAFT))
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
    project.setContextType(required(request.contextType(), "Tipo de contexto"));
    project.setProductionMode(required(request.productionMode(), "Modo de produção"));
    project.setTargetChannel(required(request.targetChannel(), "Canal"));
    project.setFormat(required(request.format(), "Formato"));
    project.setTitle(required(request.title(), "Título"));
    project.setObjective(required(request.objective(), "Objetivo"));
    project.setFunnelStage(trimToNull(request.funnelStage()));
    project.setPrimaryMetric(trimToNull(request.primaryMetric()));
    project.setHookText(trimToNull(request.hookText()));
    project.setScriptText(trimToNull(request.scriptText()));
    project.setScenePlan(trimToNull(request.scenePlan()));
    project.setVisualReferences(trimToNull(request.visualReferences()));
    project.setVoiceoverPlan(trimToNull(request.voiceoverPlan()));
    project.setSoundtrackPlan(trimToNull(request.soundtrackPlan()));
    project.setCaptionPlan(trimToNull(request.captionPlan()));
    project.setCtaText(trimToNull(request.ctaText()));
    project.setTargetDurationSeconds(request.targetDurationSeconds());
    project.setProviderPlan(trimToNull(request.providerPlan()));
    project.setEditingNotes(trimToNull(request.editingNotes()));
    project.setQualityGate(trimToNull(request.qualityGate()));
    project.setStatus(Objects.requireNonNullElse(request.status(), VideoProjectStatus.DRAFT));
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

  /** Converte a entidade do projeto de vídeo para contrato REST. */
  private static VideoProjectDto toDto(VideoProject project) {
    return new VideoProjectDto(
        project.getId(),
        project.getTenantId(),
        project.getProductId(),
        project.getExperimentId(),
        project.getSalesVideoProfileId(),
        project.getCampaignKey(),
        project.getContextType(),
        project.getProductionMode(),
        project.getTargetChannel(),
        project.getFormat(),
        project.getTitle(),
        project.getObjective(),
        project.getFunnelStage(),
        project.getPrimaryMetric(),
        project.getHookText(),
        project.getScriptText(),
        project.getScenePlan(),
        project.getVisualReferences(),
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
