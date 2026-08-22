package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.leadportal.integration.LeadPortalPaymentsClient;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Cria e vincula o checkout comercial ao experimento sem confundi-lo com a landing. */
@Service
public class ExperimentCommercialCheckoutService {

  private static final String VALIDATION_OK = "OK";
  private final ExperimentRepository experimentRepository;
  private final ProductRepository productRepository;
  private final PdeProductionSlotRepository pdeProductionSlotRepository;
  private final LeadPortalPaymentsClient paymentsClient;

  /** Inicializa a operação com as fontes canônicas de experimento, produto, PDE e pagamento. */
  public ExperimentCommercialCheckoutService(
      ExperimentRepository experimentRepository,
      ProductRepository productRepository,
      PdeProductionSlotRepository pdeProductionSlotRepository,
      LeadPortalPaymentsClient paymentsClient) {
    this.experimentRepository = experimentRepository;
    this.productRepository = productRepository;
    this.pdeProductionSlotRepository = pdeProductionSlotRepository;
    this.paymentsClient = paymentsClient;
  }

  /** Exige área de entrega validada e cria o checkout com preço persistido no experimento. */
  @Transactional
  public LeadPortalPaymentsClient.CommercialProductCheckoutResponse create(Long experimentId) {
    Experiment experiment =
        experimentRepository
            .findById(experimentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Experimento não encontrado"));
    if (experiment.getStatus() != ExperimentStatus.PLANNED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Checkout só pode ser criado enquanto o experimento estiver PLANNED");
    }
    if (experiment.getProduct() == null
        || !StringUtils.hasText(experiment.getProduct().getSlug())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Experimento sem produto comercial");
    }
    if (experiment.getUnitPrice() == null || experiment.getUnitPrice().signum() <= 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Experimento sem preço comercial válido");
    }
    if (StringUtils.hasText(experiment.getCommercialCheckoutUrl())) {
      return new LeadPortalPaymentsClient.CommercialProductCheckoutResponse(
          experiment.getProduct().getSlug(),
          experiment.getProduct().getId(),
          experiment.getId(),
          null,
          experiment.getCommercialCheckoutUrl(),
          experiment.getUnitPrice(),
          "BRL",
          experiment.getProduct().getPublicUrl());
    }
    var product = experiment.getProduct();
    var activeSlot =
        pdeProductionSlotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug()).stream()
            .filter(slot -> slot.getStatus() == PdeProductionSlotStatus.ACTIVE)
            .filter(slot -> VALIDATION_OK.equals(slot.getValidationStatus()))
            .filter(slot -> StringUtils.hasText(slot.getPublicUrl()))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Publique, valide e ative a área de entrega PDE antes de criar o checkout"));
    var response =
        paymentsClient.createCommercialProductCheckout(
            new LeadPortalPaymentsClient.CommercialProductCheckoutRequest(
                product.getSlug(),
                product.getName(),
                product.getId(),
                experiment.getId(),
                experiment.getUnitPrice(),
                activeSlot.getPublicUrl()));
    experiment.setCommercialCheckoutUrl(response.checkoutUrl());
    product.setPublicUrl(activeSlot.getPublicUrl());
    productRepository.save(product);
    experimentRepository.save(experiment);
    return response;
  }
}
