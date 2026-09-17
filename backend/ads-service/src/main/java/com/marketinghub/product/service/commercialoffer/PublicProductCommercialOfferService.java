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
import java.util.List;
import java.util.Objects;
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
    return getOffer(productSlug, null, null);
  }

  /** Retorna a oferta do slot ou da versão pedidos sem misturar experimentos paralelos. */
  @Transactional(readOnly = true)
  public PublicProductCommercialOfferResponse getOffer(
      String productSlug, String slotCode, String experienceVersion) {
    return buildOffer(productSlug, findSaleableSlot(productSlug, slotCode, experienceVersion));
  }

  /**
   * Monta a oferta da versão exata para o preflight autenticado, inclusive quando ainda candidata.
   */
  @Transactional(readOnly = true)
  public PublicProductCommercialOfferResponse getValidationOffer(
      String productSlug, String slotCode, String experienceVersion) {
    return buildOffer(productSlug, findValidationSlot(productSlug, slotCode, experienceVersion));
  }

  /** Monta a resposta comercial depois que o seletor de slot já foi validado. */
  private PublicProductCommercialOfferResponse buildOffer(
      String productSlug, PdeProductionSlot slot) {
    Product product =
        productRepository
            .findBySlug(normalizeRequired(productSlug, "Produto obrigatório"))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
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
   * Resolve primeiro o produto para manter a seleção pública compatível com o contrato existente.
   */
  private PdeProductionSlot findSaleableSlot(
      String productSlug, String requestedSlotCode, String requestedExperienceVersion) {
    Product product =
        productRepository
            .findBySlug(normalizeRequired(productSlug, "Produto obrigatório"))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
    return findSaleableSlot(product, requestedSlotCode, requestedExperienceVersion);
  }

  /**
   * Seleciona somente candidata, pronta ou ativa por identidade explícita para impedir mistura no
   * preflight.
   */
  private PdeProductionSlot findValidationSlot(
      String productSlug, String requestedSlotCode, String requestedExperienceVersion) {
    Product product =
        productRepository
            .findBySlug(normalizeRequired(productSlug, "Produto obrigatório"))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
    String slotCode = normalizeOptional(requestedSlotCode);
    String experienceVersion = normalizeOptional(requestedExperienceVersion);
    if (!StringUtils.hasText(slotCode) && !StringUtils.hasText(experienceVersion)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Preflight PDE exige slot ou versão explícitos.");
    }
    var candidates =
        slotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug()).stream()
            .filter(slot -> slot.getSourceExperimentId() != null)
            .filter(
                slot ->
                    slot.getStatus() == PdeProductionSlotStatus.CANDIDATE
                        || slot.getStatus() == PdeProductionSlotStatus.READY
                        || slot.getStatus() == PdeProductionSlotStatus.ACTIVE)
            .filter(
                slot ->
                    !StringUtils.hasText(slotCode) || slotCode.equalsIgnoreCase(slot.getSlotCode()))
            .filter(
                slot ->
                    !StringUtils.hasText(experienceVersion)
                        || experienceVersion.equals(slot.getExperienceVersion()))
            .toList();
    if (candidates.size() != 1) {
      throw new ResponseStatusException(
          candidates.isEmpty() ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT,
          candidates.isEmpty()
              ? "Versão PDE solicitada não encontrada."
              : "Seletores de versão PDE não identificam uma única candidata.");
    }
    return candidates.get(0);
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
  private PdeProductionSlot findSaleableSlot(
      Product product, String requestedSlotCode, String requestedExperienceVersion) {
    String slotCode = normalizeOptional(requestedSlotCode);
    String experienceVersion = normalizeOptional(requestedExperienceVersion);
    List<PdeProductionSlot> candidates =
        slotRepository.findByProductSlugOrderBySlotCodeAsc(product.getSlug()).stream()
            .filter(slot -> slot.getSourceExperimentId() != null)
            .filter(
                slot ->
                    slot.getStatus() == PdeProductionSlotStatus.READY
                        || slot.getStatus() == PdeProductionSlotStatus.ACTIVE)
            .toList();
    if (StringUtils.hasText(slotCode)) {
      PdeProductionSlot selected =
          candidates.stream()
              .filter(slot -> slotCode.equalsIgnoreCase(slot.getSlotCode()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.NOT_FOUND, "Slot PDE solicitado não encontrado."));
      if (StringUtils.hasText(experienceVersion)
          && !experienceVersion.equals(selected.getExperienceVersion())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Seletores de versão PDE divergentes.");
      }
      return selected;
    }
    var stream = candidates.stream();
    if (StringUtils.hasText(experienceVersion)) {
      stream = stream.filter(slot -> experienceVersion.equals(slot.getExperienceVersion()));
    } else {
      stream =
          stream.filter(
              slot ->
                  experimentRepository
                      .findById(slot.getSourceExperimentId())
                      .map(Experiment::getProduct)
                      .map(Product::getId)
                      .filter(id -> Objects.equals(id, product.getId()))
                      .isPresent());
    }
    return stream
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

  /** Normaliza um seletor opcional preservando ausência para compatibilidade pública. */
  private String normalizeOptional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Usa o primeiro texto disponível preservando o cadastro do experimento como prioridade. */
  private String firstRequired(String primary, String fallback, String message) {
    return normalizeRequired(StringUtils.hasText(primary) ? primary : fallback, message);
  }
}
