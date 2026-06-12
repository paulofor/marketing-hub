package com.marketinghub.oprm.nichocnae.routineresearchcycle.service;

import com.marketinghub.oprm.nichocnae.OprmRoutineResearchCycle;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.detailStageExecution.RecordBackendRoutineResearchCycleDetalheDto;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.listStageExecutions.RoutineResearchCycleExecutionSummaryResponse;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.pending.RecordRoutineResearchCyclePending;
import com.marketinghub.repository.jpa.oprm.nichocnae.OprmRoutineResearchCycleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsável por expor a borda backend da etapa de ciclo da pesquisa de rotina de nicho CNAE. */
@Service
public class BackendRoutineResearchCycleService {
  private static final String CYCLE_STATUS_RUNNING = "RUNNING";

  private final OprmRoutineResearchCycleRepository routineResearchCycleRepository;

  /** Inicializa o serviço com o repositório canônico de ciclos de pesquisa de rotina. */
  public BackendRoutineResearchCycleService(OprmRoutineResearchCycleRepository routineResearchCycleRepository) {
    this.routineResearchCycleRepository = routineResearchCycleRepository;
  }

  /** Lista ciclos de pesquisa de rotina pendentes para processamento assíncrono da etapa. */
  @Transactional(readOnly = true)
  public List<RecordRoutineResearchCyclePending> listPending() {
    return routineResearchCycleRepository.findByStatusOrderByStartedAtAsc(CYCLE_STATUS_RUNNING, PageRequest.of(0, 20))
        .stream()
        .map(this::toPending)
        .toList();
  }

  /** Lista execuções do ciclo de pesquisa de rotina associadas ao CNAE informado. */
  @Transactional(readOnly = true)
  public List<RoutineResearchCycleExecutionSummaryResponse> listStageExecutionsByCnae(String cnaeCode) {
    return routineResearchCycleRepository.findByCnaeCodeOrderByStartedAtDesc(cnaeCode).stream()
        .map(this::toSummary)
        .toList();
  }

  /** Lista execuções do ciclo de pesquisa de rotina associadas a um nicho CNAE de origem. */
  @Transactional(readOnly = true)
  public List<RoutineResearchCycleExecutionSummaryResponse> listStageExecutions(Long sourceNicheId) {
    return routineResearchCycleRepository.findBySourceNicheIdOrderByStartedAtDesc(sourceNicheId).stream()
        .map(this::toSummary)
        .toList();
  }

  /** Retorna os detalhes operacionais de uma execução específica do ciclo de pesquisa de rotina. */
  @Transactional(readOnly = true)
  public RecordBackendRoutineResearchCycleDetalheDto detailStageExecution(Long researchCycleId) {
    OprmRoutineResearchCycle cycle = routineResearchCycleRepository
        .findById(researchCycleId)
        .orElseThrow(() -> new EntityNotFoundException("Routine research cycle not found: " + researchCycleId));
    return toDetail(cycle);
  }

  /** Converte um ciclo em unidade de trabalho fechada para processamento interno da etapa. */
  private RecordRoutineResearchCyclePending toPending(OprmRoutineResearchCycle cycle) {
    return new RecordRoutineResearchCyclePending(
        cycle.getId(),
        cycle.getSourceNicheId(),
        cycle.getCnaeCode(),
        cycle.getCnaeDescription(),
        cycle.getNicheName(),
        cycle.getOriginalNicheName(),
        cycle.getNeutralNicheName(),
        cycle.getResearchMode(),
        cycle.getSolutionLanguageRiskScore(),
        cycle.getSourceScore(),
        cycle.getTriggerSource(),
        cycle.getStatus(),
        cycle.getStartedAt(),
        cycle.getCreatedAt());
  }

  /** Converte um ciclo em resumo para listagem operacional. */
  private RoutineResearchCycleExecutionSummaryResponse toSummary(OprmRoutineResearchCycle cycle) {
    return new RoutineResearchCycleExecutionSummaryResponse(
        cycle.getId(),
        cycle.getSourceNicheId(),
        cycle.getCnaeCode(),
        cycle.getNicheName(),
        cycle.getOriginalNicheName(),
        cycle.getNeutralNicheName(),
        cycle.getResearchMode(),
        cycle.getSolutionLanguageRiskScore(),
        cycle.getSourceScore(),
        cycle.getStatus(),
        cycle.getTotalQueries(),
        cycle.getTotalExtractedSignals(),
        cycle.getStartedAt(),
        cycle.getFinishedAt());
  }

  /** Converte um ciclo em detalhe operacional completo. */
  private RecordBackendRoutineResearchCycleDetalheDto toDetail(OprmRoutineResearchCycle cycle) {
    return new RecordBackendRoutineResearchCycleDetalheDto(
        cycle.getId(),
        cycle.getSourceNicheId(),
        cycle.getCnaeCode(),
        cycle.getCnaeDescription(),
        cycle.getNicheName(),
        cycle.getOriginalNicheName(),
        cycle.getNeutralNicheName(),
        cycle.getResearchMode(),
        cycle.getSolutionLanguageRiskScore(),
        cycle.getSourceScore(),
        cycle.getStatus(),
        cycle.getTotalQueries(),
        cycle.getTotalSourceCandidates(),
        cycle.getTotalSourceSnapshots(),
        cycle.getTotalExtractedSignals(),
        cycle.getStartedAt(),
        cycle.getFinishedAt(),
        cycle.getErrorMessage());
  }
}
