package com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.web;

import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.BackendMeiAudienceSegmenterService;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.completeStageExecution.CompleteMeiAudienceSegmenterRequest;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.completeStageExecution.CompleteMeiAudienceSegmenterResponse;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.failStageExecution.FailMeiAudienceSegmenterRequest;
import com.marketinghub.oprm.nichocnae.meiaudiencesegmenter.service.pending.RecordMeiAudienceSegmenterPending;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller responsável pelos endpoints internos da segmentação comportamental MEI/autônomo do OPRM. */
@RestController
@RequestMapping("/api")
public class BackendMeiAudienceSegmenterController {
  private final BackendMeiAudienceSegmenterService executionService;

  /** Inicializa o controller com o serviço backend da segmentação comportamental MEI/autônomo. */
  public BackendMeiAudienceSegmenterController(BackendMeiAudienceSegmenterService executionService) {
    this.executionService = executionService;
  }

  /** Lista cartões de rotina pendentes de segmentação comportamental por IA. */
  @GetMapping("/internal/oprm/nichocnae/mei-audience-segmenter/stage-executions/pending")
  public List<RecordMeiAudienceSegmenterPending> pending() {
    return executionService.listPending();
  }

  /** Persiste a segmentação comportamental validada para um ciclo de pesquisa. */
  @PostMapping("/internal/oprm/nichocnae/mei-audience-segmenter/stage-executions/{researchCycleId}/complete")
  public ResponseEntity<CompleteMeiAudienceSegmenterResponse> complete(
      @PathVariable Long researchCycleId, @RequestBody CompleteMeiAudienceSegmenterRequest request) {
    return ResponseEntity.ok(executionService.complete(researchCycleId, request));
  }

  /** Registra falha operacional da segmentação comportamental de um ciclo. */
  @PostMapping("/internal/oprm/nichocnae/mei-audience-segmenter/stage-executions/{researchCycleId}/fail")
  public ResponseEntity<Void> fail(@PathVariable Long researchCycleId, @RequestBody FailMeiAudienceSegmenterRequest request) {
    executionService.fail(researchCycleId, request);
    return ResponseEntity.noContent().build();
  }
}
