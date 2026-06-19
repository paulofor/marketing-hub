package com.marketinghub.oprm.nichocnae.evidencelevelgate.service;

import com.marketinghub.oprm.nichocnae.OprmNicheRoutineCard;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.evidencelevelgate.service.completeStageExecution.CompleteEvidenceLevelGateRequest;
import com.marketinghub.oprm.nichocnae.evidencelevelgate.service.completeStageExecution.CompleteEvidenceLevelGateResponse;
import com.marketinghub.oprm.nichocnae.evidencelevelgate.service.detailStageExecution.EvidenceLevelGateDetailResponse;
import com.marketinghub.oprm.nichocnae.evidencelevelgate.service.failStageExecution.FailEvidenceLevelGateRequest;
import com.marketinghub.oprm.nichocnae.evidencelevelgate.service.pending.RecordEvidenceLevelGatePending;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmNicheRoutineCardRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsável apenas por ler pendências e persistir resultados do gate E0-E5 calculados pelo executor externo. */
@Service
public class BackendEvidenceLevelGateService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendEvidenceLevelGateService.class);
  private static final int MAX_PENDING = 10;
  private static final String STAGE_CODE = "evidence-level-gate";
  private static final String NEXT_STAGE = "enriched-niche-materializer";
  private static final String STATUS_FAILED = "FAILED";
  private static final String STATUS_APPROVED = "EVIDENCE_GATE_APPROVED";

  private final OprmRoutineResearchCycleRepository cycleRepository;
  private final OprmNicheRoutineCardRepository cardRepository;

  /** Inicializa o serviço com repositórios canônicos de ciclo e cartão. */
  public BackendEvidenceLevelGateService(
      OprmRoutineResearchCycleRepository cycleRepository, OprmNicheRoutineCardRepository cardRepository) {
    this.cycleRepository = cycleRepository;
    this.cardRepository = cardRepository;
  }

  /** Lista cartões pendentes para o executor externo calcular nível E0-E5. */
  @Transactional(readOnly = true)
  public List<RecordEvidenceLevelGatePending> listPending() {
    return cardRepository.findPendingEvidenceLevelGate(PageRequest.of(0, MAX_PENDING)).stream().map(this::toPending).toList();
  }

  /** Persiste a decisão E0-E5 recebida do executor e move o ciclo apenas conforme o resultado informado. */
  @Transactional
  public CompleteEvidenceLevelGateResponse complete(Long researchCycleId, CompleteEvidenceLevelGateRequest request) {
    try {
      OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
      OprmNicheRoutineCard card = cardRepository.findById(requiredId(request == null ? null : request.routineCardId(), "routineCardId"))
          .orElseThrow(() -> new EntityNotFoundException("Routine card not found: " + request.routineCardId()));
      if (!card.getResearchCycleId().equals(researchCycleId)) {
        throw new IllegalArgumentException("routineCardId must belong to researchCycleId");
      }
      String level = requiredText(request.evidenceLevel(), "evidenceLevel").toUpperCase();
      String status = requiredText(request.gateStatus(), "gateStatus").toUpperCase();
      Integer confidence = request.confidenceScore() == null ? 0 : Math.max(0, Math.min(100, request.confidenceScore()));
      Instant now = Instant.now();
      card.setEvidenceLevel(level);
      card.setEvidenceGateStatus(status);
      card.setEvidenceConfidenceScore(confidence);
      card.setEvidenceGateNotes(buildNotes(request));
      card.setEvidenceGateCheckedBy(defaultText(request.checkedBy(), "oprmEvidenceLevelGate"));
      card.setEvidenceGateCheckedAt(now);
      cardRepository.save(card);
      boolean approved = Boolean.TRUE.equals(request.approvedForMaterialization());
      cycle.setStatus(approved ? STATUS_APPROVED : status);
      cycle.setCurrentStageCode(approved ? NEXT_STAGE : STAGE_CODE);
      cycle.setUpdatedAt(now);
      cycle.setFinishedAt(now);
      cycleRepository.save(cycle);
      return new CompleteEvidenceLevelGateResponse(card.getId(), cycle.getId(), cycle.getStatus(), level, status, approved, confidence, now);
    } catch (RuntimeException ex) {
      LOGGER.error("Erro ao persistir etapa onze E0-E5 OPRM nichocnae (researchCycleId={}, routineCardId={})", researchCycleId, request == null ? null : request.routineCardId(), ex);
      throw ex;
    }
  }

  /** Registra falha técnica da etapa E0-E5 sem tomar decisão comercial no backend. */
  @Transactional
  public void fail(Long researchCycleId, FailEvidenceLevelGateRequest request) {
    try {
      OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
      Instant now = Instant.now();
      cycle.setStatus(STATUS_FAILED);
      cycle.setCurrentStageCode(STAGE_CODE);
      cycle.setErrorMessage(requiredText(request == null ? null : request.errorMessage(), "errorMessage"));
      cycle.setUpdatedAt(now);
      cycle.setFinishedAt(now);
      cycleRepository.save(cycle);
    } catch (RuntimeException ex) {
      LOGGER.error("Erro ao registrar falha da etapa onze E0-E5 OPRM nichocnae (researchCycleId={})", researchCycleId, ex);
      throw ex;
    }
  }

  /** Retorna o resultado E0-E5 persistido para relatório do usuário. */
  @Transactional(readOnly = true)
  public EvidenceLevelGateDetailResponse detail(Long researchCycleId) {
    OprmRoutineResearchCycle cycle = findCycle(researchCycleId);
    OprmNicheRoutineCard card = cardRepository.findFirstByResearchCycleIdOrderByIdDesc(researchCycleId).orElse(null);
    return new EvidenceLevelGateDetailResponse(researchCycleId, cycle.getStatus(), card == null ? null : card.getId(), card == null ? null : card.getEvidenceLevel(), card == null ? null : card.getEvidenceGateStatus(), card == null ? null : card.getEvidenceConfidenceScore(), card == null ? null : card.getEvidenceGateNotes(), card == null ? null : card.getEvidenceGateCheckedBy(), card == null ? null : card.getEvidenceGateCheckedAt());
  }

  /** Converte o cartão persistido no contrato de leitura para o executor externo. */
  private RecordEvidenceLevelGatePending toPending(OprmNicheRoutineCard card) {
    return new RecordEvidenceLevelGatePending(card.getId(), card.getResearchCycleId(), card.getNicheName(), card.getRoutineSummary(), card.getPainsSummary(), card.getResultsSummary(), card.getEvidenceSummary(), card.getSourceDomains(), card.getConfidenceScore(), card.getRoutineEvidenceScore(), card.getDifficultyEvidenceScore(), card.getSourceDiversityScore(), card.getSpecificityScore(), card.getConfidenceScore());
  }

  /** Busca o ciclo ou falha quando o identificador não existe. */
  private OprmRoutineResearchCycle findCycle(Long researchCycleId) {
    return cycleRepository.findById(requiredId(researchCycleId, "researchCycleId")).orElseThrow(() -> new EntityNotFoundException("Research cycle not found: " + researchCycleId));
  }

  /** Monta notas textuais persistidas sem calcular regra comercial no backend. */
  private String buildNotes(CompleteEvidenceLevelGateRequest request) {
    return "rejectionReasons=" + defaultText(request.rejectionReasons(), "") + "; nextMovements=" + defaultText(request.nextMovements(), "");
  }

  /** Exige identificador obrigatório. */
  private Long requiredId(Long value, String field) {
    if (value == null) throw new IllegalArgumentException(field + " is required");
    return value;
  }

  /** Exige texto obrigatório. */
  private String requiredText(String value, String field) {
    if (!StringUtils.hasText(value)) throw new IllegalArgumentException(field + " is required");
    return value.trim();
  }

  /** Usa valor padrão para texto opcional em branco. */
  private String defaultText(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim() : fallback;
  }
}
