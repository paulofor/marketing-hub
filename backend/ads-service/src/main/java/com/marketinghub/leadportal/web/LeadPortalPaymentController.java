package com.marketinghub.leadportal.web;

import com.marketinghub.leadportal.dto.LeadPortalPaymentDto;
import com.marketinghub.leadportal.service.LeadPortalPaymentQueryService;
import com.marketinghub.leadportal.integration.LeadPortalPaymentsClient;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Expõe pagamentos e controles administrativos de checkout do Lead Portal. */
@RestController
@RequestMapping("/api/lead-portal/payments")
public class LeadPortalPaymentController {

  private final LeadPortalPaymentQueryService paymentQueryService;
  private final LeadPortalPaymentsClient paymentsClient;

  /** Configura consulta de pagamentos e comandos do serviço oficial de checkout. */
  public LeadPortalPaymentController(
      LeadPortalPaymentQueryService paymentQueryService, LeadPortalPaymentsClient paymentsClient) {
    this.paymentQueryService = paymentQueryService;
    this.paymentsClient = paymentsClient;
  }

  /** Lista pagamentos recentes para acompanhamento administrativo. */
  @GetMapping
  public List<LeadPortalPaymentDto> list(
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return paymentQueryService.listRecentPayments(limit);
  }

  /** Ativa um preço temporário de teste preservando o checkout comercial. */
  @PostMapping("/temporary-checkout")
  public LeadPortalPaymentsClient.TemporaryCheckoutResponse activateTemporaryCheckout(
      @RequestBody TemporaryCheckoutAdminRequest request) {
    return paymentsClient.activateTemporaryCheckout(new LeadPortalPaymentsClient.TemporaryCheckoutRequest(
        request.productKey(), request.productName(), request.testAmount(),
        request.commercialCheckoutUrl(), request.durationMinutes()));
  }

  /** Consulta o estado vigente do checkout temporário. */
  @GetMapping("/temporary-checkout/{productKey}")
  public LeadPortalPaymentsClient.TemporaryCheckoutResponse temporaryCheckout(
      @PathVariable String productKey) {
    return paymentsClient.getTemporaryCheckout(productKey);
  }

  /** Restaura o checkout comercial antes do vencimento. */
  @PostMapping("/temporary-checkout/{productKey}/restore")
  public LeadPortalPaymentsClient.TemporaryCheckoutResponse restoreTemporaryCheckout(
      @PathVariable String productKey) {
    return paymentsClient.restoreTemporaryCheckout(productKey);
  }

  /** Contrato administrativo para ativação temporária de preço. */
  public record TemporaryCheckoutAdminRequest(
      String productKey,
      String productName,
      BigDecimal testAmount,
      String commercialCheckoutUrl,
      Integer durationMinutes) {}
}
