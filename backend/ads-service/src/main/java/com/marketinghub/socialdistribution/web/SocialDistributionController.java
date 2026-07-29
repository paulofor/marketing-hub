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

  /** Marca uma publicação como publicada e registra o link final. */
  @PostMapping("/publications/{id}/published")
  public SocialVideoPublicationResponse markPublished(
      @PathVariable Long id, @RequestBody MarkSocialVideoPublishedRequest request) {
    return service.markPublished(id, request);
  }

  /** Registra uma leitura de métricas da publicação. */
  @PostMapping("/publications/{id}/metrics")
  public SocialPublicationMetricResponse recordMetric(
      @PathVariable Long id, @RequestBody RecordSocialPublicationMetricRequest request) {
    return service.recordMetric(id, request);
  }
}
