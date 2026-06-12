package com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service;

import com.marketinghub.oprm.nichocnae.OprmExtractedSignal;
import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.OprmSourceSnapshot;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.BackendMeiAudienceProfileService;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.upsertAudienceProfile.UpsertMeiAudienceProfileRequest;
import com.marketinghub.oprm.nichocnae.meiaudienceprofile.service.upsertAudienceProfile.UpsertMeiAudienceProfileResponse;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.completeStageExecution.CompleteMeiAudienceSegmenterRequest;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.completeStageExecution.CompleteMeiAudienceSegmenterResponse;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.failStageExecution.FailMeiAudienceSegmenterRequest;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.pending.RecordMeiAudienceSegmenterPending;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.pending.SegmenterSignalResponse;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.pending.SegmenterSourceSnapshotResponse;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmExtractedSignalRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmSourceSnapshotRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Serviço responsável por expor, validar e persistir a segmentação comportamental MEI/autônomo do OPRM. */
@Service
public class BackendMeiAudienceSegmenterService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendMeiAudienceSegmenterService.class);
  private static final String ROUTINE_SYNTHESIZED_STATUS = "ROUTINE_SYNTHESIZED";
  private static final String SEGMENTED_STATUS = "MEI_AUDIENCE_SEGMENTED";
  private static final String FAILED_STATUS = "FAILED";
  private static final int MAX_PENDING = 10;
  private static final int MAX_SIGNALS = 120;
  private static final int MAX_SOURCES = 30;
  private static final int MAX_TEXT_LENGTH = 4000;
  private static final List<String> SOLUTION_TERMS = List.of(
      " produto", " oferta", " preço", " promessa", " campanha", " landing page", " software", " automação", " inteligência artificial", " ia ", " curso", " ferramenta");

  private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;
  private final OprmNicheRoutineCardRepository routineCardRepository;
  private final OprmExtractedSignalRepository extractedSignalRepository;
  private final OprmSourceSnapshotRepository sourceSnapshotRepository;
  private final BackendMeiAudienceProfileService profileService;

  /** Inicializa o serviço com repositórios e serviço de perfil canônico usados pela etapa de segmentação. */
  public BackendMeiAudienceSegmenterService(
      OprmRoutineResearchCycleRepository routineResearchCycleRepository,
      OprmNicheRoutineCardRepository routineCardRepository,
      OprmExtractedSignalRepository extractedSignalRepository,
      OprmSourceSnapshotRepository sourceSnapshotRepository,
      BackendMeiAudienceProfileService profileService) {
    this.routineResearchCycleRepository = routineResearchCycleRepository;
    this.routineCardRepository = routineCardRepository;
    this.extractedSignalRepository = extractedSignalRepository;
    this.sourceSnapshotRepository = sourceSnapshotRepository;
    this.profileService = profileService;
  }

  /** Lista somente cartões de ciclos ativos e elegíveis para segmentação comportamental antes do gate e materialização. */
  @Transactional(readOnly = true)
  public List<RecordMeiAudienceSegmenterPending> listPending() {
    return routineCardRepository.findPendingMeiAudienceSegmentation(PageRequest.of(0, MAX_PENDING)).stream()
        .map(this::toPendingIfEligible)
        .flatMap(Optional::stream)
        .toList();
  }

  /** Persiste a segmentação validada em perfil MEI/autônomo e atualiza o status do ciclo. */
  @Transactional
  public CompleteMeiAudienceSegmenterResponse complete(Long researchCycleId, CompleteMeiAudienceSegmenterRequest request) {
    try {
      validateCompletionRequest(researchCycleId, request);
      OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
      OprmNicheRoutineCard card = findCard(request.routineCardId());
      if (!card.getResearchCycleId().equals(researchCycleId)) {
        throw new IllegalArgumentException("routineCardId must belong to researchCycleId");
      }
      UpsertMeiAudienceProfileResponse profile = profileService.upsertAudienceProfile(toProfileRequest(request));
      Instant now = Instant.now();
      cycle.setStatus(SEGMENTED_STATUS);
      cycle.setUpdatedAt(now);
      cycle.setErrorMessage(null);
      routineResearchCycleRepository.save(cycle);
      return new CompleteMeiAudienceSegmenterResponse(
          profile.id(), profile.researchCycleId(), profile.routineCardId(), cycle.getStatus(), profile.audienceName(),
          profile.autonomousProfessionalFitScore(), profile.behavioralEvidenceScore(), profile.sourceFreshnessScore(), profile.updatedAt());
    } catch (RuntimeException ex) {
      LOGGER.error(
          "Erro ao concluir segmentação MEI/autônomo do OPRM nichocnae (researchCycleId={}, routineCardId={}, segmentedBy={})",
          researchCycleId,
          request == null ? null : request.routineCardId(),
          request == null ? null : request.segmentedBy(),
          ex);
      throw ex;
    }
  }

  /** Registra falha da segmentação no ciclo para reprocessamento e diagnóstico operacional. */
  @Transactional
  public void fail(Long researchCycleId, FailMeiAudienceSegmenterRequest request) {
    try {
      OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
      Instant now = Instant.now();
      cycle.setStatus(FAILED_STATUS);
      cycle.setErrorMessage(requiredText(request == null ? null : request.errorMessage(), "errorMessage"));
      cycle.setFinishedAt(now);
      cycle.setUpdatedAt(now);
      routineResearchCycleRepository.save(cycle);
    } catch (RuntimeException ex) {
      LOGGER.error("Erro ao registrar falha da segmentação MEI/autônomo do OPRM nichocnae (researchCycleId={})", researchCycleId, ex);
      throw ex;
    }
  }

  /** Revalida a elegibilidade do ciclo antes de expor o cartão na fila MEI/autônomo. */
  private Optional<RecordMeiAudienceSegmenterPending> toPendingIfEligible(OprmNicheRoutineCard card) {
    OprmRoutineResearchCycle cycle = findCycle(card.getResearchCycleId());
    if (!ROUTINE_SYNTHESIZED_STATUS.equals(cycle.getStatus())) {
      return Optional.empty();
    }
    return Optional.of(toPending(card, cycle));
  }

  /** Converte cartão, ciclo, fontes e sinais na unidade de trabalho enviada ao coletor de IA. */
  private RecordMeiAudienceSegmenterPending toPending(OprmNicheRoutineCard card, OprmRoutineResearchCycle cycle) {
    List<OprmSourceSnapshot> snapshots = sourceSnapshotRepository.findByResearchCycleIdOrderByIdAsc(card.getResearchCycleId());
    List<OprmExtractedSignal> signals = extractedSignalRepository.findByResearchCycleIdOrderByIdAsc(card.getResearchCycleId());
    return new RecordMeiAudienceSegmenterPending(
        cycle.getId(),
        card.getId(),
        cycle.getSourceNicheId(),
        cycle.getCnaeCode(),
        cycle.getCnaeDescription(),
        cycle.getNeutralNicheName(),
        card.getNicheName(),
        card.getRoutineSummary(),
        card.getCustomerBehaviorSummary(),
        card.getChannelsSummary(),
        card.getOperationalPainsSummary(),
        card.getEmotionalPainsSummary(),
        card.getDreamsSummary(),
        card.getFearsSummary(),
        card.getLanguageSummary(),
        card.getPainsSummary(),
        card.getResultsSummary(),
        card.getEvidenceSummary(),
        card.getSourceDomains(),
        card.getRoutineEvidenceScore(),
        card.getDifficultyEvidenceScore(),
        card.getSourceDiversityScore(),
        card.getSolutionLanguageRiskScore(),
        card.getCreatedAt(),
        snapshots.stream().sorted(Comparator.comparing(OprmSourceSnapshot::getId)).limit(MAX_SOURCES).map(this::toSource).toList(),
        signals.stream().sorted(Comparator.comparing(OprmExtractedSignal::getId)).limit(MAX_SIGNALS).map(this::toSignal).toList());
  }

  /** Converte snapshot persistido em fonte curta com indicadores de atualidade e aderência. */
  private SegmenterSourceSnapshotResponse toSource(OprmSourceSnapshot snapshot) {
    return new SegmenterSourceSnapshotResponse(
        snapshot.getId(), snapshot.getSourceCandidateId(), snapshot.getSourceUrl(), snapshot.getSourceDomain(), snapshot.getSourceTitle(),
        snapshot.getSourceType(), snapshot.getSourceClassificationType(), snapshot.getSourceFreshnessScore(), snapshot.getOutdatedSourceRisk(),
        snapshot.getBrazilRelevanceScore(), snapshot.getAutonomousProfessionalEvidenceScore(), snapshot.getStructuredBusinessDriftRisk(),
        snapshot.getSolutionLanguageRisk(), snapshot.getPublishedAt(), trimOptional(snapshot.getSnippet()), trimOptional(snapshot.getShortExcerpt()));
  }

  /** Converte sinal persistido em evidência rastreável usada pelo segmentador. */
  private SegmenterSignalResponse toSignal(OprmExtractedSignal signal) {
    return new SegmenterSignalResponse(
        signal.getId(), signal.getSourceSnapshotId(), signal.getSourceCandidateId(), signal.getSignalType(), signal.getSignalText(),
        signal.getEvidenceExcerpt(), signal.getSourceDomain(), signal.getConfidenceScore());
  }

  /** Converte a saída da etapa em contrato oficial de perfil MEI/autônomo. */
  private UpsertMeiAudienceProfileRequest toProfileRequest(CompleteMeiAudienceSegmenterRequest request) {
    return new UpsertMeiAudienceProfileRequest(
        request.researchCycleId(), request.routineCardId(), request.sourceNicheCandidateId(), null, request.cnaeCode(), request.cnaeDescription(),
        request.neutralNicheName(), request.audienceName(), request.occupationTerms(), request.workMode(), request.customerAcquisitionBehavior(),
        request.dailyRoutineSummary(), request.recurringTasksSummary(), request.operationalPainsSummary(), request.emotionalPainsSummary(),
        request.dreamsSummary(), request.fearsSummary(), request.languagePatterns(), request.channelsUsed(), request.recentSourceSummary(),
        request.autonomousProfessionalFitScore(), request.behavioralEvidenceScore(), request.sourceFreshnessScore(), request.outdatedSourceRiskScore(),
        request.structuredBusinessDriftRiskScore(), request.solutionLanguageRiskScore());
  }

  /** Valida o contrato final e bloqueia contaminação por produto, oferta, campanha ou solução. */
  private void validateCompletionRequest(Long researchCycleId, CompleteMeiAudienceSegmenterRequest request) {
    if (researchCycleId == null || request == null || !researchCycleId.equals(request.researchCycleId())) {
      throw new IllegalArgumentException("researchCycleId must match path");
    }
    if (request.routineCardId() == null || request.sourceNicheCandidateId() == null) {
      throw new IllegalArgumentException("routineCardId and sourceNicheCandidateId are required");
    }
    validateText(request.cnaeCode(), "cnaeCode");
    validateText(request.cnaeDescription(), "cnaeDescription");
    validateText(request.neutralNicheName(), "neutralNicheName");
    validateText(request.audienceName(), "audienceName");
    validateText(request.workMode(), "workMode");
    validateText(request.dailyRoutineSummary(), "dailyRoutineSummary");
    validateText(request.operationalPainsSummary(), "operationalPainsSummary");
    validateText(request.recentSourceSummary(), "recentSourceSummary");
    validateScore(request.autonomousProfessionalFitScore(), "autonomousProfessionalFitScore");
    validateScore(request.behavioralEvidenceScore(), "behavioralEvidenceScore");
    validateScore(request.sourceFreshnessScore(), "sourceFreshnessScore");
    validateScore(request.outdatedSourceRiskScore(), "outdatedSourceRiskScore");
    validateScore(request.structuredBusinessDriftRiskScore(), "structuredBusinessDriftRiskScore");
    validateScore(request.solutionLanguageRiskScore(), "solutionLanguageRiskScore");
    rejectSolutionLanguage(request);
  }

  /** Valida texto obrigatório e tamanho máximo do campo de segmentação. */
  private void validateText(String value, String fieldName) {
    String text = requiredText(value, fieldName);
    if (text.length() > MAX_TEXT_LENGTH) {
      throw new IllegalArgumentException(fieldName + " must contain at most " + MAX_TEXT_LENGTH + " characters");
    }
  }

  /** Bloqueia linguagem de produto/oferta no payload final publicável de perfil. */
  private void rejectSolutionLanguage(CompleteMeiAudienceSegmenterRequest request) {
    String combined = (request.audienceName() + " " + request.workMode() + " " + request.customerAcquisitionBehavior() + " "
        + request.dailyRoutineSummary() + " " + request.operationalPainsSummary() + " " + request.emotionalPainsSummary() + " "
        + request.dreamsSummary() + " " + request.fearsSummary() + " " + request.languagePatterns() + " " + request.channelsUsed())
        .toLowerCase(Locale.ROOT);
    for (String term : SOLUTION_TERMS) {
      if ((" " + combined + " ").contains(term)) {
        throw new IllegalArgumentException("Segmentação MEI/autônomo contém linguagem de solução proibida: " + term.trim());
      }
    }
  }

  /** Valida pontuação percentual obrigatória entre zero e cem. */
  private void validateScore(Integer value, String fieldName) {
    if (value == null || value < 0 || value > 100) {
      throw new IllegalArgumentException(fieldName + " must be between 0 and 100");
    }
  }

  /** Localiza o ciclo de pesquisa ou falha com erro contratual. */
  private OprmRoutineResearchCycle findCycle(Long researchCycleId) {
    return routineResearchCycleRepository.findById(researchCycleId)
        .orElseThrow(() -> new EntityNotFoundException("Routine research cycle not found: " + researchCycleId));
  }

  /** Localiza o cartão de rotina ou falha com erro contratual. */
  private OprmNicheRoutineCard findCard(Long routineCardId) {
    return routineCardRepository.findById(routineCardId)
        .orElseThrow(() -> new EntityNotFoundException("Routine card not found: " + routineCardId));
  }

  /** Retorna texto obrigatório normalizado. */
  private String requiredText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }

  /** Retorna texto opcional normalizado sem exceder o contrato de payload curto. */
  private String trimOptional(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.length() <= MAX_TEXT_LENGTH ? trimmed : trimmed.substring(0, MAX_TEXT_LENGTH);
  }
}
