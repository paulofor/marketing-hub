package com.marketinghub.oprm.nichocnae.routineresearchcycle.web;

import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.BackendRoutineResearchCycleService;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.detailStageExecution.RecordBackendRoutineResearchCycleDetalheDto;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.listRecentJobs.OprmNichoCnaeJobsPageResponse;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.listStageExecutions.RoutineResearchCycleExecutionSummaryResponse;
import com.marketinghub.oprm.nichocnae.routineresearchcycle.service.pending.RecordRoutineResearchCyclePending;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsável por expor os endpoints backend da etapa de ciclo da pesquisa de rotina de nicho CNAE. */
@RestController
@RequestMapping("/api")
public class BackendRoutineResearchCycleController {
  private final BackendRoutineResearchCycleService executionService;

  /** Inicializa o controller com o serviço backend da etapa de ciclo da pesquisa de rotina. */
  public BackendRoutineResearchCycleController(BackendRoutineResearchCycleService executionService) {
    this.executionService = executionService;
  }

  /** Lista ciclos de pesquisa de rotina pendentes para processamento interno pelo Worker AI. */
  @GetMapping("/internal/oprm/nichocnae/routine-research-cycle/stage-executions/pending")
  public List<RecordRoutineResearchCyclePending> pending() {
    return executionService.listPending();
  }

  /** Lista jobs recentes do OPRM NichoCNAE para acompanhamento administrativo paginado. */
  @GetMapping("/oprm/nichocnae/jobs")
  public ResponseEntity<OprmNichoCnaeJobsPageResponse> listRecentJobs(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(executionService.listRecentJobs(page, size));
  }

  /** Lista execuções do ciclo de pesquisa de rotina vinculadas ao CNAE informado. */
  @GetMapping("/oprm/nichocnae/cnaes/{cnaeCode}/routine-research-cycle/stage-executions")
  public ResponseEntity<List<RoutineResearchCycleExecutionSummaryResponse>> listStageExecutionsByCnae(
      @PathVariable String cnaeCode) {
    return ResponseEntity.ok(executionService.listStageExecutionsByCnae(cnaeCode));
  }

  /** Lista execuções do ciclo de pesquisa de rotina vinculadas ao nicho CNAE informado. */
  @GetMapping("/oprm/nichocnae/{sourceNicheId}/routine-research-cycle/stage-executions")
  public ResponseEntity<List<RoutineResearchCycleExecutionSummaryResponse>> listStageExecutions(
      @PathVariable Long sourceNicheId) {
    return ResponseEntity.ok(executionService.listStageExecutions(sourceNicheId));
  }

  /** Retorna detalhes de uma execução específica do ciclo de pesquisa de rotina. */
  @GetMapping("/oprm/nichocnae/routine-research-cycle/stage-executions/{researchCycleId}")
  public ResponseEntity<RecordBackendRoutineResearchCycleDetalheDto> detailStageExecution(
      @PathVariable Long researchCycleId) {
    return ResponseEntity.ok(executionService.detailStageExecution(researchCycleId));
  }

  /** Baixa um relatório Markdown com a auditoria completa da execução informada. */
  @GetMapping(
      value = "/oprm/nichocnae/routine-research-cycle/stage-executions/{researchCycleId}/report",
      produces = "text/markdown")
  public ResponseEntity<String> downloadMarkdownReport(@PathVariable Long researchCycleId) {
    String filename = "nicho-cnae" + researchCycleId + ".md";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentDisposition(
        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
    headers.setContentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"));
    return ResponseEntity.ok().headers(headers).body(executionService.buildMarkdownReport(researchCycleId));
  }
}
