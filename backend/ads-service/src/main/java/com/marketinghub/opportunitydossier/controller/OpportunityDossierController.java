package com.marketinghub.opportunitydossier.controller;

import com.marketinghub.opportunitydossier.service.OpportunityDossierService;
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

  public OpportunityDossierController(OpportunityDossierService service) {
    this.service = service;
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
}
