package com.marketinghub.oprm.nichocnae.sourcefetcher.service;

import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceCandidate;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution.CompleteSourceFetcherRequest;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution.CompleteSourceFetcherResponse;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution.SourceSnapshotResponse;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.detailStageExecution.SourceFetcherDetailResponse;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.failStageExecution.FailSourceFetcherRequest;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.pending.RecordSourceFetcherPending;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceSnapshotRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsável por persistir os metadados e trechos curtos coletados na etapa quatro do OPRM nicho CNAE. */
@Service
public class BackendSourceFetcherService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendSourceFetcherService.class);
  private static final String CANDIDATE_STATUS_FOUND = "FOUND";
  private static final String CANDIDATE_STATUS_FETCHED = "FETCHED";
  private static final String CANDIDATE_STATUS_REJECTED = "REJECTED";
  private static final String DEFAULT_FETCH_STATUS = "COMPLETED";
  private static final String DEFAULT_SOURCE_TYPE = "PUBLIC_CONTENT";
  private static final String DEFAULT_SOURCE_CLASSIFICATION_TYPE = "OLD_OR_UNDATED_CONTENT";
  private static final String DEFAULT_STORAGE_POLICY = "SHORT_EXCERPT_ALLOWED";
  private static final String SIGNAL_STATUS_PENDING = "PENDING";
  private static final int MAX_PENDING = 30;
  private static final int MAX_SHORT_EXCERPT_LENGTH = 1200;

  private final OprmSourceCandidateRepository sourceCandidateRepository;
  private final OprmSourceSnapshotRepository sourceSnapshotRepository;
  private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;

  /** Inicializa o serviço com os repositórios canônicos da etapa quatro do pipeline. */
  public BackendSourceFetcherService(
      OprmSourceCandidateRepository sourceCandidateRepository,
      OprmSourceSnapshotRepository sourceSnapshotRepository,
      OprmRoutineResearchCycleRepository routineResearchCycleRepository) {
    this.sourceCandidateRepository = sourceCandidateRepository;
    this.sourceSnapshotRepository = sourceSnapshotRepository;
    this.routineResearchCycleRepository = routineResearchCycleRepository;
  }

  /** Lista fontes candidatas encontradas e ainda não selecionadas para coleta curta. */
  @Transactional(readOnly = true)
  public List<RecordSourceFetcherPending> listPending() {
    return sourceCandidateRepository
        .findByStatusAndSelectedForFetchFalseOrderByResearchCycleIdAscResearchQueryIdAscSearchPositionAscIdAsc(
            CANDIDATE_STATUS_FOUND, PageRequest.of(0, MAX_PENDING))
        .stream()
        .map(this::toPending)
        .toList();
  }

  /** Grava o snapshot curto coletado para uma fonte candidata selecionada. */
  @Transactional
  public CompleteSourceFetcherResponse complete(Long sourceCandidateId, CompleteSourceFetcherRequest request) {
    try {
      validateCompletionRequest(sourceCandidateId, request);
      OprmSourceCandidate candidate = findCandidate(sourceCandidateId);
      validateCandidateMatchesRequest(candidate, request);
      OprmRoutineResearchCycle cycle = findCycle(candidate.getResearchCycleId());
      if (sourceSnapshotRepository.existsBySourceCandidateId(sourceCandidateId)) {
        throw new IllegalStateException("sourceCandidate already has snapshot: " + sourceCandidateId);
      }
      Instant now = Instant.now();
      OprmSourceSnapshot snapshot = sourceSnapshotRepository.save(createSnapshot(candidate, request, now));
      candidate.setSelectedForFetch(true);
      candidate.setRelevanceScore(defaultInteger(request.relevanceScore(), 100));
      candidate.setRejectionReason(null);
      candidate.setStatus(CANDIDATE_STATUS_FETCHED);
      candidate.setUpdatedAt(now);
      sourceCandidateRepository.save(candidate);
      cycle.setTotalSourceSnapshots(countCycleSnapshots(cycle.getId(), 1));
      cycle.setUpdatedAt(now);
      routineResearchCycleRepository.save(cycle);
      return new CompleteSourceFetcherResponse(
          candidate.getId(),
          candidate.getResearchCycleId(),
          candidate.getSelectedForFetch(),
          candidate.getRelevanceScore(),
          cycle.getTotalSourceSnapshots(),
          toSnapshotResponse(snapshot));
    } catch (RuntimeException ex) {
      LOGGER.error(
          "Erro ao concluir etapa quatro do OPRM nichocnae (sourceCandidateId={}, httpStatus={}, fetchStatus={})",
          sourceCandidateId,
          request == null ? null : request.httpStatus(),
          request == null ? null : request.fetchStatus(),
          ex);
      throw ex;
    }
  }

  /** Registra rejeição ou falha de coleta sem salvar HTML completo da fonte candidata. */
  @Transactional
  public void fail(Long sourceCandidateId, FailSourceFetcherRequest request) {
    try {
      OprmSourceCandidate candidate = findCandidate(sourceCandidateId);
      Instant now = Instant.now();
      candidate.setSelectedForFetch(false);
      candidate.setRelevanceScore(request == null ? null : request.relevanceScore());
      candidate.setRejectionReason(requiredText(request == null ? null : request.rejectionReason(), "rejectionReason"));
      candidate.setStatus(CANDIDATE_STATUS_REJECTED);
      candidate.setUpdatedAt(now);
      sourceCandidateRepository.save(candidate);
    } catch (RuntimeException ex) {
      LOGGER.error("Erro ao registrar falha da etapa quatro do OPRM nichocnae (sourceCandidateId={})", sourceCandidateId, ex);
      throw ex;
    }
  }

  /** Detalha os snapshots curtos coletados para um ciclo de pesquisa de rotina. */
  @Transactional(readOnly = true)
  public SourceFetcherDetailResponse detail(Long researchCycleId) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    List<SourceSnapshotResponse> snapshots = sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(researchCycleId)
        .stream()
        .map(this::toSnapshotResponse)
        .toList();
    return new SourceFetcherDetailResponse(
        cycle.getId(), cycle.getStatus(), cycle.getTotalSourceCandidates(), cycle.getTotalSourceSnapshots(), snapshots);
  }

  /** Localiza uma fonte candidata ou falha com erro de contrato quando ela não existe. */
  private OprmSourceCandidate findCandidate(Long sourceCandidateId) {
    return sourceCandidateRepository
        .findById(sourceCandidateId)
        .orElseThrow(() -> new EntityNotFoundException("Source candidate not found: " + sourceCandidateId));
  }

  /** Localiza o ciclo de pesquisa ou falha com erro de contrato quando ele não existe. */
  private OprmRoutineResearchCycle findCycle(Long researchCycleId) {
    return routineResearchCycleRepository
        .findById(researchCycleId)
        .orElseThrow(() -> new EntityNotFoundException("Routine research cycle not found: " + researchCycleId));
  }

  /** Valida o payload de conclusão para bloquear HTML completo e exigir campos contratuais. */
  private void validateCompletionRequest(Long sourceCandidateId, CompleteSourceFetcherRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    requiredText(request.sourceUrl(), "sourceUrl");
    requiredText(request.sourceDomain(), "sourceDomain");
    requiredText(request.sourceTitle(), "sourceTitle");
    requiredText(request.sourceType(), "sourceType");
    requiredText(request.sourceIntent(), "sourceIntent");
    requiredScore(request.routineEvidenceScore(), "routineEvidenceScore");
    requiredBoolean(request.commercialPageRisk(), "commercialPageRisk");
    requiredBoolean(request.solutionLanguageRisk(), "solutionLanguageRisk");
    requiredText(request.fetchStatus(), "fetchStatus");
    requiredText(request.storagePolicy(), "storagePolicy");
    String shortExcerpt = requiredText(request.shortExcerpt(), "shortExcerpt");
    if (shortExcerpt.length() > MAX_SHORT_EXCERPT_LENGTH) {
      throw new IllegalArgumentException("shortExcerpt must contain at most " + MAX_SHORT_EXCERPT_LENGTH + " characters");
    }
    if (sourceCandidateId == null) {
      throw new IllegalArgumentException("sourceCandidateId is required");
    }
  }

  /** Confere se o snapshot recebido pertence exatamente à URL candidata selecionada. */
  private void validateCandidateMatchesRequest(OprmSourceCandidate candidate, CompleteSourceFetcherRequest request) {
    if (!candidate.getSourceUrl().equals(requiredText(request.sourceUrl(), "sourceUrl"))) {
      throw new IllegalArgumentException("sourceUrl must match source candidate URL");
    }
    if (!candidate.getSourceDomain().equals(requiredText(request.sourceDomain(), "sourceDomain"))) {
      throw new IllegalArgumentException("sourceDomain must match source candidate domain");
    }
  }

  /** Cria a entidade de snapshot preservando somente metadados e trechos curtos permitidos. */
  private OprmSourceSnapshot createSnapshot(
      OprmSourceCandidate candidate, CompleteSourceFetcherRequest request, Instant now) {
    OprmSourceSnapshot snapshot = new OprmSourceSnapshot();
    snapshot.setResearchCycleId(candidate.getResearchCycleId());
    snapshot.setSourceCandidateId(candidate.getId());
    snapshot.setSourceUrl(requiredText(request.sourceUrl(), "sourceUrl"));
    snapshot.setSourceDomain(requiredText(request.sourceDomain(), "sourceDomain"));
    snapshot.setSourceTitle(requiredText(request.sourceTitle(), "sourceTitle"));
    snapshot.setSourceType(defaultText(request.sourceType(), DEFAULT_SOURCE_TYPE));
    snapshot.setSourceIntent(requiredText(request.sourceIntent(), "sourceIntent"));
    snapshot.setRoutineEvidenceScore(requiredScore(request.routineEvidenceScore(), "routineEvidenceScore"));
    snapshot.setCommercialPageRisk(requiredBoolean(request.commercialPageRisk(), "commercialPageRisk"));
    snapshot.setSolutionLanguageRisk(requiredBoolean(request.solutionLanguageRisk(), "solutionLanguageRisk"));
    snapshot.setSourceClassificationType(defaultText(request.sourceClassificationType(), DEFAULT_SOURCE_CLASSIFICATION_TYPE));
    snapshot.setSourceFreshnessScore(normalizeOptionalScore(request.sourceFreshnessScore()));
    snapshot.setOutdatedSourceRisk(requiredBoolean(request.outdatedSourceRisk(), "outdatedSourceRisk"));
    snapshot.setBrazilRelevanceScore(normalizeOptionalScore(request.brazilRelevanceScore()));
    snapshot.setAutonomousProfessionalEvidenceScore(normalizeOptionalScore(request.autonomousProfessionalEvidenceScore()));
    snapshot.setStructuredBusinessDriftRisk(requiredBoolean(request.structuredBusinessDriftRisk(), "structuredBusinessDriftRisk"));
    snapshot.setPublishedAt(request.publishedAt());
    snapshot.setSnippet(trimToNull(request.snippet()));
    snapshot.setShortExcerpt(requiredText(request.shortExcerpt(), "shortExcerpt"));
    snapshot.setFetchedAt(now);
    snapshot.setFetchStatus(defaultText(request.fetchStatus(), DEFAULT_FETCH_STATUS));
    snapshot.setHttpStatus(request.httpStatus());
    snapshot.setStoragePolicy(defaultText(request.storagePolicy(), DEFAULT_STORAGE_POLICY));
    snapshot.setLicenseState(trimToNull(request.licenseState()));
    snapshot.setErrorMessage(null);
    snapshot.setSignalExtractionStatus(SIGNAL_STATUS_PENDING);
    snapshot.setSignalExtractionError(null);
    snapshot.setSignalExtractedAt(null);
    snapshot.setCreatedAt(now);
    return snapshot;
  }

  /** Calcula o total de snapshots do ciclo depois da persistência atual. */
  private Integer countCycleSnapshots(Long researchCycleId, int fallbackCurrentSnapshotCount) {
    List<OprmSourceSnapshot> snapshots = sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(researchCycleId);
    return snapshots.isEmpty() ? fallbackCurrentSnapshotCount : snapshots.size();
  }

  /** Converte uma candidata pendente para o contrato interno de unidade de trabalho da etapa quatro. */
  private RecordSourceFetcherPending toPending(OprmSourceCandidate candidate) {
    return new RecordSourceFetcherPending(
        candidate.getId(),
        candidate.getResearchCycleId(),
        candidate.getResearchQueryId(),
        candidate.getSourceUrl(),
        candidate.getSourceTitle(),
        candidate.getSourceSnippet(),
        candidate.getSourceDomain(),
        candidate.getSourceGroup(),
        defaultText(candidate.getSourceIntent(), candidate.getSourceGroup()),
        defaultInteger(candidate.getRoutineEvidenceScore(), candidate.getRelevanceScore()),
        Boolean.TRUE.equals(candidate.getCommercialPageRisk()),
        Boolean.TRUE.equals(candidate.getSolutionLanguageRisk()),
        candidate.getSourceClassificationType(),
        candidate.getSourceFreshnessScore(),
        Boolean.TRUE.equals(candidate.getOutdatedSourceRisk()),
        candidate.getBrazilRelevanceScore(),
        candidate.getAutonomousProfessionalEvidenceScore(),
        Boolean.TRUE.equals(candidate.getStructuredBusinessDriftRisk()),
        candidate.getPublishedAt(),
        candidate.getSearchProvider(),
        candidate.getSearchPosition(),
        candidate.getStatus(),
        candidate.getCreatedAt(),
        candidate.getUpdatedAt());
  }

  /** Converte uma entidade de snapshot para o contrato de resposta da etapa quatro. */
  private SourceSnapshotResponse toSnapshotResponse(OprmSourceSnapshot snapshot) {
    return new SourceSnapshotResponse(
        snapshot.getId(),
        snapshot.getResearchCycleId(),
        snapshot.getSourceCandidateId(),
        snapshot.getSourceUrl(),
        snapshot.getSourceDomain(),
        snapshot.getSourceTitle(),
        snapshot.getSourceType(),
        snapshot.getSourceIntent(),
        snapshot.getRoutineEvidenceScore(),
        Boolean.TRUE.equals(snapshot.getCommercialPageRisk()),
        Boolean.TRUE.equals(snapshot.getSolutionLanguageRisk()),
        snapshot.getSourceClassificationType(),
        snapshot.getSourceFreshnessScore(),
        Boolean.TRUE.equals(snapshot.getOutdatedSourceRisk()),
        snapshot.getBrazilRelevanceScore(),
        snapshot.getAutonomousProfessionalEvidenceScore(),
        Boolean.TRUE.equals(snapshot.getStructuredBusinessDriftRisk()),
        snapshot.getPublishedAt(),
        snapshot.getSnippet(),
        snapshot.getShortExcerpt(),
        snapshot.getFetchedAt(),
        snapshot.getFetchStatus(),
        snapshot.getHttpStatus(),
        snapshot.getStoragePolicy(),
        snapshot.getLicenseState(),
        snapshot.getErrorMessage(),
        snapshot.getCreatedAt());
  }

  /** Exige texto útil para campos obrigatórios do contrato da etapa quatro. */
  private String requiredText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }

  /** Normaliza escore opcional mantendo nulo quando a etapa anterior não informou indicador. */
  private Integer normalizeOptionalScore(Integer score) {
    return score == null ? null : Math.max(0, Math.min(100, score));
  }

  /** Retorna texto normalizado ou valor padrão quando o campo opcional veio vazio. */
  private String defaultText(String value, String defaultValue) {
    return StringUtils.hasText(value) ? value.trim() : defaultValue;
  }

  /** Normaliza campos opcionais de texto preservando nulo quando não há conteúdo útil. */
  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Exige pontuação de rotina no intervalo contratual de zero a cem. */
  private Integer requiredScore(Integer value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    if (value < 0 || value > 100) {
      throw new IllegalArgumentException(fieldName + " must be between 0 and 100");
    }
    return value;
  }

  /** Exige booleano explícito para preservar indicadores de risco da etapa três. */
  private Boolean requiredBoolean(Boolean value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value;
  }

  /** Retorna inteiro informado ou valor padrão para campos opcionais de pontuação. */
  private Integer defaultInteger(Integer value, Integer defaultValue) {
    return value == null ? defaultValue : value;
  }
}
