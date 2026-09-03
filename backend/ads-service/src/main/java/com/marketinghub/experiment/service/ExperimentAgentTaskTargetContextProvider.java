package com.marketinghub.experiment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskTargetContextProvider;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver.CanonicalCheckout;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: publicar o alvo comercial segregado das tarefas ligadas a PDEs. */
@Service
public class ExperimentAgentTaskTargetContextProvider implements AgentTaskTargetContextProvider {
  private static final Logger log =
      LoggerFactory.getLogger(ExperimentAgentTaskTargetContextProvider.class);
  private static final Pattern EXPERIMENT_REFERENCE =
      Pattern.compile("experiment:([1-9][0-9]*)(?:@[^:]+)?(?:[:].*)?");
  private static final Pattern PRODUCT_REFERENCE =
      Pattern.compile("product:([1-9][0-9]*)(?:@[^:]+)?(?:[:].*)?");
  private static final Pattern COMMERCIAL_PLAN_REFERENCE =
      Pattern.compile("commercial-plan:([1-9][0-9]*)(?:@[^:]+)?(?:[:].*)?");
  private static final Pattern EXPERIMENT_SEGMENT =
      Pattern.compile("(?:^|:)experiment-([1-9][0-9]*)(?:$|:)");
  private static final List<String> VERSIONED_PDE_VISUAL_PROCESSES =
      List.of("pde-commercial-homologation-activation", "pde-construction-approval");
  private final ExperimentRepository experiments;
  private final ProductRepository products;
  private final PdeProductionSlotRepository productionSlots;
  private final CommercialPlanRepository commercialPlans;
  private final ObjectMapper objectMapper;
  private final PdeCommercialCheckoutContractResolver checkoutResolver;

  /** Configura as fontes canônicas de experimento, produto e contrato PDE. */
  @Autowired
  public ExperimentAgentTaskTargetContextProvider(
      ExperimentRepository experiments,
      ProductRepository products,
      ObjectMapper objectMapper,
      PdeProductionSlotRepository productionSlots,
      CommercialPlanRepository commercialPlans,
      PdeCommercialCheckoutContractResolver checkoutResolver) {
    this.experiments = experiments;
    this.products = products;
    this.objectMapper = objectMapper;
    this.productionSlots = productionSlots;
    this.commercialPlans = commercialPlans;
    this.checkoutResolver = checkoutResolver;
  }

  /** Mantém testes focados na identidade comercial sem exigir catálogo de slots produtivos. */
  ExperimentAgentTaskTargetContextProvider(
      ExperimentRepository experiments, ProductRepository products, ObjectMapper objectMapper) {
    this(
        experiments,
        products,
        objectMapper,
        null,
        null,
        new PdeCommercialCheckoutContractResolver(objectMapper));
  }

  /** Permite testar a resolução versionada do slot sem carregar um plano comercial. */
  ExperimentAgentTaskTargetContextProvider(
      ExperimentRepository experiments,
      ProductRepository products,
      ObjectMapper objectMapper,
      PdeProductionSlotRepository productionSlots) {
    this(
        experiments,
        products,
        objectMapper,
        productionSlots,
        null,
        new PdeCommercialCheckoutContractResolver(objectMapper));
  }

  /** Permite testar a resolução por plano usando o mesmo contrato canônico de checkout. */
  ExperimentAgentTaskTargetContextProvider(
      ExperimentRepository experiments,
      ProductRepository products,
      ObjectMapper objectMapper,
      PdeProductionSlotRepository productionSlots,
      CommercialPlanRepository commercialPlans) {
    this(
        experiments,
        products,
        objectMapper,
        productionSlots,
        commercialPlans,
        new PdeCommercialCheckoutContractResolver(objectMapper));
  }

  /** Resolve somente referências explícitas e nunca usa nome livre da tarefa como identidade. */
  @Override
  @Transactional(readOnly = true)
  public Optional<AgentTaskTargetResponse> resolve(String sourceReference) {
    return resolve(sourceReference, null);
  }

