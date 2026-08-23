package com.marketinghub.product.service.commercialoffer;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Monta a oferta pública de um produto a partir do produto, slot e experimento canônicos. */
@Service
public class PublicProductCommercialOfferService {
  private static final Set<ExperimentStatus> SALEABLE_EXPERIMENT_STATUSES =
      Set.of(ExperimentStatus.PLANNED, ExperimentStatus.RUNNING);

  private final ProductRepository productRepository;
  private final PdeProductionSlotRepository slotRepository;
  private final ExperimentRepository experimentRepository;
  private final String supplierLegalName;
  private final String supplierRegistrationNumber;
  private final String supplierAddress;
  private final String supplierSupportEmail;

  /** Inicializa a leitura comercial sem duplicar dados no frontend PDE. */
  public PublicProductCommercialOfferService(
      ProductRepository productRepository,
      PdeProductionSlotRepository slotRepository,
      ExperimentRepository experimentRepository,
      @Value("${commerce.supplier.legal-name:}") String supplierLegalName,
      @Value("${commerce.supplier.registration-number:}") String supplierRegistrationNumber,
      @Value("${commerce.supplier.address:}") String supplierAddress,
      @Value("${commerce.supplier.support-email:}") String supplierSupportEmail) {
    this.productRepository = productRepository;
    this.slotRepository = slotRepository;
    this.experimentRepository = experimentRepository;
    this.supplierLegalName = supplierLegalName;
    this.supplierRegistrationNumber = supplierRegistrationNumber;
    this.supplierAddress = supplierAddress;
    this.supplierSupportEmail = supplierSupportEmail;
  }

  /** Retorna somente oferta completa, vendável e vinculada a um slot publicado. */
  @Transactional(readOnly = true)
  public PublicProductCommercialOfferResponse getOffer(String productSlug) {
    Product product =
        productRepository
            .findBySlug(normalizeRequired(productSlug, "Produto obrigatório"))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
    PdeProductionSlot slot = findSaleableSlot(product.getSlug());
    Experiment experiment =
        experimentRepository
            .findById(slot.getSourceExperimentId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.PRECONDITION_FAILED,
                        "Slot PDE sem experimento comercial disponível."));
    validateExperiment(product, experiment);
    String legalName = normalizeRequired(supplierLegalName, "Fornecedor sem razão social.");
    String registrationNumber =
        normalizeRequired(supplierRegistrationNumber, "Fornecedor sem registro fiscal.");
    String address = normalizeRequired(supplierAddress, "Fornecedor sem endereço comercial.");
    String supportEmail =
        normalizeRequired(supplierSupportEmail, "Fornecedor sem contato de suporte.");
    if (!supportEmail.contains("@")) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "Contato de suporte do fornecedor é inválido.");
    }
    String salesPageUrl = slot.getPublicUrl().replaceAll("/+$", "");
    return new PublicProductCommercialOfferResponse(
        product.getSlug(),
        experiment.getId(),
        experiment.getStatus().name(),
        experiment.getPlatform() != null ? experiment.getPlatform().name() : null,
        normalizeRequired(experiment.getSinglePain(), "Oferta sem dor principal."),
        normalizeRequired(experiment.getFreeReward(), "Oferta sem prova de valor."),
        normalizeRequired(experiment.getFunnelPromise(), "Oferta sem promessa comercial."),
        firstRequired(experiment.getPrimaryCta(), product.getPrimaryCta(), "Oferta sem CTA."),
        experiment.getUnitPrice(),
        experiment.getCommercialCheckoutUrl().trim(),
        salesPageUrl,
        product.getTargetAudience(),
        product.getProductFormat(),
        product.getDeliveryMode(),
        product.getValueUnit(),
        legalName,
        registrationNumber,
        address,
        supportEmail,
        salesPageUrl + "/terms",
        salesPageUrl + "/privacy",
        salesPageUrl + "/refund-policy");
  }

  /**
   * Seleciona o slot mais recente que pode ser homologado ou vender sem expor versões aposentadas.
   */
  private PdeProductionSlot findSaleableSlot(String productSlug) {
    return slotRepository.findByProductSlugOrderBySlotCodeAsc(productSlug).stream()
        .filter(slot -> slot.getSourceExperimentId() != null)
        .filter(
            slot ->
                slot.getStatus() == PdeProductionSlotStatus.READY
                    || slot.getStatus() == PdeProductionSlotStatus.ACTIVE)
        .max(
            Comparator.comparing(
                PdeProductionSlot::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.PRECONDITION_FAILED,
                    "Produto sem slot PDE pronto para oferta comercial."));
  }

  /**
   * Impede que um slot publique checkout incompleto, produto divergente ou experimento encerrado.
   */
  private void validateExperiment(Product product, Experiment experiment) {
    if (experiment.getProduct() == null
        || !product.getId().equals(experiment.getProduct().getId())) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "Experimento não pertence ao produto do slot PDE.");
    }
    if (experiment.getStatus() == null
        || !SALEABLE_EXPERIMENT_STATUSES.contains(experiment.getStatus())) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "Experimento não está disponível para venda.");
    }
    if (experiment.getUnitPrice() == null
        || experiment.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "Oferta sem preço comercial válido.");
    }
    String checkout =
        normalizeRequired(experiment.getCommercialCheckoutUrl(), "Oferta sem checkout comercial.");
    if (!checkout.startsWith("https://")) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "Checkout comercial precisa usar HTTPS.");
    }
  }

  /** Normaliza um texto obrigatório e devolve erro funcional quando o gate não fecha. */
  private String normalizeRequired(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, message);
    }
    return value.trim();
  }

  /** Usa o primeiro texto disponível preservando o cadastro do experimento como prioridade. */
  private String firstRequired(String primary, String fallback, String message) {
    return normalizeRequired(StringUtils.hasText(primary) ? primary : fallback, message);
  }
}
