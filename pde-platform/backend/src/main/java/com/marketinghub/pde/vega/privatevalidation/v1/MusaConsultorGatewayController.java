package com.marketinghub.pde.vega.privatevalidation.v1;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Transporta o MUSA contínuo pelo backend principal, sem persistir nem conceder direitos. */
@RestController
@RequestMapping("/api/pde/vega/consultor/v1")
public class MusaConsultorGatewayController {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(MusaConsultorGatewayController.class);
  private final VegaPrivateGatewayService gateway;
  private final InternalApiAuthorizer authorizer;

  /** Reutiliza o transporte e a autorização canônicos do PDE. */
  public MusaConsultorGatewayController(
      VegaPrivateGatewayService gateway, InternalApiAuthorizer authorizer) {
    this.gateway = gateway;
    this.authorizer = authorizer;
  }

  /** Encaminha somente consultas e o relatório protegido. */
  @GetMapping({"/contract", "/workspace", "/operations/{id}", "/internal/report"})
  public ResponseEntity<String> query(HttpServletRequest request) {
    return forward(request, null);
  }

  /** Encaminha mutações e controles internos com origem e credenciais conferidas. */
  @PostMapping({
    "/login",
    "/logout",
    "/commands",
    "/internal/events",
    "/internal/cycles/{id}/access",
    "/internal/cycles/{id}/close",
    "/internal/access/{id}/renew"
  })
  public ResponseEntity<String> command(
      HttpServletRequest request, @RequestBody(required = false) String body) {
    return forward(request, body);
  }

  /** Isola operações por sessão e recusa origem divergente antes do transporte. */
  private ResponseEntity<String> forward(HttpServletRequest request, String body) {
    String path =
        request
            .getRequestURI()
            .substring(request.getContextPath().length() + "/api/pde/vega/consultor/v1".length());
    String internal = request.getHeader("X-PDE-Internal-Token");
    if (path.startsWith("/internal/")) {
      try {
        authorizer.requireAuthorized(internal);
      } catch (SecurityException ex) {
        log.warn(
            "MUSA: operação interna recusada operation={} path={}", request.getMethod(), path, ex);
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.UNAUTHORIZED, "Operador não autenticado.", ex);
      }
    }
    String origin = request.getHeader("Origin");
    if (!"GET".equals(request.getMethod()) && origin != null) {
      URI source = URI.create(origin);
      if (!java.util.Set.of("http", "https").contains(source.getScheme())
          || !source.getAuthority().equals(request.getHeader("Host")))
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.FORBIDDEN, "Origem não autorizada");
    }
    String session = request.getHeader("X-Musa-Session");
    if (session == null && request.getCookies() != null)
      for (var cookie : request.getCookies())
        if ("musa-consultor-session".equals(cookie.getName())) session = cookie.getValue();
    return gateway.forwardConsultor(request.getMethod(), path, body, session, internal);
  }
}
