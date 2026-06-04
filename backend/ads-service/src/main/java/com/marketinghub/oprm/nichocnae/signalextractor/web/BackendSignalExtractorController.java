package com.marketinghub.oprm.nichocnae.signalextractor.web;

import com.marketinghub.oprm.nichocnae.signalextractor.service.BackendSignalExtractorService;
import com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.CompleteSignalExtractorRequest;
import com.marketinghub.oprm.nichocnae.signalextractor.service.completeStageExecution.CompleteSignalExtractorResponse;
import com.marketinghub.oprm.nichocnae.signalextractor.service.detailStageExecution.SignalExtractorDetailResponse;
import com.marketinghub.oprm.nichocnae.signalextractor.service.failStageExecution.FailSignalExtractorRequest;
import com.marketinghub.oprm.nichocnae.signalextractor.service.pending.RecordSignalExtractorPending;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por expor os endpoints backend da etapa cinco do pipeline OPRM nichocnae. */
@RestController
@RequestMapping("/api")
public class BackendSignalExtractorController {
  private final BackendSignalExtractorService executionService;

  /** Inicializa o controller com o serviço backend da etapa cinco de extração de sinais. */
  public BackendSignalExtractorController(BackendSignalExtractorService executionService) {
    this.executionService = executionService;
  }

  /** Lista snapshots curtos pendentes para extração estruturada de sinais por worker externo. */
  @GetMapping("/internal/oprm/nichocnae/signal-extractor/stage-executions/pending")
  public List<RecordSignalExtractorPending> pending() {
    return executionService.listPending();
  }

  /** Persiste sinais estruturados extraídos de um snapshot curto selecionado. */
  @PostMapping("/internal/oprm/nichocnae/signal-extractor/stage-executions/{sourceSnapshotId}/complete")
  public ResponseEntity<CompleteSignalExtractorResponse> complete(
      @PathVariable Long sourceSnapshotId, @RequestBody CompleteSignalExtractorRequest request) {
    return ResponseEntity.ok(executionService.complete(sourceSnapshotId, request));
  }

  /** Registra falha operacional de extração de sinais para um snapshot curto. */
  @PostMapping("/internal/oprm/nichocnae/signal-extractor/stage-executions/{sourceSnapshotId}/fail")
  public ResponseEntity<Void> fail(@PathVariable Long sourceSnapshotId, @RequestBody FailSignalExtractorRequest request) {
    executionService.fail(sourceSnapshotId, request);
    return ResponseEntity.noContent().build();
  }

  /** Detalha os sinais extraídos para um ciclo de pesquisa. */
  @GetMapping("/oprm/nichocnae/signal-extractor/stage-executions/{researchCycleId}")
  public ResponseEntity<SignalExtractorDetailResponse> detail(@PathVariable Long researchCycleId) {
    return ResponseEntity.ok(executionService.detail(researchCycleId));
  }
}
