package com.marketinghub.product.service.commercialoffer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver.CanonicalCheckout;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Monta a oferta pública de um produto a partir do produto, slot e experimento canônicos. */
@Service
public class PublicProductCommercialOfferService {
  private static final Logger log =
      LoggerFactory.getLogger(PublicProductCommercialOfferService.class);
  private static final Set<ExperimentStatus> SALEABLE_EXPERIMENT_STATUSES =
      Set.of(ExperimentStatus.PLANNED, ExperimentStatus.RUNNING);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final ProductRepository productRepository;
  private final PdeProductionSlotRepository slotRepository;
  private final ExperimentRepository experimentRepository;
  private final PdeCommercialCheckoutContractResolver checkoutResolver;
  private final String supplierDisplayName;
  private final String supplierRegistrationNumber;
  private final String supplierSupportEmail;

  /** Inicializa a leitura comercial sem duplicar dados no frontend PDE. */
  public PublicProductCommercialOfferService(
      ProductRepository productRepository,
      PdeProductionSlotRepository slotRepository,
      ExperimentRepository experimentRepository,
      PdeCommercialCheckoutContractResolver checkoutResolver,
      @Value("${commerce.supplier.display-name:}") String supplierDisplayName,
      @Value("${commerce.supplier.registration-number:}") String supplierRegistrationNumber,
      @Value("${commerce.supplier.support-email:}") String supplierSupportEmail) {
    this.productRepository = productRepository;
    this.slotRepository = slotRepository;
    this.experimentRepository = experimentRepository;
    this.checkoutResolver = checkoutResolver;
    this.supplierDisplayName = supplierDisplayName;
    this.supplierRegistrationNumber = supplierRegistrationNumber;
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
    CanonicalCheckout canonicalCheckout = checkoutResolver.resolve(product).orElse(null);
    String checkoutUrl =
        canonicalCheckout == null
            ? experiment.getCommercialCheckoutUrl()
            : canonicalCheckout.checkoutUrl();
    validateExperiment(product, experiment, canonicalCheckout, checkoutUrl);
    String displayName = normalizeRequired(supplierDisplayName, "Fornecedor sem marca pública.");
    String registrationNumber =
        normalizeRequired(supplierRegistrationNumber, "Fornecedor sem registro fiscal.");
    String supportEmail =
        normalizeRequired(supplierSupportEmail, "Fornecedor sem contato de suporte.");
    if (!supportEmail.contains("@")) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "Contato de suporte do fornecedor é inválido.");
    }
    String salesPageUrl = slot.getPublicUrl().replaceAll("/+$", "");
    String primaryCta =
        firstRequired(experiment.getPrimaryCta(), product.getPrimaryCta(), "Oferta sem CTA.");
    validateVersionedCommercialBinding(slot, experiment, primaryCta);
    return new PublicProductCommercialOfferResponse(
        product.getSlug(),
        slot.getExperienceVersion(),
        slot.getLayoutKey(),
        experiment.getId(),
        experiment.getStatus().name(),
        experiment.getPlatform() != null ? experiment.getPlatform().name() : null,
        normalizeRequired(experiment.getSinglePain(), "Oferta sem dor principal."),
        normalizeRequired(experiment.getFreeReward(), "Oferta sem prova de valor."),
        normalizeRequired(experiment.getFunnelPromise(), "Oferta sem promessa comercial."),
        primaryCta,
        experiment.getUnitPrice(),
        checkoutUrl.trim(),
        salesPageUrl,
        product.getTargetAudience(),
        product.getProductFormat(),
        product.getDeliveryMode(),
        product.getValueUnit(),
        displayName,
        registrationNumber,
        supportEmail,
        salesPageUrl + "/terms",
        salesPageUrl + "/privacy",
        salesPageUrl + "/refund-policy");
  }

  /**
   * Impede que a experiência assistida v2 combine versão, CTA, preço ou experimento divergentes.
   */
  private void validateVersionedCommercialBinding(
      PdeProductionSlot slot, Experiment experiment, String primaryCta) {
    boolean requiresBinding =
        "assisted-service-v2".equals(slot.getLayoutKey())
            || (StringUtils.hasText(slot.getExperienceVersion())
                && slot.getExperienceVersion().contains("pde-v2"));
    if (!requiresBinding) {
      return;
    }
    String contract =
        normalizeRequired(
            slot.getPublishedExperienceJson(), "Experiência v2 sem contrato publicado.");
    try {
      JsonNode root = OBJECT_MAPPER.readTree(contract);
      JsonNode binding = root.path("commercialBinding");
      String funnelPromise =
          normalizeRequired(experiment.getFunnelPromise(), "Oferta sem promessa comercial.");
      boolean aligned =
          slot.getProductSlug().equals(root.path("slug").asText())
              && slot.getExperienceVersion().equals(root.path("experienceVersion").asText())
              && slot.getLayoutKey().equals(root.path("layoutKey").asText())
              && funnelPromise.equals(root.path("promise").asText())
              && experiment.getId().equals(binding.path("experimentId").longValue())
              && primaryCta.equals(binding.path("primaryCta").asText())
              && experiment.getUnitPrice().compareTo(binding.path("priceBrl").decimalValue()) == 0
              && "ONE_TIME".equals(binding.path("billingModel").asText());
      if (!aligned) {
        throw new ResponseStatusException(
            HttpStatus.PRECONDITION_FAILED,
            "Contrato PDE v2 diverge da oferta comercial canônica.");
      }
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao validar contrato comercial PDE v2: productSlug={}, slotCode={}, experimentId={}",
          slot.getProductSlug(),
          slot.getSlotCode(),
          experiment.getId(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "Contrato PDE v2 publicado é inválido.", ex);
    }
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
  private void validateExperiment(
      Product product,
      Experiment experiment,
      CanonicalCheckout canonicalCheckout,
      String checkoutUrl) {
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
    String checkout = normalizeRequired(checkoutUrl, "Oferta sem checkout comercial.");
    if (!checkout.startsWith("https://")) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "Checkout comercial precisa usar HTTPS.");
    }
    if (canonicalCheckout != null
        && experiment.getUnitPrice().compareTo(canonicalCheckout.priceBrl()) != 0) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED,
          "Preço do experimento diverge do checkout versionado do PDE.");
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
