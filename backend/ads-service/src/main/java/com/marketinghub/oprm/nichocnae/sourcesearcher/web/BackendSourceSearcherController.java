package com.marketinghub.oprm.nichocnae.sourcesearcher.web;

import com.marketinghub.oprm.nichocnae.sourcesearcher.service.BackendSourceSearcherService;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution.CompleteSourceSearcherRequest;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.completeStageExecution.CompleteSourceSearcherResponse;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.detailStageExecution.SourceSearcherDetailResponse;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.failStageExecution.FailSourceSearcherRequest;
import com.marketinghub.oprm.nichocnae.sourcesearcher.service.pending.RecordSourceSearcherPending;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por expor os endpoints backend da etapa três do pipeline OPRM nichocnae. */
@RestController
@RequestMapping("/api")
public class BackendSourceSearcherController {
  private final BackendSourceSearcherService executionService;

  /** Inicializa o controller com o serviço backend da etapa três de busca de fontes. */
  public BackendSourceSearcherController(BackendSourceSearcherService executionService) {
    this.executionService = executionService;
  }

  /** Lista frases pendentes para execução por um provedor de busca configurável. */
  @GetMapping("/internal/oprm/nichocnae/source-searcher/stage-executions/pending")
  public List<RecordSourceSearcherPending> pending() {
    return executionService.listPending();
  }

  /** Persiste os resultados encontrados por uma busca pública para uma frase de pesquisa. */
  @PostMapping("/internal/oprm/nichocnae/source-searcher/stage-executions/{researchQueryId}/complete")
  public ResponseEntity<CompleteSourceSearcherResponse> complete(
      @PathVariable Long researchQueryId, @RequestBody CompleteSourceSearcherRequest request) {
    return ResponseEntity.ok(executionService.complete(researchQueryId, request));
  }

  /** Registra falha operacional da busca de fontes para uma frase de pesquisa. */
  @PostMapping("/internal/oprm/nichocnae/source-searcher/stage-executions/{researchQueryId}/fail")
  public ResponseEntity<Void> fail(@PathVariable Long researchQueryId, @RequestBody FailSourceSearcherRequest request) {
    executionService.fail(researchQueryId, request);
    return ResponseEntity.noContent().build();
  }

  /** Detalha as fontes candidatas já encontradas para um ciclo de pesquisa. */
  @GetMapping("/oprm/nichocnae/source-searcher/stage-executions/{researchCycleId}")
  public ResponseEntity<SourceSearcherDetailResponse> detail(@PathVariable Long researchCycleId) {
    return ResponseEntity.ok(executionService.detail(researchCycleId));
  }
}
