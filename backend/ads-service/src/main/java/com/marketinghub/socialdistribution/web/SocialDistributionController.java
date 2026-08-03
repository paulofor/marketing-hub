package com.marketinghub.socialdistribution.web;

import com.marketinghub.socialdistribution.SocialPlatform;
import com.marketinghub.socialdistribution.dto.SocialDistributionDtos.*;
import com.marketinghub.socialdistribution.service.SocialDistributionService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor endpoints REST da distribuição orgânica de vídeos. */
@RestController
@RequestMapping("/api/social-distribution")
public class SocialDistributionController {
  private final SocialDistributionService service;

  /** Inicializa o controller com o serviço de distribuição orgânica. */
  public SocialDistributionController(SocialDistributionService service) {
    this.service = service;
  }

  /** Lista contas sociais configuradas para publicação. */
  @GetMapping("/accounts")
  public List<SocialAccountResponse> listAccounts(
      @RequestParam(required = false) SocialPlatform platform) {
    return service.listAccounts(platform);
  }

  /** Cadastra uma conta social com pendências de conexão. */
  @PostMapping("/accounts")
  public SocialAccountResponse createAccount(@RequestBody SaveSocialAccountRequest request) {
    return service.createAccount(request);
  }

  /** Lista publicações orgânicas de vídeos. */
  @GetMapping("/publications")
  public List<SocialVideoPublicationResponse> listPublications(
      @RequestParam(required = false) Long productId,
      @RequestParam(required = false) SocialPlatform platform) {
    return service.listPublications(productId, platform);
  }

  /** Cria uma publicação orgânica em rascunho. */
  @PostMapping("/publications")
  public SocialVideoPublicationResponse createPublication(
      @RequestBody CreateSocialVideoPublicationRequest request) {
    return service.createPublication(request);
  }

  /** Coloca uma publicação na fila quando a conta está pronta. */
  @PostMapping("/publications/{id}/queue")
  public SocialVideoPublicationResponse queuePublication(@PathVariable Long id) {
    return service.queuePublication(id);
  }

  /** Lista publicações pendentes para um executor oficial futuro. */
  @GetMapping("/publications/pending")
  public List<SocialVideoPublicationResponse> listQueuedPublications() {
    return service.listQueuedPublications();
  }

  /** Marca uma publicação como em execução pelo worker oficial. */
  @PostMapping("/publications/{id}/publishing")
  public SocialVideoPublicationResponse markPublishing(@PathVariable Long id) {
    return service.markPublishing(id);
  }

  /** Marca uma publicação como publicada e registra o link final. */
  @PostMapping("/publications/{id}/published")
  public SocialVideoPublicationResponse markPublished(
      @PathVariable Long id, @RequestBody MarkSocialVideoPublishedRequest request) {
    return service.markPublished(id, request);
  }

  /** Marca uma publicação como falha após retorno do worker oficial. */
  @PostMapping("/publications/{id}/failed")
  public SocialVideoPublicationResponse markFailed(
      @PathVariable Long id, @RequestBody MarkSocialVideoFailedRequest request) {
    return service.markFailed(id, request);
  }

  /** Registra uma leitura de métricas da publicação. */
  @PostMapping("/publications/{id}/metrics")
  public SocialPublicationMetricResponse recordMetric(
      @PathVariable Long id, @RequestBody RecordSocialPublicationMetricRequest request) {
    return service.recordMetric(id, request);
  }

  /** Lista planos de crescimento orgânico e seus resultados atribuídos. */
  @GetMapping("/growth-plans")
  public List<SocialGrowthPlanResponse> listGrowthPlans(
      @RequestParam(required = false) Long productId) {
    return service.listGrowthPlans(productId);
  }

  /** Cria um plano de crescimento orgânico em rascunho. */
  @PostMapping("/growth-plans")
  public SocialGrowthPlanResponse createGrowthPlan(
      @RequestBody CreateSocialGrowthPlanRequest request) {
    return service.createGrowthPlan(request);
  }

  /** Adiciona uma pauta rastreável ao calendário do plano. */
  @PostMapping("/growth-plans/{planId}/contents")
  public SocialGrowthContentResponse createGrowthContent(
      @PathVariable Long planId, @RequestBody CreateSocialGrowthContentRequest request) {
    return service.createGrowthContent(planId, request);
  }

  /** Registra aprovação humana antes de permitir vínculo com uma publicação. */
  @PostMapping("/growth-plans/{planId}/contents/{contentId}/approve")
  public SocialGrowthContentResponse approveGrowthContent(
      @PathVariable Long planId, @PathVariable Long contentId) {
    return service.approveGrowthContent(planId, contentId);
  }
}
