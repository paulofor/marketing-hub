package com.marketinghub.pde.paidvisual.v1;

import com.marketinghub.pde.visualpersonalization.v1.VisualPreparationGatewayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: transportar a entrega paga ao backend principal sem decidir estado ou autorização. */
@RestController
@RequestMapping("/api/pde/paid-visual/v1")
public class PaidVisualGatewayController {
  private final VisualPreparationGatewayService gateway;
  /** Recebe o transporte principal já configurado e validado. */
  public PaidVisualGatewayController(VisualPreparationGatewayService gateway) { this.gateway=gateway; }
  /** Encaminha somente consulta pública de oferta ou sessão autenticada. */
  @GetMapping({"/experiments/{id}/offer","/session"})
  public ResponseEntity<String> read(HttpServletRequest request,
      @RequestHeader(value="X-PDE-Access-Token",required=false) String token) {
    return gateway.forwardPaid("GET",suffix(request),null,token);
  }
  /** Encaminha entrada e recuperação mantendo somente o bearer da própria cliente. */
  @PostMapping({"/experiments/{id}/login","/session/submit","/session/{jobId}/reconcile"})
  public ResponseEntity<String> write(HttpServletRequest request,@RequestBody(required=false) String body,
      @RequestHeader(value="X-PDE-Access-Token",required=false) String token) {
    return gateway.forwardPaid("POST",suffix(request),body,token);
  }
  /** Extrai o caminho relativo do contrato pago sem aceitar destino fornecido pelo chamador. */
  private String suffix(HttpServletRequest request) { return request.getRequestURI().substring(request.getContextPath().length()+"/api/pde/paid-visual/v1".length()); }
}
