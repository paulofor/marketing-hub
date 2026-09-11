package com.marketinghub.pde.vega.privateprototype.v1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.vega.privateprototype.v1.service.VegaPrivateService;
import com.marketinghub.pde.vega.privateprototype.v1.service.contract.VegaPrivateContract.*;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: expor contratos privados do Vega e proteger os callbacks de seus executores.
 */
@RestController
@RequestMapping("/api/pde/vega/private/v1")
public class VegaPrivateController {
  private final VegaPrivateService service;
  private final String internalToken;

  /**
   * Recebe o serviço e a credencial canônica do PDE, incluindo configuração por arquivo protegido.
   */
  public VegaPrivateController(
      VegaPrivateService service,
      @Value("${integrations.pde-platform.internal-token:${PDE_INTERNAL_API_TOKEN:}}")
          String internalToken) {
    this.service = service;
    this.internalToken = internalToken;
  }

  /** Informa as capacidades sanitizadas da experiência implementada. */
  @GetMapping("/contract")
  public JsonNode contract() {
    return service.contract();
  }

  /** Troca o convite da participante após consentimento. */
  @PostMapping("/access")
  public JsonNode access(@Valid @RequestBody Access input) {
    return service.access(input);
  }

  /** Recupera somente a leitura associada à credencial apresentada. */
  @GetMapping("/session")
  public JsonNode session(@RequestHeader("X-Vega-Session") String secret) {
    return service.session(secret);
  }

  /** Registra o toque explícito para começar, separado do carregamento da página. */
  @PostMapping("/start")
  public JsonNode start(@RequestHeader("X-Vega-Session") String secret) {
    return service.start(secret);
  }

  /** Valida contexto mínimo e publica a pendência para o worker. */
  @PostMapping("/generate")
  public JsonNode generate(
      @RequestHeader("X-Vega-Session") String secret, @Valid @RequestBody Input input) {
    return service.generate(secret, input);
  }

  /** Registra a ação funcional sem aceitar métricas ou eventos retroativos arbitrários. */
  @PostMapping("/events")
  public JsonNode event(
      @RequestHeader("X-Vega-Session") String secret, @Valid @RequestBody Event input) {
    return service.event(secret, input);
  }

  /** Encerra a leitura preservando o cartão e seus limites na tela. */
  @PostMapping("/finish")
  public JsonNode finish(@RequestHeader("X-Vega-Session") String secret) {
    return service.finish(secret);
  }

  /** Abre sessão segregada ou convite humano explicitamente solicitado por operador autorizado. */
  @PostMapping("/internal/sessions")
  public JsonNode create(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @Valid @RequestBody InternalSession input) {
    authorize(token);
    return service.create(input);
  }

  /** Revoga o acesso selecionado sem apagar sua evidência. */
  @DeleteMapping("/internal/sessions/{id}")
  public void revoke(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable String id) {
    authorize(token);
    service.revoke(id);
  }

  /** Expõe o ponto inicial canônico da fila ao executor. */
  @GetMapping("/internal/adjustment/stage-executions/pending")
  public JsonNode pending(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token) {
    authorize(token);
    return service.pending();
  }

  /** Reserva uma pendência antes de qualquer geração externa. */
  @PostMapping("/internal/adjustment/stage-executions/{id}/claim")
  public JsonNode claim(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable Long id) {
    authorize(token);
    return service.claim(id);
  }

  /** Preserva a requisição antes de sua execução pelo worker. */
  @PostMapping("/internal/adjustment/stage-executions/{id}/request")
  public void request(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable Long id,
      @Valid @RequestBody RequestAudit input) {
    authorize(token);
    service.request(id, input);
  }

  /** Recebe o resultado sem executar integrações ou agendar trabalho no backend. */
  @PostMapping("/internal/adjustment/stage-executions/{id}/result")
  public JsonNode result(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable Long id,
      @Valid @RequestBody Result input) {
    authorize(token);
    return service.complete(id, input);
  }

  /** Publica a evidência persistida por ciclo para o harness e os relatórios administrativos. */
  @GetMapping("/internal/cycles/{cycleId}/report")
  public JsonNode report(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable Long cycleId) {
    authorize(token);
    return service.report(cycleId);
  }

  /** Recusa credencial ausente ou divergente sem revelar o segredo configurado. */
  private void authorize(String token) {
    if (internalToken == null
        || internalToken.isBlank()
        || token == null
        || !MessageDigest.isEqual(
            internalToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial interna necessária.");
  }
}
