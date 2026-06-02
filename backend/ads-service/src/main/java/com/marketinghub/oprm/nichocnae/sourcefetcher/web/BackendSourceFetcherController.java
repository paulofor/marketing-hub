package com.marketinghub.oprm.nichocnae.sourcefetcher.web;

import com.marketinghub.oprm.nichocnae.sourcefetcher.service.BackendSourceFetcherService;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution.CompleteSourceFetcherRequest;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.completeStageExecution.CompleteSourceFetcherResponse;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.detailStageExecution.SourceFetcherDetailResponse;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.failStageExecution.FailSourceFetcherRequest;
import com.marketinghub.oprm.nichocnae.sourcefetcher.service.pending.RecordSourceFetcherPending;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por expor os endpoints backend da etapa quatro do pipeline OPRM nichocnae. */
@RestController
@RequestMapping("/api")
public class BackendSourceFetcherController {
  private final BackendSourceFetcherService executionService;

  /** Inicializa o controller com o serviço backend da etapa quatro de coleta curta de fontes. */
  public BackendSourceFetcherController(BackendSourceFetcherService executionService) {
    this.executionService = executionService;
  }

  /** Lista fontes candidatas pendentes para coleta curta por worker externo. */
  @GetMapping("/internal/oprm/nichocnae/source-fetcher/stage-executions/pending")
  public List<RecordSourceFetcherPending> pending() {
    return executionService.listPending();
  }

  /** Persiste metadados e trecho curto coletados para uma fonte candidata selecionada. */
  @PostMapping("/internal/oprm/nichocnae/source-fetcher/stage-executions/{sourceCandidateId}/complete")
  public ResponseEntity<CompleteSourceFetcherResponse> complete(
      @PathVariable Long sourceCandidateId, @RequestBody CompleteSourceFetcherRequest request) {
    return ResponseEntity.ok(executionService.complete(sourceCandidateId, request));
  }

  /** Registra falha ou rejeição operacional de coleta para uma fonte candidata. */
  @PostMapping("/internal/oprm/nichocnae/source-fetcher/stage-executions/{sourceCandidateId}/fail")
  public ResponseEntity<Void> fail(@PathVariable Long sourceCandidateId, @RequestBody FailSourceFetcherRequest request) {
    executionService.fail(sourceCandidateId, request);
    return ResponseEntity.noContent().build();
  }

  /** Detalha os snapshots curtos já coletados para um ciclo de pesquisa. */
  @GetMapping("/oprm/nichocnae/source-fetcher/stage-executions/{researchCycleId}")
  public ResponseEntity<SourceFetcherDetailResponse> detail(@PathVariable Long researchCycleId) {
    return ResponseEntity.ok(executionService.detail(researchCycleId));
  }
}
