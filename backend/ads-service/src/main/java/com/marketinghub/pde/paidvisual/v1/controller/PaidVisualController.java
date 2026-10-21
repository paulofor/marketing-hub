package com.marketinghub.pde.paidvisual.v1.controller;

import static com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationService.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.paidvisual.v1.service.PaidVisualService;
import com.marketinghub.pde.paidvisual.v1.service.contract.PaidVisualContract.*;
import com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationExecution;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: expor entrega paga e proteger callbacks financeiros e sessões da compradora.
 */
@RestController
@RequestMapping("/api/pde/paid-visual/v1")
public class PaidVisualController {
  private final PaidVisualService service;
  private final VisualPreparationExecution execution;
  private final String internalToken;

  /** Reutiliza a credencial interna PDE sem aceitar dados financeiros públicos. */
  public PaidVisualController(
      PaidVisualService service,
      VisualPreparationExecution execution,
      @Value("${integrations.lead-portal-payments.auth-token:}") String internalToken) {
    this.service = service;
    this.execution = execution;
    this.internalToken = internalToken;
  }

  /** Evita cache ou referência externa de credenciais e resultados privados. */
  @ModelAttribute
  public void headers(jakarta.servlet.http.HttpServletResponse response) {
    response.setHeader("Cache-Control", "private, no-store");
    response.setHeader("Referrer-Policy", "no-referrer");
    response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
  }

  /** Consulta a oferta sem permitir compra antes da liberação comercial. */
  @GetMapping("/experiments/{id}/offer")
  @Operation(summary = "Consultar oferta da entrega visual paga")
  public JsonNode offer(@PathVariable Long id) {
    return service.offer(id);
  }

  /** Recupera o acesso somente por e-mail de compra e com resposta opaca. */
  @PostMapping("/experiments/{id}/login")
  @Operation(summary = "Enviar acesso para o e-mail da compra")
  public Map<String, String> login(@PathVariable Long id, @Valid @RequestBody Login input) {
    return service.login(id, input);
  }

  /** Consulta o resultado da própria cliente sem nova inferência. */
  @GetMapping("/session")
  @Operation(summary = "Recuperar entrega paga")
  public JsonNode session(@RequestHeader("X-PDE-Access-Token") String token) {
    return service.session(token);
  }

  /** Solicita geração ou a única regeneração permitida. */
  @PostMapping("/session/submit")
  @Operation(summary = "Personalizar entrega após pagamento confirmado")
  public JsonNode submit(
      @RequestHeader("X-PDE-Access-Token") String token, @Valid @RequestBody Submit input) {
    return service.submit(token, input);
  }

  /** Reaplica resposta preservada sem gerar outra cobrança. */
  @PostMapping("/session/{jobId}/reconcile")
  @Operation(summary = "Recuperar callback da própria entrega")
  public JsonNode reconcile(
      @RequestHeader("X-PDE-Access-Token") String token, @PathVariable String jobId) {
    service.authorizeReplay(token, jobId);
    execution.apply(jobId);
    return service.session(token);
  }

  /** Recebe somente o pagamento consultado no provedor pelo executor autenticado. */
  @PostMapping("/internal/payments")
  @Operation(summary = "Reconciliar estado financeiro autoritativo")
  public Map<String, String> payment(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @Valid @RequestBody Payment input) {
    if (internalToken == null
        || internalToken.isBlank()
        || token == null
        || !MessageDigest.isEqual(
            internalToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
      throw fail(401, "Credencial interna necessária.");
    return service.payment(input);
  }
}
