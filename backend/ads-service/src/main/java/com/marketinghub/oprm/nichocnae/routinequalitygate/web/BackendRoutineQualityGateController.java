package com.marketinghub.oprm.nichocnae.routinequalitygate.web;

import com.marketinghub.oprm.nichocnae.routinequalitygate.service.BackendRoutineQualityGateService;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.completeStageExecution.CompleteRoutineQualityGateRequest;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.completeStageExecution.CompleteRoutineQualityGateResponse;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.detailStageExecution.RoutineQualityGateDetailResponse;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.failStageExecution.FailRoutineQualityGateRequest;
import com.marketinghub.oprm.nichocnae.routinequalitygate.service.pending.RecordRoutineQualityGatePending;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por expor os endpoints backend da etapa sete do pipeline OPRM NichoCNAE. */
@RestController
@RequestMapping("/api")
public class BackendRoutineQualityGateController {
  private final BackendRoutineQualityGateService executionService;

  /** Inicializa o controller com o serviço backend da etapa sete de avaliação de qualidade. */
  public BackendRoutineQualityGateController(BackendRoutineQualityGateService executionService) {
    this.executionService = executionService;
  }

  /** Lista cartões de rotina pendentes de avaliação pelo gate de qualidade. */
  @GetMapping("/internal/oprm/nichocnae/routine-quality-gate/stage-executions/pending")
  public List<RecordRoutineQualityGatePending> pending() {
    return executionService.listPending();
  }

  /** Persiste a decisão de qualidade para um cartão de rotina sintetizado. */
  @PostMapping("/internal/oprm/nichocnae/routine-quality-gate/stage-executions/{researchCycleId}/complete")
  public ResponseEntity<CompleteRoutineQualityGateResponse> complete(
      @PathVariable Long researchCycleId, @RequestBody CompleteRoutineQualityGateRequest request) {
    return ResponseEntity.ok(executionService.complete(researchCycleId, request));
  }

  /** Registra falha operacional da avaliação de qualidade para um ciclo de pesquisa. */
  @PostMapping("/internal/oprm/nichocnae/routine-quality-gate/stage-executions/{researchCycleId}/fail")
  public ResponseEntity<Void> fail(@PathVariable Long researchCycleId, @RequestBody FailRoutineQualityGateRequest request) {
    executionService.fail(researchCycleId, request);
    return ResponseEntity.noContent().build();
  }

  /** Detalha a avaliação de qualidade do cartão de rotina de um ciclo. */
  @GetMapping("/oprm/nichocnae/routine-quality-gate/stage-executions/{researchCycleId}")
  public ResponseEntity<RoutineQualityGateDetailResponse> detail(@PathVariable Long researchCycleId) {
    return ResponseEntity.ok(executionService.detail(researchCycleId));
  }
}
