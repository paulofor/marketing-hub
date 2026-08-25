package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.leadportal.integration.LeadPortalPaymentsClient;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    var product = experiment.getProduct();
    var activeSlot = resolveDeliverySlot(experiment, product.getSlug());
    var response =
        paymentsClient.createCommercialProductCheckout(
            new LeadPortalPaymentsClient.CommercialProductCheckoutRequest(
                product.getSlug(),
                product.getName(),
                product.getId(),
                experiment.getId(),
                experiment.getUnitPrice(),
                activeSlot.getPublicUrl()));
    validateCheckoutContract(experiment, activeSlot, response);
    experiment.setCommercialCheckoutUrl(response.checkoutUrl());
    product.setPublicUrl(activeSlot.getPublicUrl());
    productRepository.save(product);
    experimentRepository.save(experiment);
    return response;
  }

  /** Resolve a entrega pela URL do experimento e bloqueia seleção ambígua entre versões ativas. */
  private PdeProductionSlot resolveDeliverySlot(Experiment experiment, String productSlug) {
    List<PdeProductionSlot> activeSlots =
        pdeProductionSlotRepository.findByProductSlugOrderBySlotCodeAsc(productSlug).stream()
            .filter(slot -> slot.getStatus() == PdeProductionSlotStatus.ACTIVE)
            .filter(slot -> VALIDATION_OK.equals(slot.getValidationStatus()))
            .filter(slot -> StringUtils.hasText(slot.getPublicUrl()))
            .toList();
    Optional<String> destinationDomain = normalizeDomain(experiment.getFollowUpActionUrl());
    List<PdeProductionSlot> destinationMatches =
        destinationDomain
            .map(
                domain ->
                    activeSlots.stream()
                        .filter(
                            slot ->
                                normalizeDomain(slot.getPublicUrl())
                                    .filter(domain::equals)
                                    .isPresent())
                        .toList())
            .orElseGet(List::of);
    if (destinationMatches.size() == 1) {
      return destinationMatches.get(0);
    }
    List<PdeProductionSlot> experimentMatches =
        activeSlots.stream()
            .filter(slot -> experiment.getId().equals(slot.getSourceExperimentId()))
            .toList();
    if (experimentMatches.size() == 1) {
      return experimentMatches.get(0);
    }
    if (activeSlots.size() == 1) {
      return activeSlots.get(0);
    }
    throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Publique, valide e ative uma única área de entrega PDE correspondente ao destino do experimento antes de criar o checkout");
  }

  /** Extrai o domínio de uma URL para comparar a landing com seu slot PDE versionado. */
  private Optional<String> normalizeDomain(String url) {
    if (!StringUtils.hasText(url)) {
      return Optional.empty();
    }
    String normalized = url.trim().replaceFirst("^https?://", "").replaceFirst("^//", "");
    int separator = normalized.indexOf('/');
    if (separator >= 0) {
      normalized = normalized.substring(0, separator);
    }
    separator = normalized.indexOf('?');
    if (separator >= 0) {
      normalized = normalized.substring(0, separator);
    }
    separator = normalized.indexOf('#');
    if (separator >= 0) {
      normalized = normalized.substring(0, separator);
    }
    separator = normalized.indexOf(':');
    if (separator >= 0) {
      normalized = normalized.substring(0, separator);
    }
    if (!StringUtils.hasText(normalized) || !normalized.contains(".")) {
      return Optional.empty();
    }
    return Optional.of(normalized.toLowerCase(Locale.ROOT));
  }

  /** Confirma que o serviço de pagamentos devolveu o mesmo produto, preço e destino solicitados. */
  private void validateCheckoutContract(
      Experiment experiment,
      PdeProductionSlot activeSlot,
      LeadPortalPaymentsClient.CommercialProductCheckoutResponse response) {
    boolean valid =
        response != null
            && experiment.getProduct().getSlug().equals(response.productKey())
            && experiment.getProduct().getId().equals(response.productId())
            && experiment.getId().equals(response.experimentId())
            && response.amount() != null
            && experiment.getUnitPrice().compareTo(response.amount()) == 0
            && "BRL".equals(response.currency())
            && normalizeDomain(activeSlot.getPublicUrl())
                .equals(normalizeDomain(response.deliveryPageUrl()))
            && StringUtils.hasText(response.checkoutUrl());
    if (!valid) {
      throw new IllegalStateException(
          "Serviço de pagamentos devolveu checkout divergente do contrato comercial solicitado");
    }
  }
}
