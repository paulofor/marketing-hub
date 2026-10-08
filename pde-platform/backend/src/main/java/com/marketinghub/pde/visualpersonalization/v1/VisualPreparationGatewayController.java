package com.marketinghub.pde.visualpersonalization.v1;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: expor somente o transporte autorizado da etapa visual no backend PDE. */
@RestController
@RequestMapping("/api/pde/visual-personalization/v1")
public class VisualPreparationGatewayController {
  private final VisualPreparationGatewayService gateway;
  private final InternalApiAuthorizer authorizer;

  /** Recebe o transporte e a proteção interna canônica do PDE. */
  public VisualPreparationGatewayController(VisualPreparationGatewayService gateway, InternalApiAuthorizer authorizer) {
    this.gateway = gateway;
    this.authorizer = authorizer;
  }

  /** Recupera a entrega selecionada preservando somente a credencial do próprio chamador. */
  @GetMapping("/preparations/{jobId}")
  public ResponseEntity<String> session(@PathVariable("jobId") String jobId,
      @RequestHeader("X-PDE-Visual-Session") String token) {
    return gateway.forward("GET", "/preparations/" + jobId, null, "X-PDE-Visual-Session", token);
  }

  /** Transporta conciliação da própria resposta sem elevar credenciais ou autorizar inferência. */
  @PostMapping("/preparations/{jobId}/reconcile")
  public ResponseEntity<String> reconcile(@PathVariable("jobId") String jobId,
      @RequestHeader("X-PDE-Visual-Session") String token) {
    return gateway.forward("POST", "/preparations/" + jobId + "/reconcile", null, "X-PDE-Visual-Session", token);
  }

  /** Entrega a descoberta canônica de trabalho exclusivamente ao executor interno autorizado. */
  @GetMapping("/internal/stage-executions/pending")
  public ResponseEntity<String> pending(@RequestHeader(value = "X-PDE-Internal-Token", required = false) String token) {
    authorizer.requireAuthorized(token);
    return gateway.forward("GET", "/internal/stage-executions/pending", null, "X-PDE-Internal-Token", token);
  }

  /** Encaminha reserva e callbacks sem comandar ou inferir a próxima etapa de negócio. */
  @PostMapping({"/internal/stage-executions/{jobId}/claim", "/internal/stage-executions/{jobId}/request",
      "/internal/stage-executions/{jobId}/result", "/internal/stage-executions/{jobId}/replay"})
  public ResponseEntity<String> callback(HttpServletRequest request, @PathVariable("jobId") String jobId,
      @RequestHeader(value = "X-PDE-Internal-Token", required = false) String token,
      @RequestBody(required = false) String body) {
    authorizer.requireAuthorized(token);
    String suffix = request.getRequestURI().substring(request.getContextPath().length()
        + "/api/pde/visual-personalization/v1".length());
    return gateway.forward("POST", suffix, body, "X-PDE-Internal-Token", token);
  }
}
