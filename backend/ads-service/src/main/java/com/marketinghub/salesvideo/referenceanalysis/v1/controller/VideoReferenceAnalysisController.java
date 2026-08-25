package com.marketinghub.salesvideo.referenceanalysis.v1.controller;

import com.marketinghub.salesvideo.referenceanalysis.v1.service.VideoReferenceAnalysisService;
import com.marketinghub.salesvideo.referenceanalysis.v1.service.complete.CompleteRequest;
import com.marketinghub.salesvideo.referenceanalysis.v1.service.execution.VideoReferenceAnalysisResponse;
import com.marketinghub.salesvideo.referenceanalysis.v1.service.fail.FailureRequest;
import com.marketinghub.salesvideo.referenceanalysis.v1.service.pending.Pending;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expõe os contratos canônicos da etapa v1 de análise de vídeos de referência. */
@RestController
public class VideoReferenceAnalysisController {
  private final VideoReferenceAnalysisService service;

  /** Inicializa o controller com a fonte de verdade da etapa. */
  public VideoReferenceAnalysisController(VideoReferenceAnalysisService service) {
    this.service = service;
  }

  /** Entrega ao executor no máximo uma pendência com lease atômico. */
  @GetMapping("/api/internal/sales-videos/reference-analysis/v1/analyze/stage-executions/pending")
  public List<Pending> pending(@RequestParam String workerId) {
    return service.claimPending(workerId);
  }

  /** Recebe o resultado completo e auditável produzido pelo executor. */
  @PostMapping(
      "/api/internal/sales-videos/reference-analysis/v1/analyze/stage-executions/{executionId}/complete")
  public VideoReferenceAnalysisResponse complete(
      @PathVariable Long executionId, @Valid @RequestBody CompleteRequest request) {
    return service.complete(executionId, request);
  }

  /** Recebe falha técnica preservando artefatos e auditoria disponíveis. */
  @PostMapping(
      "/api/internal/sales-videos/reference-analysis/v1/analyze/stage-executions/{executionId}/fail")
  public VideoReferenceAnalysisResponse fail(
      @PathVariable Long executionId, @Valid @RequestBody FailureRequest request) {
    return service.fail(executionId, request);
  }

  /** Expõe para a tela o resultado mais recente da referência no tenant atual. */
  @GetMapping("/api/sales-videos/reference-analysis/v1/references/{referenceId}/latest")
  public VideoReferenceAnalysisResponse latest(@PathVariable Long referenceId) {
    return service.latest(referenceId);
  }

  /** Solicita pela tela uma nova tentativa quando a última não está ativa. */
  @PostMapping("/api/sales-videos/reference-analysis/v1/references/{referenceId}/retry")
  public VideoReferenceAnalysisResponse retry(@PathVariable Long referenceId) {
    return service.retry(referenceId);
  }
}
