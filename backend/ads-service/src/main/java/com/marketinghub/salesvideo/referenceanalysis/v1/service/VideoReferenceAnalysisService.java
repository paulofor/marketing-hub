package com.marketinghub.salesvideo.referenceanalysis.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.salesvideo.VideoReferenceAnalysisExecutionRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoReferenceRepository;
import com.marketinghub.salesvideo.VideoReference;
import com.marketinghub.salesvideo.VideoReferenceAnalysisExecution;
import com.marketinghub.salesvideo.VideoReferenceAnalysisStatus;
import com.marketinghub.salesvideo.VideoReferenceStatus;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.referenceanalysis.v1.service.complete.CompleteRequest;
import com.marketinghub.salesvideo.referenceanalysis.v1.service.execution.VideoReferenceAnalysisResponse;
import com.marketinghub.salesvideo.referenceanalysis.v1.service.fail.FailureRequest;
import com.marketinghub.salesvideo.referenceanalysis.v1.service.pending.Pending;
import com.marketinghub.salesvideo.service.VideoReferenceAnalysisPort;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Controla exclusivamente persistência, fila, lease e callbacks da análise automática. */
@Service
public class VideoReferenceAnalysisService implements VideoReferenceAnalysisPort {
  private static final Logger log = LoggerFactory.getLogger(VideoReferenceAnalysisService.class);
  private static final Duration LEASE = Duration.ofMinutes(20);
  private final VideoReferenceAnalysisExecutionRepository executionRepository;
  private final VideoReferenceRepository referenceRepository;
  private final ObjectMapper objectMapper;

  /** Inicializa a fonte de verdade da etapa com seus repositórios e serializador. */
  public VideoReferenceAnalysisService(
      VideoReferenceAnalysisExecutionRepository executionRepository,
      VideoReferenceRepository referenceRepository,
      ObjectMapper objectMapper) {
    this.executionRepository = executionRepository;
    this.referenceRepository = referenceRepository;
    this.objectMapper = objectMapper;
  }

  /** Cria a primeira tentativa automática imediatamente após cadastrar a referência. */
  @Transactional
  @Override
  public VideoReferenceAnalysisExecution enqueue(VideoReference reference) {
    return createExecution(reference, nextAttempt(reference.getId()));
  }

  /** Reenfileira pela tela somente quando não existe execução ativa para a referência. */
  @Transactional
  public VideoReferenceAnalysisResponse retry(Long referenceId) {
    VideoReference reference = ownedReference(referenceId);
    executionRepository
        .findFirstByTenantIdAndReferenceIdOrderByIdDesc(reference.getTenantId(), referenceId)
        .filter(
            latest ->
                latest.getStatus() == VideoReferenceAnalysisStatus.QUEUED
                    || latest.getStatus() == VideoReferenceAnalysisStatus.RUNNING)
        .ifPresent(
            ignored -> {
              throw VideoModuleException.badRequest(
                  VideoModuleErrorCode.BAD_REQUEST, "A análise desta referência já está ativa");
            });
    reference.setStatus(VideoReferenceStatus.QUEUED);
    referenceRepository.save(reference);
    return response(createExecution(reference, nextAttempt(referenceId)));
  }

  /** Entrega uma única pendência com lease, recuperando execuções abandonadas sem sobreposição. */
  @Transactional
  public List<Pending> claimPending(String workerId) {
    List<VideoReferenceAnalysisExecution> claimable =
        executionRepository.findClaimable(
            VideoReferenceAnalysisStatus.QUEUED,
            VideoReferenceAnalysisStatus.RUNNING,
            Instant.now().minus(LEASE),
            PageRequest.of(0, 1));
    if (claimable.isEmpty()) {
      return List.of();
    }
    VideoReferenceAnalysisExecution execution = claimable.getFirst();
    Instant now = Instant.now();
    execution.setStatus(VideoReferenceAnalysisStatus.RUNNING);
    execution.setWorkerId(required(workerId, "workerId"));
    execution.setProducerExecutionId(UUID.randomUUID().toString());
    execution.setClaimedAt(now);
    execution.setStartedAt(now);
    execution.setFinishedAt(null);
    execution.setError(null);
    VideoReference reference = reference(execution.getReferenceId());
    reference.setStatus(VideoReferenceStatus.ANALYZING);
    referenceRepository.save(reference);
    VideoReferenceAnalysisExecution saved = executionRepository.save(execution);
    return List.of(
        new Pending(
            saved.getId(),
            saved.getReferenceId(),
            saved.getTenantId(),
            saved.getAttemptNumber(),
            saved.getProducerExecutionId(),
            readJson(saved.getInputJson()),
            saved.getClaimedAt()));
  }

