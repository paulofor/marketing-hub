package com.marketinghub.experiment.web;

import com.marketinghub.experiment.service.ExperimentCommercialCheckoutService;
import com.marketinghub.leadportal.integration.LeadPortalPaymentsClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe o comando administrativo de criação do checkout comercial do experimento. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/commercial-checkout")
public class ExperimentCommercialCheckoutController {

  private final ExperimentCommercialCheckoutService service;

  /** Inicializa o controller com a operação canônica de checkout. */
  public ExperimentCommercialCheckoutController(ExperimentCommercialCheckoutService service) {
    this.service = service;
  }

  /** Cria e vincula o checkout usando produto, preço e entrega persistidos. */
  @PostMapping
  public LeadPortalPaymentsClient.CommercialProductCheckoutResponse create(
      @PathVariable Long experimentId) {
    return service.create(experimentId);
  }
}
