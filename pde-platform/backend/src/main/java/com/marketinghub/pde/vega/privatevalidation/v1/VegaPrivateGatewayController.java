package com.marketinghub.pde.vega.privatevalidation.v1;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor na fronteira PDE apenas os contratos oficiais privados de Vega. */
@RestController
@RequestMapping("/api/pde/vega/private/v1")
public class VegaPrivateGatewayController {
  private final VegaPrivateGatewayService gateway;
  private final InternalApiAuthorizer authorizer;

  /** Recebe o transporte e a proteção interna existentes do backend PDE. */
  public VegaPrivateGatewayController(
      VegaPrivateGatewayService gateway, InternalApiAuthorizer authorizer) {
    this.gateway = gateway;
    this.authorizer = authorizer;
  }

  /** Expõe somente capacidades sanitizadas da versão vigente. */
  @GetMapping("/contract")
  public ResponseEntity<String> contract() {
    return gateway.forward("GET", "/contract", null, "X-Vega-Session", null);
  }

  /** Recupera a própria sessão pela credencial opaca, sem criar outra leitura. */
  @GetMapping("/session")
  public ResponseEntity<String> session(@RequestHeader("X-Vega-Session") String token) {
    return gateway.forward("GET", "/session", null, "X-Vega-Session", token);
  }

  /**
   * Encaminha as ações públicas tipadas; validação e persistência continuam no backend principal.
   */
  @PostMapping({"/access", "/start", "/generate", "/events", "/finish"})
  public ResponseEntity<String> action(
      jakarta.servlet.http.HttpServletRequest request,
      @RequestHeader(value = "X-Vega-Session", required = false) String token,
      @RequestBody(required = false) String body) {
    String path =
        request
            .getRequestURI()
            .substring(request.getContextPath().length() + "/api/pde/vega/private/v1".length());
    return gateway.forward("POST", path, body, "X-Vega-Session", token);
  }

  /** Cria sessão segregada somente para o chamador interno autorizado, sem elevar privilégios. */
  @PostMapping("/internal/sessions")
  public ResponseEntity<String> create(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @RequestBody String body) {
    authorizer.requireAuthorized(token);
    return gateway.forward("POST", "/internal/sessions", body, "X-PDE-Internal-Token", token);
  }

  /** Revoga uma sessão por identidade tipada, preservando sua auditoria. */
  @DeleteMapping("/internal/sessions/{id}")
  public ResponseEntity<String> revoke(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable("id") UUID id) {
    authorizer.requireAuthorized(token);
    return gateway.forward(
        "DELETE", "/internal/sessions/" + id, null, "X-PDE-Internal-Token", token);
  }

  /** Consulta a fila canônica por modalidade sem descobrir trabalho por outro caminho. */
  @GetMapping("/internal/adjustment/stage-executions/pending")
  public ResponseEntity<String> pending(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @RequestParam(name = "mode", defaultValue = "PROVIDER") Mode mode) {
    authorizer.requireAuthorized(token);
    return gateway.forward(
        "GET",
        "/internal/adjustment/stage-executions/pending?mode=" + mode,
        null,
        "X-PDE-Internal-Token",
        token);
  }

  /** Transporta reserva e callbacks explicitamente permitidos, sem decidir a próxima etapa. */
  @PostMapping({
    "/internal/adjustment/stage-executions/{id}/claim",
    "/internal/adjustment/stage-executions/{id}/request",
    "/internal/adjustment/stage-executions/{id}/result"
  })
  public ResponseEntity<String> callback(
      jakarta.servlet.http.HttpServletRequest request,
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable("id") Long id,
      @RequestBody(required = false) String body) {
    authorizer.requireAuthorized(token);
    String path =
        request
            .getRequestURI()
            .substring(request.getContextPath().length() + "/api/pde/vega/private/v1".length());
    return gateway.forward("POST", path, body, "X-PDE-Internal-Token", token);
  }

  /** Consulta somente as provas persistidas do ciclo explícito. */
  @GetMapping("/internal/cycles/{cycleId}/report")
  public ResponseEntity<String> report(
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @PathVariable("cycleId") Long cycleId) {
    authorizer.requireAuthorized(token);
    return gateway.forward(
        "GET", "/internal/cycles/" + cycleId + "/report", null, "X-PDE-Internal-Token", token);
  }

  /** Restringe a consulta às duas modalidades do contrato canônico. */
  public enum Mode {
    PROVIDER,
    FIXTURE
  }
}
