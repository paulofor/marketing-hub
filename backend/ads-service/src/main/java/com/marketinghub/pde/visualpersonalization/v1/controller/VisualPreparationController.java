package com.marketinghub.pde.visualpersonalization.v1.controller;

import static com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationService.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationExecution;
import com.marketinghub.pde.visualpersonalization.v1.service.VisualPreparationService;
import com.marketinghub.pde.visualpersonalization.v1.service.contract.VisualPreparationContract.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor preparação visual privada e proteger sua fila e callbacks internos. */
@RestController
@RequestMapping("/api/pde/visual-personalization/v1")
public class VisualPreparationController {
  private final VisualPreparationService service;
  private final VisualPreparationExecution executions;
  private final String internalToken;

  /** Recebe os serviços e a mesma proteção interna usada pelos contratos PDE existentes. */
  public VisualPreparationController(
      VisualPreparationService service,
      VisualPreparationExecution executions,
      @Value("${integrations.pde-platform.internal-token:${PDE_INTERNAL_API_TOKEN:}}")
          String internalToken) {
    this.service = service;
    this.executions = executions;
    this.internalToken = internalToken;
  }

  /** Consulta origem e orçamento autorizado sem abrir sessão ou consumir IA. */
  @GetMapping("/products/{productId}/context")
  @Operation(summary = "Consultar preparação visual privada autorizada")
  public JsonNode context(
      @PathVariable Long productId,
      @RequestParam Long commercialPlanId,
      @RequestParam Long experimentId) {
    return service.context(productId, commercialPlanId, experimentId);
  }

  /** Impede armazenamento compartilhado e referências externas de dados da preparação privada. */
  @ModelAttribute
  public void privateHeaders(jakarta.servlet.http.HttpServletResponse response) {
    response.setHeader("Cache-Control", "private, no-store");
    response.setHeader("Referrer-Policy", "no-referrer");
    response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
  }

  /** Cria a preparação solicitada pela tela administrativa no mesmo produto e experimento. */
  @PostMapping("/products/{productId}/preparations")
  @Operation(summary = "Preparar entrega visual sintética com autorização e orçamento existentes")
  public JsonNode create(@PathVariable Long productId, @Valid @RequestBody Create input) {
    return service.create(productId, input);
  }

  /** Recupera estado e resultado somente com a credencial da própria preparação. */
  @GetMapping("/preparations/{jobId}")
  @Operation(summary = "Recuperar entrega visual privada sem nova inferência")
  public JsonNode session(
      @PathVariable String jobId, @RequestHeader("X-PDE-Visual-Session") String token) {
    return service.session(jobId, token);
  }

  /**
   * Concilia somente a resposta da própria sessão, sem abrir request ou cobrar outra inferência.
   */
  @PostMapping("/preparations/{jobId}/reconcile")
  @Operation(summary = "Reconciliar resposta visual preservada sem chamar IA")
  public JsonNode reconcile(
      @PathVariable String jobId, @RequestHeader("X-PDE-Visual-Session") String token) {
    service.session(jobId, token);
    return executions.apply(jobId);
  }

  /** Entrega o ponto inicial canônico para descoberta de trabalho pelo executor. */
  @GetMapping("/internal/stage-executions/pending")
  public List<JsonNode> pending(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token) {
    authorize(token);
    return executions.pending();
  }

  /** Reserva uma tentativa antes de o executor montar e enviar a chamada. */
  @PostMapping("/internal/stage-executions/{jobId}/claim")
  public JsonNode claim(
      @PathVariable String jobId,
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token) {
    authorize(token);
    return executions.claim(jobId);
  }

  /** Audita a requisição limitada em Flex antes de seu envio ao provedor. */
  @PostMapping("/internal/stage-executions/{jobId}/request")
  public void request(
      @PathVariable String jobId,
      @Valid @RequestBody RequestAudit input,
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token) {
    authorize(token);
    executions.request(jobId, input);
  }

  /** Persiste e aplica a resposta preservada sem decidir qualquer autorização comercial. */
  @PostMapping("/internal/stage-executions/{jobId}/result")
  public JsonNode result(
      @PathVariable String jobId,
      @Valid @RequestBody Result input,
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token) {
    authorize(token);
    return executions.receive(jobId, input);
  }

  /** Reaplica uma resposta já recebida sem regeneração ou cobrança duplicada. */
  @PostMapping("/internal/stage-executions/{jobId}/replay")
  public JsonNode replay(
      @PathVariable String jobId,
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token) {
    authorize(token);
    return executions.apply(jobId);
  }

  /** Recusa credenciais internas ausentes ou divergentes sem expor o segredo configurado. */
  private void authorize(String token) {
    if (internalToken == null
        || internalToken.isBlank()
        || token == null
        || !MessageDigest.isEqual(
            internalToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
      throw fail(401, "Credencial interna necessária.");
  }
}
