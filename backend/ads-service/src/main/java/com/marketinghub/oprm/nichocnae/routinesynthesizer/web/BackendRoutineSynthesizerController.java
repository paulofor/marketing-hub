package com.marketinghub.oprm.nichocnae.routinesynthesizer.web;

import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.BackendRoutineSynthesizerService;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.completeStageExecution.CompleteRoutineSynthesizerRequest;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.completeStageExecution.CompleteRoutineSynthesizerResponse;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.detailStageExecution.RoutineSynthesizerDetailResponse;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.failStageExecution.FailRoutineSynthesizerRequest;
import com.marketinghub.oprm.nichocnae.routinesynthesizer.service.pending.RecordRoutineSynthesizerPending;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por expor os endpoints backend da etapa seis do pipeline OPRM nichocnae. */
@RestController
@RequestMapping("/api")
public class BackendRoutineSynthesizerController {
  private final BackendRoutineSynthesizerService executionService;

  /** Inicializa o controller com o serviço backend da etapa seis de síntese da rotina. */
  public BackendRoutineSynthesizerController(BackendRoutineSynthesizerService executionService) {
    this.executionService = executionService;
  }

  /** Lista ciclos com sinais extraídos pendentes de síntese de rotina por worker externo. */
  @GetMapping("/internal/oprm/nichocnae/routine-synthesizer/stage-executions/pending")
  public List<RecordRoutineSynthesizerPending> pending() {
    return executionService.listPending();
  }

  /** Persiste o cartão de rotina sintetizado para um ciclo de pesquisa. */
  @PostMapping("/internal/oprm/nichocnae/routine-synthesizer/stage-executions/{researchCycleId}/complete")
  public ResponseEntity<CompleteRoutineSynthesizerResponse> complete(
      @PathVariable Long researchCycleId, @RequestBody CompleteRoutineSynthesizerRequest request) {
    return ResponseEntity.ok(executionService.complete(researchCycleId, request));
  }

  /** Registra falha operacional da síntese de rotina para um ciclo de pesquisa. */
  @PostMapping("/internal/oprm/nichocnae/routine-synthesizer/stage-executions/{researchCycleId}/fail")
  public ResponseEntity<Void> fail(@PathVariable Long researchCycleId, @RequestBody FailRoutineSynthesizerRequest request) {
    executionService.fail(researchCycleId, request);
    return ResponseEntity.noContent().build();
  }

  /** Detalha o cartão de rotina sintetizado para um ciclo de pesquisa. */
  @GetMapping("/oprm/nichocnae/routine-synthesizer/stage-executions/{researchCycleId}")
  public ResponseEntity<RoutineSynthesizerDetailResponse> detail(@PathVariable Long researchCycleId) {
    return ResponseEntity.ok(executionService.detail(researchCycleId));
  }
}