  /** Separa a landing do experimento da tela da versão produtiva exata do PDE. */
  @Override
  @Transactional(readOnly = true)
  public Optional<AgentTaskTargetResponse> resolve(String sourceReference, String processCode) {
    if (sourceReference == null || sourceReference.isBlank()) return Optional.empty();
    String normalized = sourceReference.trim();
    Matcher experimentMatcher = EXPERIMENT_REFERENCE.matcher(normalized);
    if (experimentMatcher.matches()) {
      return experiments
          .findById(Long.valueOf(experimentMatcher.group(1)))
          .flatMap(
              experiment -> target(normalized, experiment, experiment.getProduct(), processCode));
    }
    Matcher productMatcher = PRODUCT_REFERENCE.matcher(normalized);
    if (productMatcher.matches()) {
      return products
          .findById(Long.valueOf(productMatcher.group(1)))
          .flatMap(product -> target(normalized, null, product, processCode));
    }
    Matcher planMatcher = COMMERCIAL_PLAN_REFERENCE.matcher(normalized);
    if (!planMatcher.matches() || commercialPlans == null) return Optional.empty();
    return commercialPlans
        .findById(Long.valueOf(planMatcher.group(1)))
        .flatMap(plan -> commercialPlanExperiment(plan, normalized))
        .flatMap(
            experiment -> target(normalized, experiment, experiment.getProduct(), processCode));
  }

  /** Exige o experimento explícito das referências novas e limita o legado ao vínculo primário. */
  private Optional<Experiment> commercialPlanExperiment(
      CommercialPlan plan, String sourceReference) {
    Matcher segment = EXPERIMENT_SEGMENT.matcher(sourceReference);
    if (segment.find()) {
      Long experimentId = Long.valueOf(segment.group(1));
      return experiments
          .findById(experimentId)
          .filter(experiment -> belongsToPlan(plan, experimentId));
    }
    if (plan.getExperiment() != null) return Optional.of(plan.getExperiment());
    if (plan.getExperiments() != null && plan.getExperiments().size() == 1) {
      return plan.getExperiments().stream().findFirst();
    }
    return Optional.empty();
  }

  /** Confirma que o experimento declarado realmente pertence ao plano antes de expor sua URL. */
  private boolean belongsToPlan(CommercialPlan plan, Long experimentId) {
    return (plan.getExperiment() != null
            && Objects.equals(plan.getExperiment().getId(), experimentId))
        || (plan.getExperiments() != null
            && plan.getExperiments().stream()
                .anyMatch(experiment -> Objects.equals(experiment.getId(), experimentId)));
  }

  /** Monta a identidade mínima exigida para impedir mistura de produtos ou versões. */
  private Optional<AgentTaskTargetResponse> target(
      String sourceReference, Experiment experiment, Product product, String processCode) {
    if (product == null || product.getId() == null || blank(product.getSlug())) {
      return Optional.empty();
    }
    String experienceVersion = experienceVersion(product);
    if (blank(experienceVersion)) return Optional.empty();
    Optional<CanonicalCheckout> canonicalCheckout =
        canonicalCheckout(experiment, product, processCode);
    return Optional.of(
        new AgentTaskTargetResponse(
            sourceReference,
            experiment == null ? null : experiment.getId(),
            product.getId(),
            product.getSlug(),
            product.getName(),
            product.getInternalName(),
            experienceVersion,
            publicUrl(experiment, product, experienceVersion, processCode),
            canonicalCheckout.map(CanonicalCheckout::provider).orElse(null),
            canonicalCheckout.map(CanonicalCheckout::offerReference).orElse(null),
            canonicalCheckout
                .map(CanonicalCheckout::checkoutUrl)
                .orElse(experiment == null ? null : experiment.getCommercialCheckoutUrl()),
            commercialPrice(experiment, product, canonicalCheckout),
            pdeContext(product, processCode)));
  }

  /** Entrega o contrato PDE estruturado somente à construção privada do próprio produto. */
  private JsonNode pdeContext(Product product, String processCode) {
    if (!isPrivateValidation(product, processCode) || blank(product.getPdeExperienceJson())) {
      return null;
    }
    try {
      JsonNode context = objectMapper.readTree(product.getPdeExperienceJson());
      return context.isObject() ? context : null;
    } catch (Exception ex) {
      log.error(
          "Contrato PDE privado inválido ao montar contexto da tarefa. productId={} productSlug={} processCode={}",
          product.getId(),
          product.getSlug(),
          processCode,
          ex);
      return null;
    }
  }

