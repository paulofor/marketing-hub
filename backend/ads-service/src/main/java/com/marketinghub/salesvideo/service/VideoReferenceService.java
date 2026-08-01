package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.salesvideo.VideoReferenceRepository;
import com.marketinghub.salesvideo.VideoReference;
import com.marketinghub.salesvideo.dto.CreateVideoReferenceRequest;
import com.marketinghub.salesvideo.dto.VideoReferenceDto;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Gerencia a fila de vídeos externos usados como referência de aprendizado comercial. */
@Component
public class VideoReferenceService {
  private final VideoReferenceRepository repository;

  /** Inicializa o serviço com o repositório canônico de referências de vídeo. */
  public VideoReferenceService(VideoReferenceRepository repository) {
    this.repository = repository;
  }

  /** Lista vídeos de referência do tenant atual. */
  @Transactional(readOnly = true)
  public List<VideoReferenceDto> listReferences() {
    String tenantId = TenantContextHolder.requireTenant();
    return repository.findByTenantIdOrderByUpdatedAtDesc(tenantId).stream()
        .map(VideoReferenceService::toDto)
        .toList();
  }

  /** Cadastra um vídeo externo para entrar na fila de análise do estúdio. */
  @Transactional
  public VideoReferenceDto createReference(CreateVideoReferenceRequest request) {
    String tenantId = TenantContextHolder.requireTenant();
    VideoReference reference =
        VideoReference.builder()
            .tenantId(tenantId)
            .title(required(request.title(), "Título"))
            .sourceUrl(required(request.sourceUrl(), "URL do vídeo"))
            .sourcePlatform(trimToNull(request.sourcePlatform()))
            .niche(trimToNull(request.niche()))
            .funnelStage(trimToNull(request.funnelStage()))
            .primaryLearningGoal(required(request.primaryLearningGoal(), "Objetivo de aprendizado"))
            .successEvidence(trimToNull(request.successEvidence()))
            .createdBy(trimToNull(request.createdBy()))
            .build();
    return toDto(repository.save(reference));
  }

  /** Converte entidade persistida para contrato público da API. */
  private static VideoReferenceDto toDto(VideoReference reference) {
    return new VideoReferenceDto(
        reference.getId(),
        reference.getTenantId(),
        reference.getTitle(),
        reference.getSourceUrl(),
        reference.getSourcePlatform(),
        reference.getNiche(),
        reference.getFunnelStage(),
        reference.getPrimaryLearningGoal(),
        reference.getSuccessEvidence(),
        reference.getAnalysisNotes(),
        reference.getStatus(),
        reference.getCreatedBy(),
        reference.getAnalyzedAt(),
        reference.getCreatedAt(),
        reference.getUpdatedAt());
  }

  /** Normaliza texto obrigatório para gravação segura. */
  private static String required(String value, String fieldName) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST, fieldName + " é obrigatório no vídeo de referência");
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
}
