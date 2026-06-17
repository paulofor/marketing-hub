package com.marketinghub.oprm.nichocnae.enrichednichematerializer.web;

import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.BackendEnrichedNicheMaterializerService;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerRequest;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.completeStageExecution.CompleteEnrichedNicheMaterializerResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.detailStageExecution.EnrichedNicheMaterializerDetailResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.diagnoseContamination.ContaminatedNicheDiagnosticResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.failStageExecution.FailEnrichedNicheMaterializerRequest;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.generatedByCnae.GeneratedEnrichedNicheByCnaeResponse;
import com.marketinghub.oprm.nichocnae.enrichednichematerializer.service.pending.RecordEnrichedNicheMaterializerPending;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

  /** Exibe o perfil enriquecido materializado a partir do identificador do perfil. */
  @GetMapping("/api/oprm/nichocnae/enriched-niche-materializer/profiles/{profileId}")
  public ResponseEntity<EnrichedNicheMaterializerDetailResponse> detailByProfileId(@PathVariable Long profileId) {
    return ResponseEntity.ok(executionService.detailByProfileId(profileId));
  }

  /** Lista os nichos enriquecidos já materializados para um CNAE. */
  @GetMapping("/api/oprm/nichocnae/cnaes/{cnaeCode}/enriched-niches")
  public List<GeneratedEnrichedNicheByCnaeResponse> listGeneratedByCnae(
      @PathVariable String cnaeCode,
      @RequestParam(defaultValue = "50") int limit) {
    return executionService.listGeneratedByCnae(cnaeCode, limit);
  }

  /** Entrega um documento Markdown baixável com auditoria de todo o pipeline processado para o perfil. */
  @GetMapping(
      value = "/api/oprm/nichocnae/enriched-niche-materializer/profiles/{profileId}/pipeline-markdown",
      produces = "text/markdown;charset=UTF-8")
  public ResponseEntity<String> downloadPipelineMarkdownByProfileId(@PathVariable Long profileId) {
    String filename = "oprm-nicho-enriquecido-" + profileId + ".md";
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(executionService.buildPipelineMarkdownByProfileId(profileId));
  }


  /** Entrega um documento Markdown baixável com auditoria do job de pesquisa, materializado ou não. */
  @GetMapping(
      value = "/api/oprm/nichocnae/routine-research-cycle/stage-executions/{researchCycleId}/pipeline-markdown",
      produces = "text/markdown;charset=UTF-8")
  public ResponseEntity<String> downloadPipelineMarkdownByResearchCycleId(@PathVariable Long researchCycleId) {
    String filename = "oprm-job-" + researchCycleId + "-relatorio.md";
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(executionService.buildPipelineMarkdownByResearchCycleId(researchCycleId));
  }

  /** Diagnostica ciclos e perfis históricos com linguagem de solução para reprocessamento neutro. */
  @GetMapping("/api/oprm/nichocnae/enriched-niche-materializer/diagnostics/solution-contamination")
  public ResponseEntity<ContaminatedNicheDiagnosticResponse> diagnoseContamination(
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(executionService.diagnoseHistoricalContamination(limit));
  }

}
