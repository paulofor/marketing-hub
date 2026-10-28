package com.marketinghub.chatgptads.v1.controller;

import com.marketinghub.chatgptads.v1.service.ChatgptAdsService;
import com.marketinghub.chatgptads.v1.service.managecampaign.ChatgptAdsContracts.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Expõe preparação administrativa e fila canônica do Hermes para ChatGPT Ads. */
@RestController
public class ChatgptAdsController {
  private final ChatgptAdsService service;
  private final com.marketinghub.chatgptads.v1.service.ChatgptAdsRequestAuth auth;
  private final com.fasterxml.jackson.databind.ObjectMapper mapper;

  /** Injeta o único coordenador das operações do canal. */
  public ChatgptAdsController(
      ChatgptAdsService service,
      com.marketinghub.chatgptads.v1.service.ChatgptAdsRequestAuth auth,
      com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.service = service;
    this.auth = auth;
    this.mapper = mapper;
  }

  /** Lista campanhas e conexão sem expor chaves. */
  @GetMapping("/api/chatgpt-ads/v1")
  public Overview list(@RequestParam(required = false) Long experimentId) {
    return service.overview(experimentId);
  }

  /** Cria rascunho local sem gasto. */
  @PostMapping("/api/chatgpt-ads/v1/campaigns")
  public CampaignView create(@RequestBody Draft draft) {
    return service.save(null, draft);
  }

  /** Edita rascunho e revoga sua autorização anterior. */
  @PutMapping("/api/chatgpt-ads/v1/campaigns/{id}")
  public CampaignView edit(@PathVariable long id, @RequestBody Draft draft) {
    return service.save(id, draft);
  }

  /** Solicita diagnóstico da conta no executor. */
  @PostMapping("/api/chatgpt-ads/v1/connection/check")
  public void connect() {
    if (!auth.configured())
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
          "Configure a conexão ChatGPT Ads no Hermes antes de verificar a conta.");
    service.connect();
  }

  /** Registra limite próprio do canal por decisão expressa do operador. */
  @PostMapping("/api/chatgpt-ads/v1/campaigns/{id}/authorize")
  public CampaignView authorize(@PathVariable long id, @RequestBody Authorization request) {
    return service.authorize(id, request);
  }

  /** Publica recursos pausados. */
  @PostMapping("/api/chatgpt-ads/v1/campaigns/{id}/publish")
  public CampaignView publish(@PathVariable long id) {
    return service.publish(id);
  }

  /** Solicita ativação dentro do teto e dos gates atuais. */
  @PostMapping("/api/chatgpt-ads/v1/campaigns/{id}/activate")
  public CampaignView activate(@PathVariable long id) {
    return service.activate(id);
  }

  /** Solicita interrupção de gasto pelo provedor. */
  @PostMapping("/api/chatgpt-ads/v1/campaigns/{id}/pause")
  public CampaignView pause(@PathVariable long id) {
    return service.pause(id);
  }

  /** Reconsulta revisão e elegibilidade sem duplicar campanha. */
  @PostMapping("/api/chatgpt-ads/v1/campaigns/{id}/sync")
  public CampaignView sync(@PathVariable long id) {
    return service.sync(id);
  }

  /** Recupera a operação original mantendo idempotência. */
  @PostMapping("/api/chatgpt-ads/v1/campaigns/{id}/retry")
  public CampaignView retry(@PathVariable long id) {
    return service.retry(id);
  }

  /** Reserva uma única operação pendente para o executor oficial. */
  @PostMapping("/api/internal/chatgpt-ads/v1/delivery/stage-executions/pending")
  public ResponseEntity<String> pending(
      @RequestBody(required = false) String body,
      @RequestHeader org.springframework.http.HttpHeaders headers) {
    auth.verify(
        "/api/internal/chatgpt-ads/v1/delivery/stage-executions/pending",
        body == null ? "" : body,
        headers);
    var work = service.pending();
    return signed(
        work == null ? 204 : 200, headers, work == null ? "" : mapper.valueToTree(work).toString());
  }

  /** Registra resposta e deixa a coordenação da próxima etapa no backend. */
  @PostMapping("/api/internal/chatgpt-ads/v1/delivery/stage-executions/{id}/result")
  public ResponseEntity<String> receive(
      @PathVariable long id,
      @RequestBody String body,
      @RequestHeader org.springframework.http.HttpHeaders headers) {
    auth.verify(
        "/api/internal/chatgpt-ads/v1/delivery/stage-executions/" + id + "/result", body, headers);
    try {
      service.receive(id, mapper.readValue(body, Receipt.class));
      return signed(200, headers, "{}");
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .error("ChatGPT Ads recibo inválido operação={}", id, ex);
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Recibo inválido.");
    }
  }

  /** Assina também o corpo de resposta para o executor conferir sua origem. */
  private ResponseEntity<String> signed(
      int status, org.springframework.http.HttpHeaders headers, String body) {
    return ResponseEntity.status(status)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .header(
            "X-Mkt-Ads-Response",
            auth.signResponse(status, headers.getFirst("X-Mkt-Ads-Nonce"), body))
        .body(body);
  }
}
