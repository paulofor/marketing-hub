package com.marketinghub.oprm.nichocnae.routinequalitygate.service;

import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.OprmMeiAudienceProfile;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
  private static final String CYCLE_STATUS_RUNNING = "RUNNING";
  private static final String LIGHTLY_RESEARCHED_STATUS = "LIGHTLY_RESEARCHED";
  private static final String MEI_AUDIENCE_READY_STATUS = "MEI_AUDIENCE_READY";
  private static final String CURRENT_STAGE_ROUTINE_QUALITY_GATE = "routine-quality-gate";
  private static final String CURRENT_STAGE_EVIDENCE_LEVEL_GATE = "evidence-level-gate";
  private static final String NEEDS_MORE_RESEARCH_STATUS = "NEEDS_MORE_RESEARCH";
  private static final String NEEDS_MORE_MEI_RESEARCH_STATUS = "NEEDS_MORE_MEI_RESEARCH";
  private static final String OUTDATED_SOURCES_STATUS = "OUTDATED_SOURCES";
  private static final String TOO_CORPORATE_STATUS = "TOO_CORPORATE";
  private static final String SOLUTION_CONTAMINATED_STATUS = "SOLUTION_CONTAMINATED";
  private static final String GENERIC_STATUS = "GENERIC";
  private static final String NEEDS_EXECUTOR_ROUTINE_EVIDENCE_STATUS = "NEEDS_EXECUTOR_ROUTINE_EVIDENCE";
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
    return routineCardRepository.findPendingRoutineQualityGate(PageRequest.of(0, MAX_PENDING)).stream()
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
      cycle.setCurrentStageCode(nextStageAfterQuality(qualityStatus));
      cycle.setUpdatedAt(now);
      cycle.setFinishedAt(now);
      if (isApprovedQualityStatus(qualityStatus)) {
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
      cycle.setCurrentStageCode(CURRENT_STAGE_ROUTINE_QUALITY_GATE);
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
        card == null ? null : parseQualityNotes(card.getQualityNotes()),
        card == null ? null : card.getQualityCheckedBy(),
        card == null ? null : card.getQualityCheckedAt());
  }

  /** Converte as notas legadas em chave/valor para um objeto JSON estável na consulta de detalhe. */
  private Map<String, Object> parseQualityNotes(String qualityNotes) {
    String notes = trimOptional(qualityNotes);
    if (notes == null) {
      return null;
    }
    Map<String, Object> parsed = new LinkedHashMap<>();
    for (String part : notes.split(";")) {
      String item = part.trim();
      if (!StringUtils.hasText(item)) {
        continue;
      }
      int separatorIndex = item.indexOf('=');
      if (separatorIndex <= 0) {
        parsed.put("texto", item);
        continue;
      }
      String key = item.substring(0, separatorIndex).trim();
      String value = item.substring(separatorIndex + 1).trim();
      if (StringUtils.hasText(key)) {
        parsed.put(key, parseQualityNoteValue(value));
      }
    }
    return parsed.isEmpty() ? Map.of("texto", notes) : parsed;
  }

  /** Converte valores textuais das notas para booleano ou número quando possível. */
  private Object parseQualityNoteValue(String value) {
    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
      return Boolean.valueOf(value);
    }
    if (value.matches("-?\\d+") && value.replace("-", "").length() <= 9) {
      return Integer.valueOf(value);
    }
    return value;
  }

  /** Converte um cartão persistido na unidade de trabalho usada pelo coletor da etapa sete. */
  private RecordRoutineQualityGatePending toPending(OprmNicheRoutineCard card) {
    List<OprmSourceSnapshot> snapshots = sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(card.getResearchCycleId());
    List<OprmExtractedSignal> signals = extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(card.getResearchCycleId());
    OprmMeiAudienceProfile profile = meiAudienceProfileRepository.findFirstByResearchCycleIdOrderByIdDesc(card.getResearchCycleId()).orElse(null);
    return new RecordRoutineQualityGatePending(
        card.getId(),
        card.getResearchCycleId(),
        card.getNicheName(),
        card.getRoutineSummary(),
        card.getPainsSummary(),
        card.getResultsSummary(),
        card.getMechanismOpportunitiesSummary(),
        profile == null ? null : profile.getCustomerAcquisitionBehavior(),
        profile == null ? null : profile.getChannelsUsed(),
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
        countBrazilianSources(snapshots),
        countRecentSources(snapshots),
        countOutdatedSourceRiskSnapshots(snapshots),
        countStructuredBusinessDriftSnapshots(snapshots),
        countCustomerAcquisitionEvidence(signals, profile),
        countEmotionalOutcomeEvidence(signals, profile),
        scoreFromProfile(profile == null ? null : profile.getAutonomousProfessionalFitScore()),
        scoreFromProfile(profile == null ? null : profile.getBehavioralEvidenceScore()),
        scoreFromProfile(profile == null ? null : profile.getSourceFreshnessScore()),
        scoreFromProfile(profile == null ? null : profile.getOutdatedSourceRiskScore()),
        scoreFromProfile(profile == null ? null : profile.getStructuredBusinessDriftRiskScore()),
        scoreFromProfile(profile == null ? null : profile.getSolutionLanguageRiskScore()),
        card.getCreatedAt());
  }

  /** Conta fontes brasileiras com relevância suficiente para evitar aprovação de material global ou genérico. */
  private int countBrazilianSources(List<OprmSourceSnapshot> snapshots) {
    return (int) snapshots.stream()
        .filter(snapshot -> scoreFromProfile(snapshot.getBrazilRelevanceScore()) >= 60 || endsWithBrazilianDomain(snapshot.getSourceDomain()))
        .count();
  }

  /** Conta fontes recentes e não marcadas como antigas para sustentar atualidade da pesquisa. */
  private int countRecentSources(List<OprmSourceSnapshot> snapshots) {
    return (int) snapshots.stream()
        .filter(snapshot -> scoreFromProfile(snapshot.getSourceFreshnessScore()) >= 60 && !Boolean.TRUE.equals(snapshot.getOutdatedSourceRisk()))
        .count();
  }

  /** Conta snapshots explicitamente marcados com risco de fonte antiga. */
  private int countOutdatedSourceRiskSnapshots(List<OprmSourceSnapshot> snapshots) {
    return (int) snapshots.stream()
        .filter(snapshot -> Boolean.TRUE.equals(snapshot.getOutdatedSourceRisk()))
        .count();
  }

  /** Conta snapshots com risco de representar empresa estruturada em vez do dono-operador MEI/autônomo. */
  private int countStructuredBusinessDriftSnapshots(List<OprmSourceSnapshot> snapshots) {
    return (int) snapshots.stream()
        .filter(snapshot -> Boolean.TRUE.equals(snapshot.getStructuredBusinessDriftRisk()))
        .count();
  }

  /** Conta evidências de aquisição, atendimento ou canal combinando sinais e o perfil segmentado. */
  private int countCustomerAcquisitionEvidence(List<OprmExtractedSignal> signals, OprmMeiAudienceProfile profile) {
    int signalCount = countSignals(signals, "CUSTOMER_ACQUISITION_BEHAVIOR", "CHANNEL_USAGE", "CHANNEL_BEHAVIOR", "CUSTOMER_SERVICE_CHANNEL");
    int profileEvidence = hasUsefulCommercialText(profile == null ? null : profile.getCustomerAcquisitionBehavior()) ? 1 : 0;
    int channelEvidence = hasUsefulCommercialText(profile == null ? null : profile.getChannelsUsed()) ? 1 : 0;
    return signalCount + profileEvidence + channelEvidence;
  }

  /** Conta evidências humanas de dor emocional, sonho ou medo do público MEI/autônomo. */
  private int countEmotionalOutcomeEvidence(List<OprmExtractedSignal> signals, OprmMeiAudienceProfile profile) {
    int signalCount = countSignals(signals, "EMOTIONAL_PAIN", "DREAM_SIGNAL", "FEAR_SIGNAL");
    int profileEvidence = 0;
    profileEvidence += hasText(profile == null ? null : profile.getEmotionalPainsSummary()) ? 1 : 0;
    profileEvidence += hasText(profile == null ? null : profile.getDreamsSummary()) ? 1 : 0;
    profileEvidence += hasText(profile == null ? null : profile.getFearsSummary()) ? 1 : 0;
    return signalCount + profileEvidence;
  }

  /** Verifica domínio brasileiro de forma simples quando a classificação de fonte ainda não trouxe score. */
  private boolean endsWithBrazilianDomain(String sourceDomain) {
    return StringUtils.hasText(sourceDomain) && sourceDomain.toLowerCase(Locale.ROOT).endsWith(".br");
  }

  /** Normaliza score nulo para zero ao montar o contrato entregue ao coletor. */
  private int scoreFromProfile(Integer score) {
    return score == null ? 0 : score;
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
      if (signal.getSignalType() != null && signalType.equalsIgnoreCase(signal.getSignalType())) {
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
    validateReadyForHypothesisCompatibility(request.qualityStatus(), request.readyForHypothesis());
    validateScore(request.specificityScore(), "specificityScore");
    validateScore(request.confidenceScore(), "confidenceScore");
    validateScore(request.duplicationScore(), "duplicationScore");
    String notes = trimOptional(request.qualityNotes());
    if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
      throw new IllegalArgumentException("qualityNotes must contain at most " + MAX_NOTES_LENGTH + " characters");
    }
  }

  /** Garante coerência entre status do gate e liberação para hipótese, evitando materialização indevida. */
  private void validateReadyForHypothesisCompatibility(String qualityStatus, Boolean readyForHypothesis) {
    String normalizedStatus = normalizeQualityStatus(qualityStatus);
    boolean approvedStatus = isApprovedQualityStatus(normalizedStatus);
    if (Boolean.TRUE.equals(readyForHypothesis) && !approvedStatus) {
      throw new IllegalArgumentException("readyForHypothesis can only be true when qualityStatus approves the MEI audience");
    }
    if (MEI_AUDIENCE_READY_STATUS.equals(normalizedStatus) && !Boolean.TRUE.equals(readyForHypothesis)) {
      throw new IllegalArgumentException("MEI_AUDIENCE_READY requires readyForHypothesis true");
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
    if (!isSupportedQualityStatus(normalized)) {
      throw new IllegalArgumentException(
          "qualityStatus must be MEI_AUDIENCE_READY, NEEDS_MORE_MEI_RESEARCH, OUTDATED_SOURCES, TOO_CORPORATE, SOLUTION_CONTAMINATED, NEEDS_EXECUTOR_ROUTINE_EVIDENCE or GENERIC");
    }
    return normalized;
  }

  /** Indica se o status recebido aprova o ciclo para a próxima etapa, mantendo compatibilidade com status legado. */
  private boolean isApprovedQualityStatus(String qualityStatus) {
    return MEI_AUDIENCE_READY_STATUS.equals(qualityStatus) || LIGHTLY_RESEARCHED_STATUS.equals(qualityStatus);
  }

  /** Retorna a próxima etapa canônica após a decisão de qualidade ou encerra fila quando o cartão foi reprovado. */
  private String nextStageAfterQuality(String qualityStatus) {
    return isApprovedQualityStatus(qualityStatus) ? CURRENT_STAGE_EVIDENCE_LEVEL_GATE : null;
  }

  /** Indica se o status pertence ao contrato atual ou ao legado ainda aceito para migração segura. */
  private boolean isSupportedQualityStatus(String normalized) {
    return MEI_AUDIENCE_READY_STATUS.equals(normalized)
        || NEEDS_MORE_MEI_RESEARCH_STATUS.equals(normalized)
        || OUTDATED_SOURCES_STATUS.equals(normalized)
        || TOO_CORPORATE_STATUS.equals(normalized)
        || SOLUTION_CONTAMINATED_STATUS.equals(normalized)
        || GENERIC_STATUS.equals(normalized)
        || NEEDS_EXECUTOR_ROUTINE_EVIDENCE_STATUS.equals(normalized)
        || LIGHTLY_RESEARCHED_STATUS.equals(normalized)
        || NEEDS_MORE_RESEARCH_STATUS.equals(normalized);
  }

  /** Exige texto útil para campos obrigatórios do contrato da etapa sete. */
  private String requiredText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }


  /** Verifica se o texto de aquisição/canal do perfil é evidência útil e não placeholder genérico. */
  private boolean hasUsefulCommercialText(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    String normalized = value.toLowerCase(Locale.ROOT)
        .replace('á', 'a')
        .replace('à', 'a')
        .replace('ã', 'a')
        .replace('â', 'a')
        .replace('é', 'e')
        .replace('ê', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('õ', 'o')
        .replace('ô', 'o')
        .replace('ú', 'u')
        .replace('ç', 'c')
        .replaceAll("\\s+", " ")
        .trim();
    return normalized.length() >= 35 && !containsInsufficientEvidencePlaceholder(normalized);
  }

  /** Detecta placeholders de ausência de evidência para não inflar contadores de aquisição/canal. */
  private boolean containsInsufficientEvidencePlaceholder(String normalized) {
    return normalized.contains("sem evidencia suficiente")
        || normalized.contains("sem evidencias suficientes")
        || normalized.contains("nao ha evidencia")
        || normalized.contains("nao existe evidencia")
        || normalized.contains("sem dados suficientes")
        || normalized.contains("informacao insuficiente")
        || normalized.contains("nao identificado")
        || normalized.contains("nao foi identificado");
  }

  /** Verifica se existe texto útil em campo opcional de perfil ou fonte. */
  private boolean hasText(String value) {
    return StringUtils.hasText(value);
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
