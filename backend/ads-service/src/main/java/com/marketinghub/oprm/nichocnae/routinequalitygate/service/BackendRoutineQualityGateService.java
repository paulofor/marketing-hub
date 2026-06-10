package com.marketinghub.oprm.nichocnae.routinequalitygate.service;

import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.completeStageExecution.CompleteRoutineQualityGateRequest;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.completeStageExecution.CompleteRoutineQualityGateResponse;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.detailStageExecution.RoutineQualityGateDetailResponse;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.failStageExecution.FailRoutineQualityGateRequest;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.pending.RecordRoutineQualityGatePending;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceSnapshotRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsável por expor, persistir e consultar a avaliação da etapa sete do pipeline OPRM NichoCNAE. */
@Service
public class BackendRoutineQualityGateService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendRoutineQualityGateService.class);
  private static final String FAILED_STATUS = "FAILED";
  private static final String LIGHTLY_RESEARCHED_STATUS = "LIGHTLY_RESEARCHED";
  private static final String NEEDS_MORE_RESEARCH_STATUS = "NEEDS_MORE_RESEARCH";
  private static final String GENERIC_STATUS = "GENERIC";
  private static final int MAX_PENDING = 10;
  private static final int MAX_NOTES_LENGTH = 4000;

  private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;
  private final OprmNicheRoutineCardRepository routineCardRepository;
  private final OprmExtractedSignalRepository extractedSignalRepository;
  private final OprmSourceSnapshotRepository sourceSnapshotRepository;
  private final OprmMeiAudienceProfileRepository meiAudienceProfileRepository;

  /** Inicializa o serviço com os repositórios canônicos necessários para calcular o contexto da qualidade. */
  public BackendRoutineQualityGateService(
      OprmRoutineResearchCycleRepository routineResearchCycleRepository,
      OprmNicheRoutineCardRepository routineCardRepository,
      OprmExtractedSignalRepository extractedSignalRepository,
      OprmSourceSnapshotRepository sourceSnapshotRepository,
      OprmMeiAudienceProfileRepository meiAudienceProfileRepository) {
    this.routineResearchCycleRepository = routineResearchCycleRepository;
    this.routineCardRepository = routineCardRepository;
    this.extractedSignalRepository = extractedSignalRepository;
    this.sourceSnapshotRepository = sourceSnapshotRepository;
    this.meiAudienceProfileRepository = meiAudienceProfileRepository;
  }

  /** Lista cartões sintetizados ainda não avaliados com contadores concretos de fontes e sinais. */
  @Transactional(readOnly = true)
  public List<RecordRoutineQualityGatePending> listPending() {
    return routineCardRepository.findByQualityCheckedAtIsNullOrderByCreatedAtAscIdAsc(PageRequest.of(0, MAX_PENDING)).stream()
        .filter(card -> meiAudienceProfileRepository.existsByResearchCycleId(card.getResearchCycleId()))
        .map(this::toPending)
        .toList();
  }

  /** Persiste a decisão de qualidade da etapa sete e atualiza o status do ciclo conforme o resultado. */
  @Transactional
  public CompleteRoutineQualityGateResponse complete(Long researchCycleId, CompleteRoutineQualityGateRequest request) {
    try {
      validateCompletionRequest(researchCycleId, request);
      OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
      OprmNicheRoutineCard card = routineCardRepository.findById(request.routineCardId())
          .orElseThrow(() -> new EntityNotFoundException("Routine card not found: " + request.routineCardId()));
      if (!card.getResearchCycleId().equals(researchCycleId)) {
        throw new IllegalArgumentException("routineCardId must belong to researchCycleId");
      }
      String qualityStatus = normalizeQualityStatus(request.qualityStatus());
      Instant now = Instant.now();
      card.setQualityStatus(qualityStatus);
      card.setReadyForHypothesis(Boolean.TRUE.equals(request.readyForHypothesis()));
      card.setSpecificityScore(request.specificityScore());
      card.setConfidenceScore(request.confidenceScore());
      card.setDuplicationScore(request.duplicationScore());
      card.setQualityNotes(trimOptional(request.qualityNotes()));
      card.setQualityCheckedBy(defaultText(request.checkedBy(), "oprmRoutineQualityGate"));
      card.setQualityCheckedAt(now);
      routineCardRepository.save(card);
      cycle.setStatus(qualityStatus);
      cycle.setUpdatedAt(now);
      if (LIGHTLY_RESEARCHED_STATUS.equals(qualityStatus)) {
        cycle.setFinishedAt(now);
        cycle.setErrorMessage(null);
      }
      routineResearchCycleRepository.save(cycle);
      return new CompleteRoutineQualityGateResponse(
          card.getId(), cycle.getId(), cycle.getStatus(), qualityStatus, card.getReadyForHypothesis(),
          card.getSpecificityScore(), card.getConfidenceScore(), card.getDuplicationScore(), card.getQualityCheckedAt());
    } catch (RuntimeException ex) {
      LOGGER.error(
          "Erro ao concluir etapa sete do OPRM nichocnae (researchCycleId={}, routineCardId={}, qualityStatus={})",
          researchCycleId,
          request == null ? null : request.routineCardId(),
          request == null ? null : request.qualityStatus(),
          ex);
      throw ex;
    }
  }

  /** Registra falha operacional da avaliação de qualidade no ciclo para investigação posterior. */
  @Transactional
  public void fail(Long researchCycleId, FailRoutineQualityGateRequest request) {
    try {
      OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
      Instant now = Instant.now();
      cycle.setStatus(FAILED_STATUS);
      cycle.setErrorMessage(requiredText(request == null ? null : request.errorMessage(), "errorMessage"));
      cycle.setFinishedAt(now);
      cycle.setUpdatedAt(now);
      routineResearchCycleRepository.save(cycle);
    } catch (RuntimeException ex) {
      LOGGER.error("Erro ao registrar falha da etapa sete do OPRM nichocnae (researchCycleId={})", researchCycleId, ex);
      throw ex;
    }
  }

  /** Detalha a decisão de qualidade já persistida para um ciclo de pesquisa. */
  @Transactional(readOnly = true)
  public RoutineQualityGateDetailResponse detail(Long researchCycleId) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    OprmNicheRoutineCard card = routineCardRepository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId).orElse(null);
    return new RoutineQualityGateDetailResponse(
        cycle.getId(),
        cycle.getStatus(),
        card == null ? null : card.getId(),
        card == null ? null : card.getQualityStatus(),
        card == null ? null : card.getReadyForHypothesis(),
        card == null ? null : card.getSpecificityScore(),
        card == null ? null : card.getConfidenceScore(),
        card == null ? null : card.getDuplicationScore(),
        card == null ? null : card.getRoutineEvidenceScore(),
        card == null ? null : card.getDifficultyEvidenceScore(),
        card == null ? null : card.getSourceDiversityScore(),
        card == null ? null : card.getSolutionLanguageRiskScore(),
        card == null ? null : card.getQualityNotes(),
        card == null ? null : card.getQualityCheckedBy(),
        card == null ? null : card.getQualityCheckedAt());
  }

  /** Converte um cartão persistido na unidade de trabalho usada pelo coletor da etapa sete. */
  private RecordRoutineQualityGatePending toPending(OprmNicheRoutineCard card) {
    List<OprmSourceSnapshot> snapshots = sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(card.getResearchCycleId());
    List<OprmExtractedSignal> signals = extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(card.getResearchCycleId());
    return new RecordRoutineQualityGatePending(
        card.getId(),
        card.getResearchCycleId(),
        card.getNicheName(),
        card.getRoutineSummary(),
        card.getPainsSummary(),
        card.getResultsSummary(),
        card.getMechanismOpportunitiesSummary(),
        card.getEvidenceSummary(),
        card.getSourceDomains(),
        card.getConfidenceScore(),
        snapshots.size(),
        signals.size(),
        countSignals(signals, "QUESTION_SIGNAL", "CUSTOMER_QUESTION", "NICHE_OWNER_QUESTION", "FINAL_CUSTOMER_QUESTION"),
        countSignals(signals, "PAIN_SIGNAL", "PAIN_POINT"),
        countSignals(signals, "OPERATIONAL_FRICTION"),
        countSignals(signals, "MECHANISM_OPPORTUNITY"),
        countSignals(signals, "ROUTINE_TASK"),
        countSignals(signals, "COMMERCIAL_OBJECT", "COMMERCIAL_TASK"),
        countSignals(signals, "LANGUAGE_MARKER", "CONTEXT_MARKER", "SEASONALITY_MARKER"),
        countSignals(signals, "SOLUTION_LANGUAGE_RISK", "MECHANISM_OPPORTUNITY") + countSolutionRiskSnapshots(snapshots),
        card.getRoutineEvidenceScore(),
        card.getDifficultyEvidenceScore(),
        card.getSourceDiversityScore(),
        card.getSolutionLanguageRiskScore(),
        card.getCreatedAt());
  }

  /** Conta snapshots com risco explícito de linguagem de solução para reforçar a avaliação de contaminação. */
  private int countSolutionRiskSnapshots(List<OprmSourceSnapshot> snapshots) {
    return (int) snapshots.stream()
        .filter(snapshot -> Boolean.TRUE.equals(snapshot.getSolutionLanguageRisk()))
        .count();
  }

  /** Conta sinais por tipo de forma tolerante a variações de caixa no payload persistido. */
  private int countSignals(List<OprmExtractedSignal> signals, String... signalTypes) {
    return (int) signals.stream()
        .filter(signal -> matchesAnySignalType(signal, signalTypes))
        .count();
  }

  /** Verifica se o sinal pertence a qualquer tipo canônico ou legado informado. */
  private boolean matchesAnySignalType(OprmExtractedSignal signal, String... signalTypes) {
    for (String signalType : signalTypes) {
      if (signalType.equalsIgnoreCase(signal.getSignalType())) {
        return true;
      }
    }
    return false;
  }

  /** Localiza o ciclo de pesquisa ou falha com erro contratual quando ele não existe. */
  private OprmRoutineResearchCycle findCycle(Long researchCycleId) {
    return routineResearchCycleRepository
        .findById(researchCycleId)
        .orElseThrow(() -> new EntityNotFoundException("Routine research cycle not found: " + researchCycleId));
  }

  /** Valida o payload final da etapa sete antes de alterar o cartão e o ciclo. */
  private void validateCompletionRequest(Long researchCycleId, CompleteRoutineQualityGateRequest request) {
    if (researchCycleId == null) {
      throw new IllegalArgumentException("researchCycleId is required");
    }
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    if (!researchCycleId.equals(request.researchCycleId())) {
      throw new IllegalArgumentException("researchCycleId must match path");
    }
    if (request.routineCardId() == null) {
      throw new IllegalArgumentException("routineCardId is required");
    }
    normalizeQualityStatus(request.qualityStatus());
    if (request.readyForHypothesis() == null) {
      throw new IllegalArgumentException("readyForHypothesis is required");
    }
    validateScore(request.specificityScore(), "specificityScore");
    validateScore(request.confidenceScore(), "confidenceScore");
    validateScore(request.duplicationScore(), "duplicationScore");
    String notes = trimOptional(request.qualityNotes());
    if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
      throw new IllegalArgumentException("qualityNotes must contain at most " + MAX_NOTES_LENGTH + " characters");
    }
  }

  /** Valida uma pontuação percentual obrigatória entre zero e cem. */
  private void validateScore(Integer value, String fieldName) {
    if (value == null || value < 0 || value > 100) {
      throw new IllegalArgumentException(fieldName + " must be between 0 and 100");
    }
  }

  /** Normaliza e valida os únicos status finais permitidos pelo gate de qualidade. */
  private String normalizeQualityStatus(String value) {
    String normalized = requiredText(value, "qualityStatus").toUpperCase(Locale.ROOT);
    if (!LIGHTLY_RESEARCHED_STATUS.equals(normalized) && !NEEDS_MORE_RESEARCH_STATUS.equals(normalized) && !GENERIC_STATUS.equals(normalized)) {
      throw new IllegalArgumentException("qualityStatus must be LIGHTLY_RESEARCHED, NEEDS_MORE_RESEARCH or GENERIC");
    }
    return normalized;
  }

  /** Exige texto útil para campos obrigatórios do contrato da etapa sete. */
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

  /** Retorna texto opcional aparado ou nulo quando não existe conteúdo útil. */
  private String trimOptional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
