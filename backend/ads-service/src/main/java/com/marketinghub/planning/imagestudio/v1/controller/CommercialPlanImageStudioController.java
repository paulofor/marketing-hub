package com.marketinghub.planning.imagestudio.v1.controller;

import com.marketinghub.planning.imagestudio.v1.service.CommercialPlanImageStudioJobDto;
import com.marketinghub.planning.imagestudio.v1.service.CommercialPlanImageStudioPendingDto;
import com.marketinghub.planning.imagestudio.v1.service.CommercialPlanImageStudioService;
import com.marketinghub.planning.imagestudio.v1.service.CommercialPlanVisualAssetReviewPendingDto;
import com.marketinghub.planning.imagestudio.v1.service.CommercialPlanVisualAssetReviewResultRequest;
import com.marketinghub.planning.imagestudio.v1.service.CreateCommercialPlanImageStudioJobRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Responsabilidade: expor o Estúdio de Imagens v1 do plano comercial e suas filas internas. */
@RestController
@RequestMapping
public class CommercialPlanImageStudioController {
  private final CommercialPlanImageStudioService service;

  /** Inicializa o controller único do Estúdio de Imagens. */
  public CommercialPlanImageStudioController(CommercialPlanImageStudioService service) {
    this.service = service;
  }

  /** Solicita pela tela uma criação ou edição de entregável visual. */
  @PostMapping("/api/planning/commercial-plans/{planId}/image-studio/jobs")
  public CommercialPlanImageStudioJobDto create(
      @PathVariable Long planId, @RequestBody CreateCommercialPlanImageStudioJobRequest request) {
    return service.create(planId, request);
  }

  /** Lista o histórico persistido de produções do plano. */
  @GetMapping("/api/planning/commercial-plans/{planId}/image-studio/jobs")
  public List<CommercialPlanImageStudioJobDto> list(@PathVariable Long planId) {
    return service.list(planId);
  }

  /** Entrega a Têmis a fila canônica de criação e edição. */
  @GetMapping("/api/internal/planning/image-studio/v1/stage-executions/pending")
  public List<CommercialPlanImageStudioPendingDto> pending(
      @RequestParam(defaultValue = "2") int limit) {
    return service.claimPending(limit);
  }

  /** Recebe o arquivo produzido por Têmis e abre o gate independente. */
  @PostMapping(
      value = "/api/internal/planning/image-studio/v1/stage-executions/{jobId}/artifact",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CommercialPlanImageStudioJobDto artifact(
      @PathVariable Long jobId,
      @RequestParam String producerExecutionId,
      @RequestParam MultipartFile file,
      @RequestParam String model,
      @RequestParam String requestJson,
      @RequestParam String responseJson,
      @RequestParam(required = false) String usageJson,
      @RequestParam(required = false) BigDecimal costUsd)
      throws IOException {
    return service.complete(
        jobId, producerExecutionId, file, model, requestJson, responseJson, usageJson, costUsd);
  }

  /** Registra falha de produção sem criar item inválido na biblioteca. */
  @PostMapping("/api/internal/planning/image-studio/v1/stage-executions/{jobId}/failure")
  public CommercialPlanImageStudioJobDto failure(
      @PathVariable Long jobId, @RequestBody ImageStudioFailureRequest request) {
    return service.fail(
        jobId,
        request.producerExecutionId(),
        request.error(),
        request.requestJson(),
        request.responseJson());
  }

  /** Entrega a outra execução de Têmis os itens que aguardam revisão. */
  @GetMapping("/api/internal/planning/image-studio/v1/reviews/pending")
  public List<CommercialPlanVisualAssetReviewPendingDto> pendingReviews(
      @RequestParam(defaultValue = "2") int limit) {
    return service.claimReviews(limit);
  }

  /** Expõe ao MCP o snapshot efetivo do entregável reservado. */
  @GetMapping("/api/internal/planning/image-studio/v1/reviews/{assetId}/context")
  public CommercialPlanVisualAssetReviewPendingDto reviewContext(
      @PathVariable Long assetId, @RequestParam Long planId) {
    return service.reviewContext(assetId, planId);
  }

  /** Persiste o parecer independente e deixa o backend promover ou manter DRAFT. */
  @PostMapping("/api/internal/planning/image-studio/v1/reviews/{assetId}/result")
  public void review(
      @PathVariable Long assetId,
      @RequestBody CommercialPlanVisualAssetReviewResultRequest request) {
    service.review(assetId, request);
  }

  /** Responsabilidade: transportar uma falha técnica auditável de Têmis. */
  public record ImageStudioFailureRequest(
      String producerExecutionId, String error, String requestJson, String responseJson) {}
}
