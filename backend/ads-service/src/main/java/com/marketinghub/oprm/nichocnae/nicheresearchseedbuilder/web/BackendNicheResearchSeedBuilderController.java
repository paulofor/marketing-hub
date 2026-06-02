package com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.web;

import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.BackendNicheResearchSeedBuilderService;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.CompleteNicheResearchSeedBuilderRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.completeStageExecution.CompleteNicheResearchSeedBuilderResponse;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.detailStageExecution.NicheResearchSeedBuilderDetailResponse;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.failStageExecution.FailNicheResearchSeedBuilderRequest;
import com.marketinghub.oprm.nichocnae.nicheresearchseedbuilder.service.pending.RecordNicheResearchSeedBuilderPending;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por expor os endpoints backend da etapa dois do pipeline OPRM nichocnae. */
@RestController
@RequestMapping("/api")
public class BackendNicheResearchSeedBuilderController {
  private final BackendNicheResearchSeedBuilderService executionService;

  /** Inicializa o controller com o serviço backend da etapa dois de seed e queries. */
  public BackendNicheResearchSeedBuilderController(BackendNicheResearchSeedBuilderService executionService) {
    this.executionService = executionService;
  }

  /** Lista ciclos de pesquisa prontos para geração de seed operacional e frases de pesquisa. */
  @GetMapping("/internal/oprm/nichocnae/niche-research-seed-builder/stage-executions/pending")
  public List<RecordNicheResearchSeedBuilderPending> pending() {
    return executionService.listPending();
  }

  /** Persiste a saída validada da IA para seed operacional e queries da etapa dois. */
  @PostMapping("/internal/oprm/nichocnae/niche-research-seed-builder/stage-executions/{researchCycleId}/complete")
  public ResponseEntity<CompleteNicheResearchSeedBuilderResponse> complete(
      @PathVariable Long researchCycleId, @RequestBody CompleteNicheResearchSeedBuilderRequest request) {
    return ResponseEntity.ok(executionService.complete(researchCycleId, request));
  }

  /** Registra falha operacional da etapa dois em um ciclo de pesquisa de rotina. */
  @PostMapping("/internal/oprm/nichocnae/niche-research-seed-builder/stage-executions/{researchCycleId}/fail")
  public ResponseEntity<Void> fail(
      @PathVariable Long researchCycleId, @RequestBody FailNicheResearchSeedBuilderRequest request) {
    executionService.fail(researchCycleId, request);
    return ResponseEntity.noContent().build();
  }

  /** Detalha o seed e as frases de pesquisa geradas para uma execução da etapa dois. */
  @GetMapping("/oprm/nichocnae/niche-research-seed-builder/stage-executions/{researchCycleId}")
  public ResponseEntity<NicheResearchSeedBuilderDetailResponse> detail(@PathVariable Long researchCycleId) {
    return ResponseEntity.ok(executionService.detail(researchCycleId));
  }
}
