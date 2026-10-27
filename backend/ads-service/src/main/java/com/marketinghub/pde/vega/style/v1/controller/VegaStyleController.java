package com.marketinghub.pde.vega.style.v1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.pde.vega.style.v1.service.VegaStyleService;
import com.marketinghub.pde.vega.style.v1.service.contract.VegaStyleContract.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Expõe a jornada contextual privada e separa ações da cliente das fixtures autenticadas do runner.
 */
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Vega contextual privada v1",
    description = "Jornada e provas sintéticas da v16, sem cobrança ou publicação comercial")
@RestController
@RequestMapping("/api/pde/vega/style/v1")
public class VegaStyleController {
  private final VegaStyleService service;
  private final String internalToken;

  /** Recebe a jornada e a credencial protegida usada somente em operações técnicas. */
  public VegaStyleController(
      VegaStyleService service,
      @Value("${integrations.pde-platform.internal-token:${PDE_INTERNAL_API_TOKEN:}}")
          String internalToken) {
    this.service = service;
    this.internalToken = internalToken;
  }

  /** Informa capacidades sem promover a implementação a homologada. */
  @GetMapping("/contract")
  public JsonNode contract() {
    return service.contract();
  }

  /** Cria sessão sintética apenas para o executor autenticado. */
  @PostMapping("/internal/sessions")
  public JsonNode create(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @Valid @RequestBody Session request) {
    authorize(token);
    return service.create(request);
  }

  /** Recupera a jornada da sessão autenticada. */
  @GetMapping("/access/workspace")
  public JsonNode workspace(HttpServletRequest request) {
    return service.workspace(secret(request));
  }

  /** Registra o começo intencional da experiência. */
  @PostMapping("/start")
  public JsonNode start(HttpServletRequest request) {
    return service.start(secret(request));
  }

  /** Executa uma ação contextual idempotente dentro da missão e da versão autenticadas. */
  @PostMapping("/access/missions/{missionId}/ai-guidance")
  public JsonNode generate(
      HttpServletRequest http,
      @PathVariable String missionId,
      @Valid @RequestBody Generate request) {
    return service.generate(secret(http), missionId, request);
  }

  /** Consulta o mesmo resultado sem novo processamento. */
  @GetMapping("/access/ai-guidance/{id}")
  public JsonNode result(HttpServletRequest request, @PathVariable String id) {
    return service.result(secret(request), id);
  }

  /** Salva a reflexão correspondente às escolhas que geraram a orientação. */
  @PostMapping("/access/missions/{missionId}/interactions")
  public ResponseEntity<Void> reflect(
      HttpServletRequest http,
      @PathVariable String missionId,
      @Valid @RequestBody Reflection request) {
    service.reflect(secret(http), missionId, request);
    return ResponseEntity.noContent().build();
  }

  /** Conclui a missão com as pré-condições conferidas pelo backend. */
  @PostMapping("/access/missions/{missionId}/complete")
  public ResponseEntity<Void> complete(HttpServletRequest request, @PathVariable String missionId) {
    service.complete(secret(request), missionId);
    return ResponseEntity.noContent().build();
  }

  /** Recebe ações funcionais sem permitir ao navegador declarar preferência sintética. */
  @PostMapping("/events")
  public JsonNode event(HttpServletRequest http, @Valid @RequestBody Event request) {
    return service.event(secret(http), request, false);
  }

  /** Testa o transporte dos sinais sem apresentá-los como opinião ou comportamento humano. */
  @PostMapping("/internal/events")
  public JsonNode internalEvent(HttpServletRequest http, @Valid @RequestBody Event request) {
    authorize(http.getHeader("X-PDE-Internal-Token"));
    return service.event(secret(http), request, true);
  }

  /** Controla direitos e expirações simulados sem gateway ou compra. */
  @PostMapping("/internal/fixtures/{action}")
  public JsonNode fixture(HttpServletRequest request, @PathVariable String action) {
    authorize(request.getHeader("X-PDE-Internal-Token"));
    return service.fixture(secret(request), action);
  }

  /** Entrega a prova persistida somente ao executor e à própria sessão. */
  @GetMapping("/internal/report")
  public JsonNode report(HttpServletRequest request) {
    authorize(request.getHeader("X-PDE-Internal-Token"));
    return service.report(secret(request));
  }

  /** Exporta somente registros da sessão com direito integral vigente. */
  @GetMapping(value = "/materials/{name}", produces = "text/plain;charset=UTF-8")
  public ResponseEntity<String> material(HttpServletRequest request, @PathVariable String name) {
    return ResponseEntity.ok()
        .header("Cache-Control", "private, no-store")
        .header(
            "Content-Disposition",
            "attachment; filename=\""
                + (name.matches("[a-z-]+\\.txt") ? name : "material.txt")
                + "\"")
        .body(service.material(secret(request), name));
  }

  /** Lê o cookie HttpOnly ou o header de transporte e rejeita mutações entre origens. */
  private String secret(HttpServletRequest request) {
    String origin = request.getHeader("Origin");
    if (!"GET".equals(request.getMethod())
        && origin != null
        && !origin.equals(request.getScheme() + "://" + request.getHeader("Host"))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origem não autorizada.");
    }
    String header = request.getHeader("X-Vega-Style-Session");
    if (header != null) return header;
    if (request.getCookies() != null)
      for (var cookie : request.getCookies())
        if ("vega-style-session".equals(cookie.getName())) return cookie.getValue();
    return null;
  }

  /** Recusa credencial interna ausente ou divergente sem incluí-la no diagnóstico. */
  private void authorize(String token) {
    if (internalToken == null
        || internalToken.isBlank()
        || token == null
        || !MessageDigest.isEqual(
            token.getBytes(StandardCharsets.UTF_8), internalToken.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial interna necessária.");
  }
}
