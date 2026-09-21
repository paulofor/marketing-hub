package com.marketinghub.gerasalespage.v1.web;

import com.marketinghub.gerasalespage.v1.service.GeraSalesPagePendingResponse;
import com.marketinghub.gerasalespage.v1.service.GeraSalesPagePromptRequest;
import com.marketinghub.gerasalespage.v1.service.GeraSalesPagePublicationAuditService;
import com.marketinghub.gerasalespage.v1.service.GeraSalesPagePublicationResponse;
import com.marketinghub.gerasalespage.v1.service.GeraSalesPageResultRequest;
import com.marketinghub.gerasalespage.v1.service.GeraSalesPageStageService;
import com.marketinghub.gerasalespage.v1.service.GeraSalesPageStartResponse;
import com.marketinghub.gerasalespage.v1.service.republish.PublicationRecoveryView;
import com.marketinghub.gerasalespage.v1.service.republish.RepublishPublicationRequest;
import com.marketinghub.gerasalespage.v1.service.retry.StageRetryRequest;
import com.marketinghub.gerasalespage.v1.service.retry.StageRetryView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor comandos e callbacks internos do GeraSalesPage v1. */
@RestController
@RequestMapping("/api")
public class GeraSalesPageController {
  private final GeraSalesPageStageService service;
  private final GeraSalesPagePublicationAuditService publicationAuditService;

  /** Inicializa o controller com o service do GeraSalesPage v1. */
  public GeraSalesPageController(
      GeraSalesPageStageService service,
      GeraSalesPagePublicationAuditService publicationAuditService) {
    this.service = service;
    this.publicationAuditService = publicationAuditService;
  }

  /** Inicia o GeraSalesPage v1 para um experimento com checkout real. */
  @PostMapping("/experiments/{experimentId}/gerasalespage/v1/start")
  public ResponseEntity<GeraSalesPageStartResponse> start(@PathVariable Long experimentId) {
    return ResponseEntity.accepted().body(service.start(experimentId));
  }

  /** Refaz o GeraSalesPage v1 substituindo execuções antigas por uma geração limpa. */
  @PostMapping("/experiments/{experimentId}/gerasalespage/v1/rebuild")
  public ResponseEntity<GeraSalesPageStartResponse> rebuild(@PathVariable Long experimentId) {
    return ResponseEntity.accepted().body(service.rebuild(experimentId));
  }

  /** Expõe a disponibilidade de retomada da última etapa sem disparar consumo de IA. */
  @GetMapping("/experiments/{experimentId}/gerasalespage/v1/stage-recovery")
  public StageRetryView stageRecovery(@PathVariable Long experimentId) {
    return service.retryView(experimentId);
  }

  /** Solicita uma única retomada técnica preservando o histórico da tentativa original. */
  @PostMapping("/experiments/{experimentId}/gerasalespage/v1/stage-recovery")
  public StageRetryView retryStage(
      @PathVariable Long experimentId, @Valid @RequestBody StageRetryRequest request) {
    return service.retry(experimentId, request.failedJobId());
  }

  /** Lista as versões publicadas da página com prompts e schemas usados em cada versão. */
  @GetMapping("/experiments/{experimentId}/gerasalespage/v1/publications")
  public List<GeraSalesPagePublicationResponse> publications(@PathVariable Long experimentId) {
    return publicationAuditService.listPublications(experimentId);
  }

  /** Confere se a publicação atual pode ser reenviada sem criar uma nova versão comercial. */
  @GetMapping("/experiments/{experimentId}/gerasalespage/v1/publications/{publicationId}/recovery")
  public PublicationRecoveryView recovery(
      @PathVariable Long experimentId, @PathVariable Long publicationId) {
    return publicationAuditService.recovery(experimentId, publicationId);
  }

  /** Recupera a identidade da mesma página auditada sem gerar novamente seu conteúdo. */
  @PostMapping(
      "/experiments/{experimentId}/gerasalespage/v1/publications/{publicationId}/republish")
  public PublicationRecoveryView republish(
      @PathVariable Long experimentId,
      @PathVariable Long publicationId,
      @Valid @RequestBody RepublishPublicationRequest request) {
    return publicationAuditService.republish(experimentId, publicationId, request);
  }

  /** Lista jobs pendentes de uma etapa para consumo canônico pelo AI Worker. */
  @GetMapping("/internal/gerasalespage/v1/{stageCode}/stage-executions/pending")
  public List<GeraSalesPagePendingResponse> pending(@PathVariable String stageCode) {
    return service.pending(stageCode);
  }

  /** Marca uma execução como em processamento. */
  @PostMapping("/internal/gerasalespage/v1/{stageCode}/stage-executions/{idJob}/running")
  public ResponseEntity<Void> running(@PathVariable String stageCode, @PathVariable String idJob) {
    service.markRunning(idJob);
    return ResponseEntity.accepted().build();
  }

  /** Recebe o prompt, schema e request bruto enviados à OpenAI. */
  @PostMapping("/internal/gerasalespage/v1/{stageCode}/stage-executions/{idJob}/recebe-prompt")
  public ResponseEntity<Void> recebePrompt(
      @PathVariable String stageCode,
      @PathVariable String idJob,
      @Valid @RequestBody GeraSalesPagePromptRequest payload) {
    service.receivePrompt(idJob, payload);
    return ResponseEntity.accepted().build();
  }

  /** Recebe a resposta ou falha da OpenAI e conclui a execução da etapa. */
  @PostMapping("/internal/gerasalespage/v1/{stageCode}/stage-executions/{idJob}/recebe-resposta")
  public ResponseEntity<Void> recebeResposta(
      @PathVariable String stageCode,
      @PathVariable String idJob,
      @Valid @RequestBody GeraSalesPageResultRequest payload) {
    service.receiveResult(idJob, payload);
    return ResponseEntity.accepted().build();
  }
}
