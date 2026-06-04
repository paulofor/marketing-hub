package com.marketinghub.oprm.nichocnae.routinesynthesizer.service;

import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.completeStageExecution.CompleteRoutineSynthesizerRequest;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.completeStageExecution.CompleteRoutineSynthesizerResponse;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.detailStageExecution.RoutineCardResponse;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.detailStageExecution.RoutineSynthesizerDetailResponse;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.failStageExecution.FailRoutineSynthesizerRequest;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.pending.RecordRoutineSynthesizerPending;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.pending.SignalForRoutineSynthesis;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsável por disponibilizar, persistir e consultar a síntese de rotina da etapa seis do OPRM NichoCNAE. */
@Service
public class BackendRoutineSynthesizerService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendRoutineSynthesizerService.class);
  private static final String RUNNING_STATUS = "RUNNING";
  private static final String FAILED_STATUS = "FAILED";
  private static final String SYNTHESIZED_STATUS = "ROUTINE_SYNTHESIZED";
  private static final String DEFAULT_SYNTHESIZED_BY = "oprmRoutineSynthesizer";
  private static final int MAX_PENDING = 10;
  private static final int MAX_SIGNALS_PER_CYCLE = 80;
  private static final int MAX_SUMMARY_LENGTH = 4000;

  private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;
  private final OprmExtractedSignalRepository extractedSignalRepository;
  private final OprmNicheRoutineCardRepository routineCardRepository;

  /** Inicializa o serviço com os repositórios canônicos usados pela síntese de rotina. */
  public BackendRoutineSynthesizerService(
      OprmRoutineResearchCycleRepository routineResearchCycleRepository,
      OprmExtractedSignalRepository extractedSignalRepository,
      OprmNicheRoutineCardRepository routineCardRepository) {
    this.routineResearchCycleRepository = routineResearchCycleRepository;
    this.extractedSignalRepository = extractedSignalRepository;
    this.routineCardRepository = routineCardRepository;
  }

  /** Lista ciclos com sinais extraídos e sem cartão de rotina para processamento da etapa seis. */
  @Transactional(readOnly = true)
  public List<RecordRoutineSynthesizerPending> listPending() {
    return routineResearchCycleRepository.findByStatusOrderByStartedAtAsc(RUNNING_STATUS, PageRequest.of(0, MAX_PENDING)).stream()
        .filter(cycle -> !routineCardRepository.existsByResearchCycleId(cycle.getId()))
        .map(cycle -> new CycleWithSignals(cycle, extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(cycle.getId())))
        .filter(item -> !item.signals().isEmpty())
        .map(item -> toPending(item.cycle(), item.signals()))
        .toList();
  }

  /** Persiste o cartão de rotina sintetizado e marca o ciclo como sintetizado. */
  @Transactional
  public CompleteRoutineSynthesizerResponse complete(Long researchCycleId, CompleteRoutineSynthesizerRequest request) {
    try {
      validateCompletionRequest(researchCycleId, request);
      OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
      if (!cycle.getId().equals(request.researchCycleId())) {
        throw new IllegalArgumentException("researchCycleId must match path");
      }
      if (routineCardRepository.existsByResearchCycleId(researchCycleId)) {
        throw new IllegalStateException("research cycle already has a routine card: " + researchCycleId);
      }
      Instant now = Instant.now();
      OprmNicheRoutineCard card = new OprmNicheRoutineCard();
      card.setResearchCycleId(researchCycleId);
      card.setNicheName(requiredText(request.nicheName(), "nicheName"));
      card.setRoutineSummary(requiredText(request.routineSummary(), "routineSummary"));
      card.setPainsSummary(requiredText(request.painsSummary(), "painsSummary"));
      card.setResultsSummary(requiredText(request.resultsSummary(), "resultsSummary"));
      card.setMechanismOpportunitiesSummary(requiredText(request.mechanismOpportunitiesSummary(), "mechanismOpportunitiesSummary"));
      card.setEvidenceSummary(requiredText(request.evidenceSummary(), "evidenceSummary"));
      card.setSourceDomains(requiredText(request.sourceDomains(), "sourceDomains"));
      card.setConfidenceScore(request.confidenceScore());
      card.setSynthesizedBy(defaultText(request.synthesizedBy(), DEFAULT_SYNTHESIZED_BY));
      card.setCreatedAt(now);
      OprmNicheRoutineCard saved = routineCardRepository.save(card);
      cycle.setStatus(SYNTHESIZED_STATUS);
      cycle.setUpdatedAt(now);
      cycle.setErrorMessage(null);
      routineResearchCycleRepository.save(cycle);
      return new CompleteRoutineSynthesizerResponse(
          saved.getId(), saved.getResearchCycleId(), cycle.getStatus(), saved.getNicheName(), saved.getConfidenceScore(), saved.getCreatedAt());
    } catch (RuntimeException ex) {
      LOGGER.error(
          "Erro ao concluir etapa seis do OPRM nichocnae (researchCycleId={}, confidenceScore={})",
          researchCycleId,
          request == null ? null : request.confidenceScore(),
          ex);
      throw ex;
    }
  }

  /** Registra falha operacional da síntese de rotina no ciclo para diagnóstico de causa-raiz. */
  @Transactional
  public void fail(Long researchCycleId, FailRoutineSynthesizerRequest request) {
    try {
      OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
      Instant now = Instant.now();
      cycle.setStatus(FAILED_STATUS);
      cycle.setErrorMessage(requiredText(request == null ? null : request.errorMessage(), "errorMessage"));
      cycle.setFinishedAt(now);
      cycle.setUpdatedAt(now);
      routineResearchCycleRepository.save(cycle);
    } catch (RuntimeException ex) {
      LOGGER.error("Erro ao registrar falha da etapa seis do OPRM nichocnae (researchCycleId={})", researchCycleId, ex);
      throw ex;
    }
  }

  /** Detalha o cartão de rotina já sintetizado para um ciclo de pesquisa. */
  @Transactional(readOnly = true)
  public RoutineSynthesizerDetailResponse detail(Long researchCycleId) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    RoutineCardResponse routineCard = routineCardRepository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId)
        .map(this::toCardResponse)
        .orElse(null);
    return new RoutineSynthesizerDetailResponse(cycle.getId(), cycle.getStatus(), cycle.getTotalExtractedSignals(), routineCard);
  }

  /** Localiza o ciclo de pesquisa ou falha com erro de contrato quando ele não existe. */
  private OprmRoutineResearchCycle findCycle(Long researchCycleId) {
    return routineResearchCycleRepository
        .findById(researchCycleId)
        .orElseThrow(() -> new EntityNotFoundException("Routine research cycle not found: " + researchCycleId));
  }

  /** Valida o payload da etapa seis para bloquear cartões vazios ou confiança fora da escala. */
  private void validateCompletionRequest(Long researchCycleId, CompleteRoutineSynthesizerRequest request) {
    if (researchCycleId == null) {
      throw new IllegalArgumentException("researchCycleId is required");
    }
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    requiredText(request.nicheName(), "nicheName");
    validateSummary(request.routineSummary(), "routineSummary");
    validateSummary(request.painsSummary(), "painsSummary");
    validateSummary(request.resultsSummary(), "resultsSummary");
    validateSummary(request.mechanismOpportunitiesSummary(), "mechanismOpportunitiesSummary");
    validateSummary(request.evidenceSummary(), "evidenceSummary");
    requiredText(request.sourceDomains(), "sourceDomains");
    Integer confidence = request.confidenceScore();
    if (confidence == null || confidence < 0 || confidence > 100) {
      throw new IllegalArgumentException("confidenceScore must be between 0 and 100");
    }
  }

  /** Valida um texto sintético obrigatório com limite de tamanho operacional. */
  private void validateSummary(String value, String fieldName) {
    String text = requiredText(value, fieldName);
    if (text.length() > MAX_SUMMARY_LENGTH) {
      throw new IllegalArgumentException(fieldName + " must contain at most " + MAX_SUMMARY_LENGTH + " characters");
    }
  }

  /** Converte um ciclo com sinais persistidos para a unidade de trabalho da etapa seis. */
  private RecordRoutineSynthesizerPending toPending(OprmRoutineResearchCycle cycle, List<OprmExtractedSignal> signals) {
    List<SignalForRoutineSynthesis> selectedSignals = signals.stream()
        .sorted(Comparator.comparing(OprmExtractedSignal::getConfidenceScore).reversed().thenComparing(OprmExtractedSignal::getId))
        .limit(MAX_SIGNALS_PER_CYCLE)
        .map(this::toSignal)
        .toList();
    return new RecordRoutineSynthesizerPending(
        cycle.getId(),
        cycle.getSourceNicheId(),
        cycle.getCnaeCode(),
        cycle.getCnaeDescription(),
        cycle.getNicheName(),
        cycle.getSourceScore(),
        cycle.getStatus(),
        cycle.getTotalExtractedSignals(),
        cycle.getStartedAt(),
        selectedSignals);
  }

  /** Converte uma entidade de sinal em insumo contratual para síntese. */
  private SignalForRoutineSynthesis toSignal(OprmExtractedSignal signal) {
    return new SignalForRoutineSynthesis(
        signal.getId(),
        signal.getSourceSnapshotId(),
        signal.getSourceCandidateId(),
        signal.getSignalType(),
        signal.getSignalText(),
        signal.getEvidenceExcerpt(),
        signal.getSourceDomain(),
        signal.getConfidenceScore());
  }

  /** Converte a entidade de cartão para resposta pública da etapa seis. */
  private RoutineCardResponse toCardResponse(OprmNicheRoutineCard card) {
    return new RoutineCardResponse(
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
        card.getSynthesizedBy(),
        card.getCreatedAt());
  }

  /** Exige texto útil para campos obrigatórios do contrato da etapa seis. */
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

  /** Agrupa ciclo e sinais para evitar reconsultas na montagem do contrato pending. */
  private record CycleWithSignals(OprmRoutineResearchCycle cycle, List<OprmExtractedSignal> signals) {}
}