  /** Persiste resultado funcional, auditoria da IA, artefatos e tokens no mesmo callback. */
  @Transactional
  public VideoReferenceAnalysisResponse complete(Long executionId, CompleteRequest request) {
    VideoReferenceAnalysisExecution execution = active(executionId, request.producerExecutionId());
    execution.setOutputJson(writeJson(request.output()));
    execution.setArtifactsJson(writeJson(request.artifacts()));
    execution.setRawRequestJson(writeJson(request.rawRequest()));
    execution.setRawResponseJson(writeJson(request.rawResponse()));
    execution.setModel(request.model());
    execution.setInputTokens(request.inputTokens());
    execution.setCachedInputTokens(request.cachedInputTokens());
    execution.setOutputTokens(request.outputTokens());
    execution.setCostUsd(request.costUsd());
    execution.setDecision(request.decision());
    execution.setStatus(VideoReferenceAnalysisStatus.COMPLETED);
    execution.setFinishedAt(Instant.now());
    VideoReference reference = reference(execution.getReferenceId());
    reference.setAnalysisNotes(request.summaryMarkdown());
    reference.setStatus(VideoReferenceStatus.ANALYZED);
    reference.setAnalyzedAt(Instant.now());
    referenceRepository.save(reference);
    return response(executionRepository.save(execution));
  }

  /** Persiste falha completa e devolve a referência para um estado visível e recuperável. */
  @Transactional
  public VideoReferenceAnalysisResponse fail(Long executionId, FailureRequest request) {
    VideoReferenceAnalysisExecution execution = active(executionId, request.producerExecutionId());
    execution.setArtifactsJson(writeNullable(request.artifacts()));
    execution.setRawRequestJson(writeNullable(request.rawRequest()));
    execution.setRawResponseJson(writeNullable(request.rawResponse()));
    execution.setModel(request.model());
    execution.setError(request.error());
    execution.setStatus(VideoReferenceAnalysisStatus.FAILED);
    execution.setFinishedAt(Instant.now());
    VideoReference reference = reference(execution.getReferenceId());
    reference.setStatus(VideoReferenceStatus.REJECTED);
    referenceRepository.save(reference);
    return response(executionRepository.save(execution));
  }

  /** Consulta o último resultado auditável garantindo isolamento pelo tenant atual. */
  @Transactional(readOnly = true)
  public VideoReferenceAnalysisResponse latest(Long referenceId) {
    VideoReference reference = ownedReference(referenceId);
    return executionRepository
        .findFirstByTenantIdAndReferenceIdOrderByIdDesc(reference.getTenantId(), referenceId)
        .map(this::response)
        .orElseThrow(
            () ->
                VideoModuleException.notFound(
                    VideoModuleErrorCode.PROFILE_NOT_FOUND,
                    "Execução de análise não encontrada: " + referenceId));
  }

  /** Bloqueia a contingência manual enquanto a fonte automática estiver ativa ou concluída. */
  @Transactional(readOnly = true)
  @Override
  public void assertManualContingencyAllowed(Long referenceId) {
    VideoReference reference = ownedReference(referenceId);
    executionRepository
        .findFirstByTenantIdAndReferenceIdOrderByIdDesc(reference.getTenantId(), referenceId)
        .filter(latest -> latest.getStatus() != VideoReferenceAnalysisStatus.FAILED)
        .ifPresent(
            ignored -> {
              throw VideoModuleException.badRequest(
                  VideoModuleErrorCode.BAD_REQUEST,
                  "A contingência manual só pode ser usada após falha da análise automática");
            });
  }

  /** Monta e persiste uma tentativa com snapshot imutável da entrada comercial. */
  private VideoReferenceAnalysisExecution createExecution(VideoReference reference, int attempt) {
    VideoReferenceAnalysisExecution execution = new VideoReferenceAnalysisExecution();
    execution.setReferenceId(reference.getId());
    execution.setTenantId(reference.getTenantId());
    execution.setStatus(VideoReferenceAnalysisStatus.QUEUED);
    execution.setAttemptNumber(attempt);
    execution.setInputJson(
        writeJson(
            objectMapper.valueToTree(
                new AnalysisInput(
                    reference.getId(),
                    reference.getTitle(),
                    reference.getSourceUrl(),
                    reference.getSourcePlatform(),
                    reference.getNiche(),
                    reference.getFunnelStage(),
                    reference.getPrimaryLearningGoal(),
                    reference.getSuccessEvidence()))));
    return executionRepository.save(execution);
  }

