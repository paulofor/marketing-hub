package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.salesvideo.VideoReferenceRepository;
import com.marketinghub.salesvideo.VideoReference;
import com.marketinghub.salesvideo.VideoReferenceStatus;
import com.marketinghub.salesvideo.dto.AnalyzeVideoReferenceRequest;
import com.marketinghub.salesvideo.dto.CreateVideoReferenceRequest;
import com.marketinghub.salesvideo.dto.VideoReferenceDto;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetStorageService.StoredObject;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import com.marketinghub.storage.StorageException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** Gerencia a fila de vídeos externos usados como referência de aprendizado comercial. */
@Component
public class VideoReferenceService {
  private static final Logger log = LoggerFactory.getLogger(VideoReferenceService.class);

  private final VideoReferenceRepository repository;
  private final AssetStorageService storageService;
  private final VideoReferenceAnalysisPort analysisPort;

  /** Inicializa o serviço com repositório e storage canônicos de referências de vídeo. */
  public VideoReferenceService(
      VideoReferenceRepository repository,
      AssetStorageService storageService,
      VideoReferenceAnalysisPort analysisPort) {
    this.repository = repository;
    this.storageService = storageService;
    this.analysisPort = analysisPort;
  }

  /** Lista vídeos de referência do tenant atual. */
  @Transactional(readOnly = true)
  public List<VideoReferenceDto> listReferences() {
    String tenantId = TenantContextHolder.requireTenant();
    return repository.findByTenantIdOrderByUpdatedAtDesc(tenantId).stream()
        .map(VideoReferenceService::toDto)
        .toList();
  }

  /** Consulta um vídeo de referência garantindo isolamento por tenant. */
  @Transactional(readOnly = true)
  public VideoReferenceDto getReference(Long referenceId) {
    return toDto(loadReference(referenceId));
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
    VideoReference saved = repository.save(reference);
    analysisPort.enqueue(saved);
    return toDto(saved);
  }

  /** Registra análise comercial estruturada e libera o aprendizado para a tela de resultado. */
  @Transactional
  public VideoReferenceDto analyzeReference(
      Long referenceId, AnalyzeVideoReferenceRequest request) {
    VideoReference reference = loadReference(referenceId);
    analysisPort.assertManualContingencyAllowed(referenceId);
    reference.setAnalysisNotes(buildAnalysisNotes(reference, request));
    reference.setStatus(VideoReferenceStatus.ANALYZED);
    reference.setAnalyzedAt(Instant.now());
    reference.setCreatedBy(
        StringUtils.hasText(reference.getCreatedBy())
            ? reference.getCreatedBy()
            : trimToNull(request.analyzedBy()));
    return toDto(repository.save(reference));
  }

  /** Recebe upload do usuário e cadastra o arquivo na fila de análise do estúdio. */
  @Transactional
  public VideoReferenceDto uploadReference(
      MultipartFile file,
      String title,
      String sourcePlatform,
      String niche,
      String funnelStage,
      String primaryLearningGoal,
      String successEvidence,
      String createdBy) {
    validateVideoFile(file);
    String requiredTitle = required(title, "Título");
    String requiredGoal = required(primaryLearningGoal, "Objetivo de aprendizado");
    try {
      StoredObject stored =
          storageService.store(
              file,
              new AssetUploadContext(
                  AssetUploadCategory.SALES_VIDEO, null, null, "reference-analysis"));
      return createReference(
          new CreateVideoReferenceRequest(
              requiredTitle,
              stored.publicUrl(),
              StringUtils.hasText(sourcePlatform) ? sourcePlatform : "Upload",
              niche,
              funnelStage,
              requiredGoal,
              successEvidence,
              createdBy));
    } catch (IOException | StorageException ex) {
      log.error(
          "Falha ao armazenar vídeo de referência para análise title={} createdBy={}",
          title,
          createdBy,
          ex);
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "Não foi possível armazenar o vídeo enviado para análise");
    }
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

  /** Carrega um vídeo de referência garantindo isolamento por tenant. */
  private VideoReference loadReference(Long referenceId) {
    String tenantId = TenantContextHolder.requireTenant();
    VideoReference reference =
        repository
            .findById(referenceId)
            .orElseThrow(
                () ->
                    VideoModuleException.notFound(
                        VideoModuleErrorCode.PROFILE_NOT_FOUND,
                        "Vídeo de referência não encontrado: " + referenceId));
    if (!tenantId.equals(reference.getTenantId())) {
      throw VideoModuleException.notFound(
          VideoModuleErrorCode.PROFILE_NOT_FOUND,
          "Vídeo de referência não encontrado: " + referenceId);
    }
    return reference;
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

  /** Monta relatório em Markdown para manter compatibilidade com a tela por etapas. */
  private static String buildAnalysisNotes(
      VideoReference reference, AnalyzeVideoReferenceRequest request) {
    return """
        **Evidencias usadas**
        %s

        **Diagnostico comercial**
        %s

        **Analise por sequencia**
        %s

        **O que o sistema deve aprender desse video**
        %s

        **Melhorias acionaveis para usar em vendas**
        %s

        **Alternativas avaliadas**
        - Proximo movimento: %s
        1. Analisar manualmente apenas impressões gerais: baixo custo, mas pouco reaproveitavel.
        2. Esperar automação completa por IA: maior escala futura, mas atrasa o aprendizado comercial imediato.
        3. Registrar análise estruturada por evidência, funil, sequência, aprendizado e ação: melhor equilíbrio para vender mais agora.

        Escolhi a terceira abordagem porque transforma o vídeo "%s" em padrão reutilizável de criativo, roteiro, prova e CTA.

        Analisado por: %s
        """
        .formatted(
            required(request.evidence(), "Evidências usadas"),
            required(request.commercialDiagnosis(), "Diagnóstico comercial"),
            required(request.sequenceAnalysis(), "Análise por sequência"),
            required(request.systemLearnings(), "Aprendizados do sistema"),
            required(request.salesImprovements(), "Melhorias acionáveis"),
            required(request.operationalDecision(), "Decisão operacional"),
            reference.getTitle(),
            required(request.analyzedBy(), "Responsável pela análise"));
  }

  /** Normaliza strings vazias recebidas do frontend. */
  private static String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  /** Valida se o arquivo enviado é um vídeo aceito pela fila de análise. */
  private static void validateVideoFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST, "Arquivo de vídeo é obrigatório");
    }
    String contentType = file.getContentType();
    String filename = file.getOriginalFilename();
    boolean looksLikeVideo =
        StringUtils.hasText(contentType)
            && contentType.toLowerCase(Locale.ROOT).startsWith("video/");
    boolean hasKnownExtension =
        StringUtils.hasText(filename)
            && filename.toLowerCase(Locale.ROOT).matches(".*\\.(mp4|mov|webm|m4v)$");
    if (!looksLikeVideo && !hasKnownExtension) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST, "Envie um arquivo de vídeo MP4, MOV, WEBM ou M4V");
    }
  }
}
