package com.marketinghub.pde.vega.privatevalidation.v1;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Encaminha a jornada contextual ao backend principal mantendo o cookie fora do JavaScript. */
@RestController
@RequestMapping("/api/pde/vega/style/v1")
public class VegaStyleGatewayController {
  private final VegaPrivateGatewayService gateway;
  private final InternalApiAuthorizer authorizer;

  /** Reutiliza transporte e autorização existentes, sem persistência no módulo de fronteira. */
  public VegaStyleGatewayController(
      VegaPrivateGatewayService gateway, InternalApiAuthorizer authorizer) {
    this.gateway = gateway;
    this.authorizer = authorizer;
  }

  /** Transporta somente consultas tipadas da própria sessão e o relatório interno protegido. */
  @GetMapping({
    "/contract",
    "/access/workspace",
    "/access/ai-guidance/{id}",
    "/materials/{name}",
    "/internal/report"
  })
  public ResponseEntity<String> query(HttpServletRequest request) {
    return forward(request, null);
  }

  /** Transporta comandos funcionais e separa os controles técnicos autenticados. */
  @PostMapping({
    "/start",
    "/events",
    "/access/missions/{missionId}/ai-guidance",
    "/access/missions/{missionId}/interactions",
    "/access/missions/{missionId}/complete",
    "/internal/sessions",
    "/internal/events",
    "/internal/fixtures/{action}"
  })
  public ResponseEntity<String> command(
      HttpServletRequest request, @RequestBody(required = false) String body) {
    return forward(request, body);
  }

  /**
   * Confere origem e escopo antes de transportar as credenciais recebidas, sem injetar segredos.
   */
  private ResponseEntity<String> forward(HttpServletRequest request, String body) {
    String path =
        request
            .getRequestURI()
            .substring(request.getContextPath().length() + "/api/pde/vega/style/v1".length());
    String internal = request.getHeader("X-PDE-Internal-Token");
    if (path.startsWith("/internal/")) authorizer.requireAuthorized(internal);
    String origin = request.getHeader("Origin");
    if (!"GET".equals(request.getMethod()) && origin != null) {
      URI source = URI.create(origin);
      if (!java.util.Set.of("http", "https").contains(source.getScheme())
          || !source.getAuthority().equals(request.getHeader("Host")))
        throw new SecurityException("Origem não autorizada");
    }
    String session = request.getHeader("X-Vega-Style-Session");
    if (session == null && request.getCookies() != null)
      for (var cookie : request.getCookies())
        if ("vega-style-session".equals(cookie.getName())) session = cookie.getValue();
    return gateway.forwardStyle(request.getMethod(), path, body, session, internal);
  }
}