  /** Resolve o próximo número monotônico de tentativa da referência. */
  private int nextAttempt(Long referenceId) {
    return executionRepository
            .findFirstByReferenceIdOrderByAttemptNumberDesc(referenceId)
            .map(VideoReferenceAnalysisExecution::getAttemptNumber)
            .orElse(0)
        + 1;
  }

  /** Valida lease e correlação para impedir callback antigo de sobrescrever resultado atual. */
  private VideoReferenceAnalysisExecution active(Long id, String producerExecutionId) {
    VideoReferenceAnalysisExecution execution =
        executionRepository
            .findById(id)
            .orElseThrow(
                () ->
                    VideoModuleException.notFound(
                        VideoModuleErrorCode.PROFILE_NOT_FOUND, "Execução não encontrada: " + id));
    if (execution.getStatus() != VideoReferenceAnalysisStatus.RUNNING
        || !required(producerExecutionId, "producerExecutionId")
            .equals(execution.getProducerExecutionId())) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST,
          "Callback antigo ou execução fora do estado RUNNING: " + id);
    }
    return execution;
  }

  /**
   * Carrega referência por identificador sem aplicar contexto administrativo ao callback interno.
   */
  private VideoReference reference(Long referenceId) {
    return referenceRepository
        .findById(referenceId)
        .orElseThrow(
            () ->
                VideoModuleException.notFound(
                    VideoModuleErrorCode.PROFILE_NOT_FOUND,
                    "Vídeo de referência não encontrado: " + referenceId));
  }

  /** Carrega referência e valida isolamento por tenant para ações da tela. */
  private VideoReference ownedReference(Long referenceId) {
    VideoReference reference = reference(referenceId);
    if (!TenantContextHolder.requireTenant().equals(reference.getTenantId())) {
      throw VideoModuleException.notFound(
          VideoModuleErrorCode.PROFILE_NOT_FOUND,
          "Vídeo de referência não encontrado: " + referenceId);
    }
    return reference;
  }

  /** Converte entidade em relatório público sem expor request ou response bruto da IA. */
  private VideoReferenceAnalysisResponse response(VideoReferenceAnalysisExecution execution) {
    return new VideoReferenceAnalysisResponse(
        execution.getId(),
        execution.getReferenceId(),
        execution.getAttemptNumber(),
        execution.getStatus(),
        readJson(execution.getInputJson()),
        readNullable(execution.getOutputJson()),
        readNullable(execution.getArtifactsJson()),
        execution.getModel(),
        execution.getInputTokens(),
        execution.getCachedInputTokens(),
        execution.getOutputTokens(),
        execution.getCostUsd(),
        execution.getDecision(),
        execution.getError(),
        execution.getStartedAt(),
        execution.getFinishedAt(),
        execution.getCreatedAt(),
        execution.getUpdatedAt());
  }

  /** Serializa JSON para persistência auditável sem mutar o conteúdo produzido. */
  private String writeJson(JsonNode value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao serializar JSON da análise de referência", ex);
      throw new IllegalStateException("Não foi possível serializar a análise de referência", ex);
    }
  }

  /** Serializa JSON opcional preservando ausência real de evidência. */
  private String writeNullable(JsonNode value) {
    return value == null || value.isNull() ? null : writeJson(value);
  }

  /** Desserializa JSON obrigatório persistido na execução. */
  private JsonNode readJson(String value) {
    try {
      return objectMapper.readTree(value);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao desserializar JSON persistido da análise de referência", ex);
      throw new IllegalStateException("JSON persistido da análise de referência é inválido", ex);
    }
  }

  /** Desserializa JSON opcional persistido na execução. */
  private JsonNode readNullable(String value) {
    return value == null ? null : readJson(value);
  }

  /** Valida textos operacionais obrigatórios dos contratos internos. */
  private String required(String value, String field) {
    if (value == null || value.isBlank()) {
      throw VideoModuleException.badRequest(
          VideoModuleErrorCode.BAD_REQUEST, field + " é obrigatório");
    }
    return value.trim();
  }

  /** Snapshot estável da referência entregue ao executor sem acesso direto ao banco. */
  private record AnalysisInput(
      Long referenceId,
      String title,
      String sourceUrl,
      String sourcePlatform,
      String niche,
      String funnelStage,
      String primaryLearningGoal,
      String successEvidence) {}
}
