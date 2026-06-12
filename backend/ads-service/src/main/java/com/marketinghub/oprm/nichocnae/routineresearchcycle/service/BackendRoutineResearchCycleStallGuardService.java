package com.marketinghub.oprm.nichocnae.routineresearchcycle.service;

import com.marketinghub.oprm.cnae.OprmNicheCandidate;
import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.repository.jpa.oprm.cnae.OprmNicheCandidateRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsável por detectar ciclos NichoCNAE parados e expor o bloqueio operacional no status persistido. */
@Service
public class BackendRoutineResearchCycleStallGuardService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BackendRoutineResearchCycleStallGuardService.class);
  private static final String CYCLE_STATUS_RUNNING = "RUNNING";
  private static final String CYCLE_STATUS_STALLED = "STALLED";
  private static final String ROUTINE_STATUS_STALLED = "RESEARCH_STALLED";
  private static final Duration WITHOUT_PROGRESS_LIMIT = Duration.ofHours(6);
  private static final int MAX_CYCLES_PER_SCAN = 50;

  private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;
  private final OprmNicheCandidateRepository nicheCandidateRepository;

  /** Inicializa a proteção com os repositórios canônicos do ciclo e do candidato de origem. */
  public BackendRoutineResearchCycleStallGuardService(
      OprmRoutineResearchCycleRepository routineResearchCycleRepository,
      OprmNicheCandidateRepository nicheCandidateRepository) {
    this.routineResearchCycleRepository = routineResearchCycleRepository;
    this.nicheCandidateRepository = nicheCandidateRepository;
  }

  /** Marca como STALLED ciclos RUNNING sem nenhum avanço operacional após o limite definido. */
  @Transactional
  public int markRunningCyclesWithoutProgressAsStalled(Instant referenceTime) {
    Instant threshold = referenceTime.minus(WITHOUT_PROGRESS_LIMIT);
    List<OprmRoutineResearchCycle> staleCycles = routineResearchCycleRepository.findRunningCyclesWithoutProgressBefore(
        CYCLE_STATUS_RUNNING, threshold, PageRequest.of(0, MAX_CYCLES_PER_SCAN));
    staleCycles.forEach(cycle -> markCycleAsStalled(cycle, referenceTime));
    if (!staleCycles.isEmpty()) {
      LOGGER.warn(
          "Proteção OPRM NichoCNAE marcou ciclos sem progresso como STALLED (count={}, threshold={}, limitHours={})",
          staleCycles.size(),
          threshold,
          WITHOUT_PROGRESS_LIMIT.toHours());
    }
    return staleCycles.size();
  }

  /** Atualiza o ciclo e o candidato de origem para que a tela não exiba execução saudável. */
  private void markCycleAsStalled(OprmRoutineResearchCycle cycle, Instant now) {
    cycle.setStatus(CYCLE_STATUS_STALLED);
    cycle.setFinishedAt(now);
    cycle.setUpdatedAt(now);
    cycle.setErrorMessage(buildStalledMessage(cycle));
    routineResearchCycleRepository.save(cycle);
    nicheCandidateRepository
        .findById(cycle.getSourceNicheId())
        .ifPresent(candidate -> markCandidateAsStalled(candidate, cycle, now));
    LOGGER.warn(
        "Ciclo OPRM NichoCNAE sem progresso marcado como STALLED (researchCycleId={}, sourceNicheId={}, cnaeCode={}, startedAt={}, updatedAt={})",
        cycle.getId(),
        cycle.getSourceNicheId(),
        cycle.getCnaeCode(),
        cycle.getStartedAt(),
        cycle.getUpdatedAt());
  }

  /** Atualiza o candidato vinculado ao ciclo parado para preservar consistência do acompanhamento operacional. */
  private void markCandidateAsStalled(OprmNicheCandidate candidate, OprmRoutineResearchCycle cycle, Instant now) {
    candidate.setRoutineResearchStatus(ROUTINE_STATUS_STALLED);
    candidate.setLastRoutineResearchCycleId(cycle.getId());
    candidate.setUpdatedAt(now);
    nicheCandidateRepository.save(candidate);
  }

  /** Monta mensagem objetiva para orientar correção da causa-raiz do pipeline parado. */
  private String buildStalledMessage(OprmRoutineResearchCycle cycle) {
    return "Pipeline OPRM NichoCNAE sem progresso por mais de "
        + WITHOUT_PROGRESS_LIMIT.toHours()
        + " horas; verificar executor oprm-coletor-mei e conectividade com backend antes de reprocessar. researchCycleId="
        + cycle.getId();
  }
}