  /** Usa o preço da versão PDE e bloqueia qualquer experimento comercial divergente. */
  private BigDecimal commercialPrice(
      Experiment experiment, Product product, Optional<CanonicalCheckout> canonicalCheckout) {
    BigDecimal fallback =
        experiment == null ? product.getCurrentPriceBrl() : experiment.getUnitPrice();
    if (canonicalCheckout.isEmpty()) return fallback;
    BigDecimal canonicalPrice = canonicalCheckout.orElseThrow().priceBrl();
    if (fallback != null && fallback.compareTo(canonicalPrice) != 0) {
      throw new IllegalStateException(
          "Preço do alvo comercial diverge do checkout versionado do contrato PDE");
    }
    return canonicalPrice;
  }

  /** Usa o checkout da mesma versão PDE somente nos processos que revisam essa experiência. */
  private Optional<CanonicalCheckout> canonicalCheckout(
      Experiment experiment, Product product, String processCode) {
    if (isPrivateValidation(product, processCode)) return Optional.empty();
    if (processCode != null && VERSIONED_PDE_VISUAL_PROCESSES.contains(processCode)) {
      return checkoutResolver.resolve(product);
    }
    return Optional.empty();
  }

  /** Resolve a tela exata do PDE ou mantém a landing própria do experimento conforme o processo. */
  private String publicUrl(
      Experiment experiment, Product product, String experienceVersion, String processCode) {
    if (isPrivateValidation(product, processCode)) {
      return privatePrototypeUrl(product);
    }
    if (processCode != null && VERSIONED_PDE_VISUAL_PROCESSES.contains(processCode)) {
      if (productionSlots == null) return null;
      return productionSlots
          .findFirstByProductSlugAndExperienceVersionAndStatusInOrderByPublishedAtDesc(
              product.getSlug(),
              experienceVersion,
              List.of(PdeProductionSlotStatus.READY, PdeProductionSlotStatus.ACTIVE))
          .map(slot -> blank(slot.getPublicUrl()) ? null : slot.getPublicUrl().trim())
          .orElse(null);
    }
    if (experiment != null && !blank(experiment.getFollowUpActionUrl())) {
      return experiment.getFollowUpActionUrl().trim();
    }
    return blank(product.getPublicUrl()) ? null : product.getPublicUrl().trim();
  }

  /** Identifica a construção privada para impedir uso de slot público ou checkout real. */
  private boolean isPrivateValidation(Product product, String processCode) {
    return "pde-construction-approval".equals(processCode)
        && product.getValidationDefinitionVersion() != null
        && product.getValidationDefinitionVersion().startsWith("PDE_PRIVATE_VALIDATION_V1");
  }

  /** Lê a URL da versão privada aceita, sem confundi-la com uma publicação produtiva. */
  private String privatePrototypeUrl(Product product) {
    if (blank(product.getValidationDefinitionJson())) return null;
    try {
      JsonNode acceptance =
          objectMapper
              .readTree(product.getValidationDefinitionJson())
              .path("privatePrototypeAcceptance");
      String value = acceptance.path("privateAccessUrl").asText(null);
      return "READY".equals(acceptance.path("status").asText()) && !blank(value)
          ? value.trim()
          : null;
    } catch (Exception ex) {
      log.error(
          "Contrato privado inválido ao resolver alvo da tarefa. productId={} productSlug={}",
          product.getId(),
          product.getSlug(),
          ex);
      return null;
    }
  }

  /** Extrai a versão funcional do JSON canônico persistido no produto. */
  private String experienceVersion(Product product) {
    if (blank(product.getPdeExperienceJson())) return null;
    try {
      JsonNode contract = objectMapper.readTree(product.getPdeExperienceJson());
      String value = contract.path("experienceVersion").asText(null);
      return blank(value) ? null : value.trim();
    } catch (Exception ex) {
      log.error(
          "Contrato PDE inválido ao resolver alvo da tarefa. productId={} productSlug={}",
          product.getId(),
          product.getSlug(),
          ex);
      return null;
    }
  }

  /** Verifica ausência sem transformar texto vazio em identidade válida. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
