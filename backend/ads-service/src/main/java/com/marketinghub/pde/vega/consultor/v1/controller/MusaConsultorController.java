package com.marketinghub.pde.vega.consultor.v1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.vega.consultor.v1.service.MusaConsultorService;
import com.marketinghub.pde.vega.consultor.v1.service.command.Command;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Expõe o aplicativo privado com sessão HttpOnly e separa operações técnicas do operador. */
@RestController
@RequestMapping("/api/pde/vega/consultor/v1")
public class MusaConsultorController {
  private final MusaConsultorService service;
  private final String internal;

  /** Recebe a jornada e o segredo protegido sem disponibilizá-lo ao navegador. */
  public MusaConsultorController(
      MusaConsultorService service,
      @Value("${integrations.pde-platform.internal-token:${PDE_INTERNAL_API_TOKEN:}}")
          String internal) {
    this.service = service;
    this.internal = internal;
  }

  /** Impede cache e indexação de respostas privadas, inclusive erros da operação. */
  @ModelAttribute
  public void privacy(jakarta.servlet.http.HttpServletResponse response) {
    response.setHeader("Cache-Control", "private, no-store");
    response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
    response.setHeader("Referrer-Policy", "no-referrer");
  }

  /** Publica a capacidade implementada sem custo, sessão ou aceite. */
  @GetMapping("/contract")
  @Operation(summary = "Consultar capacidade privada do MUSA contínuo")
  public JsonNode contract() {
    return service.contract();
  }

  /** Emite acesso apenas ao operador autenticado e preserva o limite cumulativo de testes. */
  @PostMapping("/internal/cycles/{cycleId}/access")
  @Operation(summary = "Emitir acesso privado sintético de 90 dias")
  public JsonNode issue(HttpServletRequest request, @PathVariable Long cycleId) {
    operator(request);
    return service.issue(cycleId);
  }

  /** Renova a mesma identidade enquanto a homologação permanece aberta. */
  @PostMapping("/internal/access/{id}/renew")
  @Operation(summary = "Renovar acesso privado sem alterar dados ou limites")
  public JsonNode renew(HttpServletRequest request, @PathVariable String id) {
    operator(request);
    return service.renew(id);
  }

  /** Revoga os acessos no encerramento sem apagar provas ou custos. */
  @PostMapping("/internal/cycles/{cycleId}/close")
  @Operation(summary = "Encerrar homologação privada e revogar acessos")
  public JsonNode close(HttpServletRequest request, @PathVariable Long cycleId) {
    operator(request);
    return service.close(cycleId);
  }

  /** Autentica a chave privada e remove a credencial da resposta consumida pelo JavaScript. */
  @PostMapping("/login")
  @Operation(summary = "Abrir sessão privada por chave de acesso")
  public ResponseEntity<JsonNode> login(HttpServletRequest request, @RequestBody JsonNode input) {
    origin(request);
    var response = service.login(input.path("accessKey").asText());
    String token = response.path("sessionToken").asText();
    ((com.fasterxml.jackson.databind.node.ObjectNode) response).remove("sessionToken");
    return ResponseEntity.ok().header("Set-Cookie", cookie(request, token, 43200)).body(response);
  }

  /** Limpa o cookie na saída sem apagar o repertório protegido. */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    origin(request);
    return ResponseEntity.noContent().header("Set-Cookie", cookie(request, "", 0)).build();
  }

  /** Recupera somente o repertório da sessão autenticada. */
  @GetMapping("/workspace")
  @Operation(summary = "Recuperar rascunho, preferências e escolhas privadas")
  public JsonNode workspace(HttpServletRequest request) {
    return service.workspace(token(request));
  }

  /** Aplica ações explícitas com revisão concorrente e chave de recuperação. */
  @PostMapping("/commands")
  @Operation(summary = "Salvar rascunho, orientar, guardar escolha ou registrar ação")
  public JsonNode command(HttpServletRequest request, @Valid @RequestBody Command command) {
    origin(request);
    return service.command(token(request), command.action(), command.payload(), false);
  }

  /** Consulta orientação já preservada sem nova geração. */
  @GetMapping("/operations/{id}")
  public JsonNode operation(HttpServletRequest request, @PathVariable String id) {
    return service.operation(token(request), id);
  }

  /** Entrega relatório técnico segregado somente ao operador e à própria sessão. */
  @GetMapping("/internal/report")
  public JsonNode report(HttpServletRequest request) {
    operator(request);
    return service.report(token(request));
  }

  /** Exercita transporte sintético dos sinais sem declarar opinião ou compra humana. */
  @PostMapping("/internal/events")
  public JsonNode event(HttpServletRequest request, @RequestBody JsonNode input) {
    operator(request);
    return service.command(token(request), "event", input, true);
  }

  /** Recusa mutações de outra origem antes de acessar a sessão. */
  private void origin(HttpServletRequest request) {
    String origin = request.getHeader("Origin");
    if (origin != null
        && !origin.equals(
            (request.isSecure() ? "https" : "http") + "://" + request.getHeader("Host")))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origem não autorizada.");
  }

  /** Lê somente cookie protegido ou header técnico sem aceitar proprietário do navegador. */
  private String token(HttpServletRequest request) {
    String header = request.getHeader("X-Musa-Session");
    if (header != null) return header;
    if (request.getCookies() != null)
      for (var cookie : request.getCookies())
        if ("musa-consultor-session".equals(cookie.getName())) return cookie.getValue();
    return null;
  }

  /** Restringe o cookie ao módulo, com proteção HTTPS e duração de sessão distinta do acesso. */
  private String cookie(HttpServletRequest request, String token, long age) {
    boolean local = isLocal(request.getServerName());
    return ResponseCookie.from("musa-consultor-session", token)
        .httpOnly(true)
        .secure(!local)
        .sameSite("Strict")
        .path("/api/pde/vega/consultor/v1")
        .maxAge(age)
        .build()
        .toString();
  }

  /** Reconhece exclusivamente hosts locais para homologação HTTP isolada. */
  private boolean isLocal(String host) {
    return java.util.Set.of("localhost", "127.0.0.1").contains(host);
  }

  /** Confere o token do operador sem revelá-lo em logs ou mensagens. */
  private void operator(HttpServletRequest request) {
    String token = request.getHeader("X-PDE-Internal-Token");
    if (internal == null
        || internal.isBlank()
        || token == null
        || !MessageDigest.isEqual(
            internal.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operador não autenticado.");
  }
}
