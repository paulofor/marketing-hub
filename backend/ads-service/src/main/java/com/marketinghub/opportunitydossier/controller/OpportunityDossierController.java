package com.marketinghub.opportunitydossier.controller;

import com.marketinghub.opportunitydossier.service.OpportunityDossierService;
import com.marketinghub.opportunitydossier.service.OpportunityReviewExecutionService;
import com.marketinghub.opportunitydossier.service.convert.ConvertOpportunityRequest;
import com.marketinghub.opportunitydossier.service.create.CreateOpportunityDossierRequest;
import com.marketinghub.opportunitydossier.service.detail.OpportunityDossierResponse;
import com.marketinghub.opportunitydossier.service.evidence.AddOpportunityEvidenceRequest;
import com.marketinghub.opportunitydossier.service.review.SubmitOpportunityReviewRequest;
import com.marketinghub.opportunitydossier.service.status.UpdateOpportunityStatusRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor o cadastro e a governança dos dossiês de oportunidade. */
@RestController
@RequestMapping("/api/opportunity-dossiers")
public class OpportunityDossierController {
  private final OpportunityDossierService service;
  private final OpportunityReviewExecutionService executions;

  /** Configura a governança administrativa e a fila interna dos pareceres. */
  public OpportunityDossierController(
      OpportunityDossierService service, OpportunityReviewExecutionService executions) {
    this.service = service;
    this.executions = executions;
  }

  /** Cadastra uma oportunidade. */
  @PostMapping
  public OpportunityDossierResponse create(@RequestBody CreateOpportunityDossierRequest request) {
    return service.create(request);
  }

  /** Lista oportunidades. */
  @GetMapping
  public List<OpportunityDossierResponse> list() {
    return service.list();
  }

  /** Exibe uma oportunidade. */
  @GetMapping("/{id}")
  public OpportunityDossierResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  /** Anexa evidência. */
  @PostMapping("/{id}/evidence")
  public OpportunityDossierResponse evidence(
      @PathVariable Long id, @RequestBody AddOpportunityEvidenceRequest request) {
    return service.addEvidence(id, request);
  }

  /** Atualiza estado governado. */
  @PatchMapping("/{id}/status")
  public OpportunityDossierResponse status(
      @PathVariable Long id, @RequestBody UpdateOpportunityStatusRequest request) {
    return service.updateStatus(id, request);
  }

  /** Recebe parecer de agente solicitado. */
  @PutMapping("/{id}/reviews/{agentKey}")
  public OpportunityDossierResponse review(
      @PathVariable Long id,
      @PathVariable String agentKey,
      @RequestBody SubmitOpportunityReviewRequest request) {
    return service.submitReview(id, agentKey, request);
  }

  /** Converte uma oportunidade aprovada em plano. */
  @PostMapping("/{id}/convert")
  public OpportunityDossierResponse convert(
      @PathVariable Long id, @RequestBody ConvertOpportunityRequest request) {
    return service.convert(id, request);
  }

  /** Reserva o próximo parecer pendente exclusivamente para o agente indicado. */
  @PostMapping("/internal/reviews/{agentKey}/stage-executions/pending")
  public com.marketinghub.opportunitydossier.service.review.OpportunityReviewJobResponse pending(
      @PathVariable String agentKey) {
    return executions.claim(agentKey);
  }

  /** Recebe o parecer funcional e sua auditoria bruta. */
  @PostMapping("/internal/reviews/{agentKey}/stage-executions/{reviewId}/complete")
  public void complete(
      @PathVariable String agentKey,
      @PathVariable Long reviewId,
      @RequestBody
          com.marketinghub.opportunitydossier.service.review.CompleteOpportunityReviewRequest
              request) {
    executions.complete(agentKey, reviewId, request);
  }

  /** Recebe uma falha técnica do executor sem concluir o parecer. */
  @PostMapping("/internal/reviews/{agentKey}/stage-executions/{reviewId}/fail")
  public void fail(
      @PathVariable String agentKey,
      @PathVariable Long reviewId,
      @RequestBody
          com.marketinghub.opportunitydossier.service.review.FailOpportunityReviewRequest request) {
    executions.fail(agentKey, reviewId, request);
  }
}
