package com.marketinghub.oprm.nichocnae.enrichednichematerializer.web;

import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.BackendEnrichedNicheMaterializerService;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerRequest;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.detailStageExecution.EnrichedNicheMaterializerDetailResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.failStageExecution.FailEnrichedNicheMaterializerRequest;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.pending.RecordEnrichedNicheMaterializerPending;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller OPRM da etapa final que materializa nicho e nicho enriquecido. */
@RestController
@RequestMapping
public class BackendEnrichedNicheMaterializerController {
  private final BackendEnrichedNicheMaterializerService executionService;

  /** Inicializa o controller com o serviço backend da etapa final. */
  public BackendEnrichedNicheMaterializerController(BackendEnrichedNicheMaterializerService executionService) {
    this.executionService = executionService;
  }

  /** Lista unidades de trabalho fechadas para o coletor materializar nichos enriquecidos. */
  @GetMapping("/api/internal/oprm/nichocnae/enriched-niche-materializer/stage-executions/pending")
  public List<RecordEnrichedNicheMaterializerPending> pending() {
    return executionService.listPending();
  }

  /** Conclui a etapa final alimentando as tabelas de nicho e nicho enriquecido. */
  @PostMapping("/api/internal/oprm/nichocnae/enriched-niche-materializer/stage-executions/{researchCycleId}/complete")
  public ResponseEntity<CompleteEnrichedNicheMaterializerResponse> complete(
      @PathVariable Long researchCycleId, @RequestBody CompleteEnrichedNicheMaterializerRequest request) {
    return ResponseEntity.ok(executionService.complete(researchCycleId, request));
  }

  /** Registra uma falha da etapa final no ciclo de pesquisa. */
  @PostMapping("/api/internal/oprm/nichocnae/enriched-niche-materializer/stage-executions/{researchCycleId}/fail")
  public ResponseEntity<Void> fail(@PathVariable Long researchCycleId, @RequestBody FailEnrichedNicheMaterializerRequest request) {
    executionService.fail(researchCycleId, request);
    return ResponseEntity.noContent().build();
  }

  /** Exibe o resultado da materialização final para a tela do pipeline. */
  @GetMapping("/api/oprm/nichocnae/enriched-niche-materializer/stage-executions/{researchCycleId}")
  public ResponseEntity<EnrichedNicheMaterializerDetailResponse> detail(@PathVariable Long researchCycleId) {
    return ResponseEntity.ok(executionService.detail(researchCycleId));
  }
}
