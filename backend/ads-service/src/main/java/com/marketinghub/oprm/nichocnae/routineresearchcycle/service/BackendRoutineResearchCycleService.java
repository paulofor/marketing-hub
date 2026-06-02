package com.marketinghub.oprm.nichocnae.routineresearchcycle.service;

import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.detailStageExecution.RecordBackendRoutineResearchCycleDetalheDto;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.listStageExecutions.RoutineResearchCycleExecutionSummaryResponse;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.pending.RecordRoutineResearchCyclePending;
import java.util.List;
import org.springframework.stereotype.Service;

/** Responsável por expor a borda backend da etapa de ciclo da pesquisa de rotina de nicho CNAE. */
@Service
public class BackendRoutineResearchCycleService {

  /** Lista ciclos de pesquisa de rotina pendentes para processamento assíncrono da etapa. */
  public List<RecordRoutineResearchCyclePending> listPending() {
    return List.of();
  }

  /** Lista execuções do ciclo de pesquisa de rotina associadas a um nicho CNAE de origem. */
  public List<RoutineResearchCycleExecutionSummaryResponse> listStageExecutions(Long sourceNicheId) {
    return List.of();
  }

  /** Retorna os detalhes operacionais de uma execução específica do ciclo de pesquisa de rotina. */
  public RecordBackendRoutineResearchCycleDetalheDto detailStageExecution(Long researchCycleId) {
    return new RecordBackendRoutineResearchCycleDetalheDto(
        researchCycleId,
        null,
        null,
        null,
        null,
        null,
        null,
        0,
        0,
        0,
        0,
        null,
        null,
        null);
  }
}
