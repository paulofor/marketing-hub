package com.marketinghub.oprm.nichocnae.signalextractor.service;

import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.CompleteSignalExtractorRequest;
import com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.CompleteSignalExtractorResponse;
import com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.ExtractedSignalResponse;
import com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.SignalExtractionItemRequest;
import com.marketinghub.oprm.nichocnae.signalextractor.service.detailStageExecution.SignalExtractorDetailResponse;
import com.marketinghub.oprm.nichocnae.signalextractor.service.failStageExecution.FailSignalExtractorRequest;
import com.marketinghub.oprm.nichocnae.signalextractor.service.pending.RecordSignalExtractorPending;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
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

/** Responsável por persistir e consultar sinais estruturados extraídos na etapa cinco do OPRM NichoCNAE. */
@Service
public class BackendSignalExtractorService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendSignalExtractorService.class);
  private static final String FETCH_STATUS_COMPLETED = "COMPLETED";
  private static final String SIGNAL_STATUS_PENDING = "PENDING";
  private static final String SIGNAL_STATUS_COMPLETED = "COMPLETED";
  private static final String SIGNAL_STATUS_FAILED = "FAILED";
  private static final String CURRENT_STAGE_SIGNAL_EXTRACTOR = "signal-extractor";
  private static final String CURRENT_STAGE_ROUTINE_SYNTHESIZER = "routine-synthesizer";
  private static final String DEFAULT_CREATED_BY = "oprmSignalExtractor";
  private static final int MAX_PENDING = 30;
  private static final int MAX_SIGNALS_PER_SNAPSHOT = 16;
  private static final int MAX_SIGNAL_TEXT_LENGTH = 500;
  private static final int MAX_EVIDENCE_LENGTH = 1000;

  private final OprmSourceSnapshotRepository sourceSnapshotRepository;
  private final OprmExtractedSignalRepository extractedSignalRepository;
  private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;

  /** Inicializa o serviço com os repositórios canônicos usados pela etapa cinco. */
  public BackendSignalExtractorService(
      OprmSourceSnapshotRepository sourceSnapshotRepository,
      OprmExtractedSignalRepository extractedSignalRepository,
      OprmRoutineResearchCycleRepository routineResearchCycleRepository) {
    this.sourceSnapshotRepository = sourceSnapshotRepository;
    this.extractedSignalRepository = extractedSignalRepository;
    this.routineResearchCycleRepository = routineResearchCycleRepository;
  }

  /** Lista snapshots curtos coletados e ainda pendentes de extração de sinais. */
  @Transactional(readOnly = true)
  public List<RecordSignalExtractorPending> listPending() {
    return sourceSnapshotRepository
        .findPendingByStatusAndCycleStage(
            FETCH_STATUS_COMPLETED, SIGNAL_STATUS_PENDING, CURRENT_STAGE_SIGNAL_EXTRACTOR, PageRequest.of(0, MAX_PENDING))
        .stream()
        .filter(snapshot -> !extractedSignalRepository.existsBySourceSnapshotId(snapshot.getId()))
        .map(this::toPending)
        .toList();
  }

  /** Persiste os sinais extraídos para um snapshot curto e atualiza os totais do ciclo. */
  @Transactional
  public CompleteSignalExtractorResponse complete(Long sourceSnapshotId, CompleteSignalExtractorRequest request) {
    try {
      validateCompletionRequest(sourceSnapshotId, request);
      OprmSourceSnapshot snapshot = findSnapshot(sourceSnapshotId);
      validateSnapshotMatchesRequest(snapshot, request);
      OprmRoutineResearchCycle cycle = findCycle(snapshot.getResearchCycleId());
      if (extractedSignalRepository.existsBySourceSnapshotId(sourceSnapshotId)) {
        throw new IllegalStateException("sourceSnapshot already has extracted signals: " + sourceSnapshotId);
      }
      Instant now = Instant.now();
      request.signals().forEach(signal -> validateSignalItem(signal, snapshot));
      List<OprmExtractedSignal> persistedSignals = request.signals().stream()
          .map(signal -> createSignal(snapshot, request, signal, now))
          .map(extractedSignalRepository::save)
          .toList();
      snapshot.setSignalExtractionStatus(SIGNAL_STATUS_COMPLETED);
      snapshot.setSignalExtractionError(null);
      snapshot.setSignalExtractedAt(now);
      sourceSnapshotRepository.save(snapshot);
      cycle.setTotalExtractedSignals(countCycleSignals(cycle.getId(), persistedSignals.size()));
      if (sourceSnapshotRepository.countByResearchCycleIdAndSignalExtractionStatus(
              cycle.getId(), SIGNAL_STATUS_PENDING) == 0) {
        cycle.setCurrentStageCode(CURRENT_STAGE_ROUTINE_SYNTHESIZER);
      }
      cycle.setUpdatedAt(now);
      routineResearchCycleRepository.save(cycle);
      List<ExtractedSignalResponse> signalResponses = persistedSignals.stream().map(this::toSignalResponse).toList();
      return new CompleteSignalExtractorResponse(
          snapshot.getId(),
          snapshot.getResearchCycleId(),
          snapshot.getSignalExtractionStatus(),
          signalResponses.size(),
          cycle.getTotalExtractedSignals(),
          signalResponses);
    } catch (RuntimeException ex) {
      LOGGER.error(
          "Erro ao concluir etapa cinco do OPRM nichocnae (sourceSnapshotId={}, researchCycleId={}, signalCount={})",
          sourceSnapshotId,
          request == null ? null : request.researchCycleId(),
          request == null || request.signals() == null ? null : request.signals().size(),
          ex);
      throw ex;
    }
  }

  /** Registra falha operacional de extração de sinais para permitir diagnóstico e reprocessamento controlado. */
  @Transactional
  public void fail(Long sourceSnapshotId, FailSignalExtractorRequest request) {
    try {
      OprmSourceSnapshot snapshot = findSnapshot(sourceSnapshotId);
      Instant now = Instant.now();
      snapshot.setSignalExtractionStatus(SIGNAL_STATUS_FAILED);
      snapshot.setSignalExtractionError(requiredText(request == null ? null : request.errorMessage(), "errorMessage"));
      snapshot.setSignalExtractedAt(now);
      sourceSnapshotRepository.save(snapshot);
    } catch (RuntimeException ex) {
      LOGGER.error("Erro ao registrar falha da etapa cinco do OPRM nichocnae (sourceSnapshotId={})", sourceSnapshotId, ex);
      throw ex;
    }
  }

  /** Detalha os sinais já extraídos para um ciclo de pesquisa de rotina. */
  @Transactional(readOnly = true)
  public SignalExtractorDetailResponse detail(Long researchCycleId) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    List<ExtractedSignalResponse> signals = extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(researchCycleId)
        .stream()
        .map(this::toSignalResponse)
        .toList();
    return new SignalExtractorDetailResponse(
        cycle.getId(), cycle.getStatus(), cycle.getTotalSourceSnapshots(), cycle.getTotalExtractedSignals(), signals);
  }

  /** Localiza o snapshot curto ou falha com erro de contrato quando ele não existe. */
  private OprmSourceSnapshot findSnapshot(Long sourceSnapshotId) {
    return sourceSnapshotRepository
        .findById(sourceSnapshotId)
        .orElseThrow(() -> new EntityNotFoundException("Source snapshot not found: " + sourceSnapshotId));
  }

  /** Localiza o ciclo de pesquisa ou falha com erro de contrato quando ele não existe. */
  private OprmRoutineResearchCycle findCycle(Long researchCycleId) {
    return routineResearchCycleRepository
        .findById(researchCycleId)
        .orElseThrow(() -> new EntityNotFoundException("Routine research cycle not found: " + researchCycleId));
  }

  /** Valida o payload de conclusão para garantir sinais úteis e em quantidade controlada. */
  private void validateCompletionRequest(Long sourceSnapshotId, CompleteSignalExtractorRequest request) {
    if (sourceSnapshotId == null) {
      throw new IllegalArgumentException("sourceSnapshotId is required");
    }
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    if (request.researchCycleId() == null) {
      throw new IllegalArgumentException("researchCycleId is required");
    }
    if (request.sourceCandidateId() == null) {
      throw new IllegalArgumentException("sourceCandidateId is required");
    }
    requiredText(request.sourceDomain(), "sourceDomain");
    requiredText(request.extractionStatus(), "extractionStatus");
    if (request.signals() == null || request.signals().isEmpty()) {
      throw new IllegalArgumentException("signals must contain at least one item");
    }
    if (request.signals().size() > MAX_SIGNALS_PER_SNAPSHOT) {
      throw new IllegalArgumentException("signals must contain at most " + MAX_SIGNALS_PER_SNAPSHOT + " items");
    }
  }

  /** Confere se os sinais recebidos pertencem exatamente ao snapshot processado pelo worker. */
  private void validateSnapshotMatchesRequest(OprmSourceSnapshot snapshot, CompleteSignalExtractorRequest request) {
    if (!snapshot.getResearchCycleId().equals(request.researchCycleId())) {
      throw new IllegalArgumentException("researchCycleId must match source snapshot");
    }
    if (!snapshot.getSourceCandidateId().equals(request.sourceCandidateId())) {
      throw new IllegalArgumentException("sourceCandidateId must match source snapshot");
    }
    if (!snapshot.getSourceDomain().equals(requiredText(request.sourceDomain(), "sourceDomain"))) {
      throw new IllegalArgumentException("sourceDomain must match source snapshot domain");
    }
  }

  /** Valida um sinal individual para bloquear textos vazios, payloads grandes ou evidência fora do snapshot. */
  private void validateSignalItem(SignalExtractionItemRequest signal, OprmSourceSnapshot snapshot) {
    if (signal == null) {
      throw new IllegalArgumentException("signal item is required");
    }
    String signalText = requiredText(signal.signalText(), "signalText");
    String evidence = requiredText(signal.evidenceExcerpt(), "evidenceExcerpt");
    requiredText(signal.signalType(), "signalType");
    if (signalText.length() > MAX_SIGNAL_TEXT_LENGTH) {
      throw new IllegalArgumentException("signalText must contain at most " + MAX_SIGNAL_TEXT_LENGTH + " characters");
    }
    if (evidence.length() > MAX_EVIDENCE_LENGTH) {
      throw new IllegalArgumentException("evidenceExcerpt must contain at most " + MAX_EVIDENCE_LENGTH + " characters");
    }
    if (!isExactEvidenceSpan(snapshot, evidence)) {
      throw new IllegalArgumentException("evidenceExcerpt must be an exact span from sourceTitle, snippet or shortExcerpt");
    }
    if (isPositiveSignal(signal.signalType()) && hasActorContextMismatch(evidence)) {
      throw new IllegalArgumentException("signal actor/context does not match the target executor");
    }
    Integer confidence = signal.confidenceScore();
    if (confidence == null || confidence < 0 || confidence > 100) {
      throw new IllegalArgumentException("confidenceScore must be between 0 and 100");
    }
  }


  /** Confere se a evidência enviada é trecho literal de um dos campos persistidos do snapshot. */
  private boolean isExactEvidenceSpan(OprmSourceSnapshot snapshot, String evidence) {
    return containsExactSpan(snapshot.getSourceTitle(), evidence)
        || containsExactSpan(snapshot.getSnippet(), evidence)
        || containsExactSpan(snapshot.getShortExcerpt(), evidence);
  }

  /** Verifica se o campo persistido contém literalmente o trecho de evidência aprovado. */
  private boolean containsExactSpan(String sourceText, String evidence) {
    return StringUtils.hasText(sourceText) && sourceText.contains(evidence);
  }

  /** Identifica sinais positivos que não podem nascer de ator ou contexto adjacente. */
  private boolean isPositiveSignal(String signalType) {
    String normalized = signalType == null ? "" : signalType.trim().toUpperCase(java.util.Locale.ROOT);
    return !normalized.endsWith("_RISK") && !normalized.equals("SEMANTIC_CONTEXT_MISMATCH");
  }

  /** Detecta evidências de ator ou ocupação adjacente que não provam a rotina do executor alvo. */
  private boolean hasActorContextMismatch(String evidence) {
    String normalized = evidence.toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("companhia aérea")
        || normalized.contains("voo cancelado")
        || normalized.contains("cancelamento de voo")
        || normalized.contains("motorista de aplicativo")
        || normalized.contains("entregador de aplicativo")
        || normalized.contains("ifood")
        || normalized.contains("uber")
        || normalized.contains("degustador de grãos")
        || normalized.contains("personal shopper")
        || normalized.contains("revendedora plus size");
  }

  /** Cria uma entidade de sinal preservando somente campos contratuais e evidência curta. */
  private OprmExtractedSignal createSignal(
      OprmSourceSnapshot snapshot,
      CompleteSignalExtractorRequest request,
      SignalExtractionItemRequest signal,
      Instant now) {
    OprmExtractedSignal extractedSignal = new OprmExtractedSignal();
    extractedSignal.setResearchCycleId(snapshot.getResearchCycleId());
    extractedSignal.setSourceSnapshotId(snapshot.getId());
    extractedSignal.setSourceCandidateId(snapshot.getSourceCandidateId());
    extractedSignal.setSignalType(requiredText(signal.signalType(), "signalType"));
    extractedSignal.setSignalText(requiredText(signal.signalText(), "signalText"));
    extractedSignal.setEvidenceExcerpt(requiredText(signal.evidenceExcerpt(), "evidenceExcerpt"));
    extractedSignal.setSourceDomain(snapshot.getSourceDomain());
    extractedSignal.setConfidenceScore(signal.confidenceScore());
    extractedSignal.setCreatedBy(defaultText(request.createdBy(), DEFAULT_CREATED_BY));
    extractedSignal.setCreatedAt(now);
    return extractedSignal;
  }

  /** Calcula o total de sinais extraídos do ciclo após a persistência atual. */
  private Integer countCycleSignals(Long researchCycleId, int fallbackCurrentSignalCount) {
    List<OprmExtractedSignal> signals = extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(researchCycleId);
    return signals.isEmpty() ? fallbackCurrentSignalCount : signals.size();
  }

  /** Converte um snapshot curto para o contrato interno de unidade de trabalho da etapa cinco. */
  private RecordSignalExtractorPending toPending(OprmSourceSnapshot snapshot) {
    return new RecordSignalExtractorPending(
        snapshot.getId(),
        snapshot.getResearchCycleId(),
        snapshot.getSourceCandidateId(),
        snapshot.getSourceUrl(),
        snapshot.getSourceDomain(),
        snapshot.getSourceTitle(),
        snapshot.getSourceType(),
        snapshot.getSnippet(),
        snapshot.getShortExcerpt(),
        snapshot.getFetchedAt(),
        snapshot.getFetchStatus(),
        snapshot.getHttpStatus(),
        snapshot.getStoragePolicy(),
        snapshot.getLicenseState(),
        snapshot.getSignalExtractionStatus(),
        snapshot.getCreatedAt());
  }

  /** Converte uma entidade de sinal para o contrato de resposta da etapa cinco. */
  private ExtractedSignalResponse toSignalResponse(OprmExtractedSignal signal) {
    return new ExtractedSignalResponse(
        signal.getId(),
        signal.getResearchCycleId(),
        signal.getSourceSnapshotId(),
        signal.getSourceCandidateId(),
        signal.getSignalType(),
        signal.getSignalText(),
        signal.getEvidenceExcerpt(),
        signal.getSourceDomain(),
        signal.getConfidenceScore(),
        signal.getCreatedBy(),
        signal.getCreatedAt());
  }

  /** Exige texto útil para campos obrigatórios do contrato da etapa cinco. */
  private String requiredText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }

  /** Retorna texto normalizado ou valor padrão quando o campo opcional veio vazio. */
  private String defaultText(String value, String defaultValue) {
    return StringUtils.hasText(value) ? value.trim() : defaultValue;
  }
}
