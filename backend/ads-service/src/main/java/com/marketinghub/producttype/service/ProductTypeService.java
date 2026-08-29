package com.marketinghub.producttype.service;

import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.producttype.ProductTypeStatus;
import com.marketinghub.producttype.service.catalog.ProductTypeBlueprintData;
import com.marketinghub.producttype.service.catalog.ProductTypeCatalogItemResponse;
import com.marketinghub.producttype.service.catalog.SaveProductTypeRequest;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: gerenciar catálogo, identidade e base de construção dos tipos de produto. */
@Service
public class ProductTypeService {
  private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
  private static final Pattern NON_CODE_PATTERN = Pattern.compile("[^A-Z0-9]+");
  private static final List<String> BLUEPRINT_FIELD_LABELS =
      List.of(
          "Versão da base",
          "Canal principal",
          "Trabalho do cliente",
          "Mecanismo de valor",
          "Jornada base",
          "Entradas obrigatórias",
          "Saídas esperadas",
          "Memória e segregação",
          "Integrações obrigatórias",
          "Segurança e bloqueios",
          "Métricas de sucesso",
          "SDK Java");

  private final ProductTypeDefinitionRepository repository;
  private final ProductRepository productRepository;

  /** Inicializa o serviço com as fontes canônicas de tipos e produtos. */
  public ProductTypeService(
      ProductTypeDefinitionRepository repository, ProductRepository productRepository) {
    this.repository = repository;
    this.productRepository = productRepository;
  }

  /** Lista tipos ativos ou todo o histórico, com pesquisa por código, nome interno e apelido. */
  @Transactional(readOnly = true)
  public List<ProductTypeCatalogItemResponse> list(String query, boolean includeRetired) {
    String canonicalQuery = canonicalIdentity(query);
    return repository.findAllByOrderByNameAsc().stream()
        .filter(type -> includeRetired || type.getStatus() == ProductTypeStatus.ACTIVE)
        .filter(type -> canonicalQuery.isEmpty() || matches(type, canonicalQuery))
        .map(this::toResponse)
        .toList();
  }

  /** Retorna um tipo específico preservado no catálogo. */
  @Transactional(readOnly = true)
  public ProductTypeCatalogItemResponse get(Long id) {
    return toResponse(getEntity(id));
  }

  /** Cria uma classificação e valida sua base sem depender de enum ou alteração de código. */
  @Transactional
  public ProductTypeCatalogItemResponse create(SaveProductTypeRequest request) {
    ProductTypeDefinition type = new ProductTypeDefinition();
    apply(type, request, true);
    return toResponse(repository.save(type));
  }

  /** Atualiza identidade, base e disponibilidade sem perder os produtos vinculados. */
  @Transactional
  public ProductTypeCatalogItemResponse update(Long id, SaveProductTypeRequest request) {
    ProductTypeDefinition type = getEntity(id);
    String previousName = type.getName();
    apply(type, request, false);
    ProductTypeDefinition saved = repository.save(type);
    if (!previousName.equals(saved.getName())) {
      List<Product> linkedProducts = productRepository.findAllByProductTypeDefinition_Id(id);
      linkedProducts.forEach(product -> product.setProductType(saved.getName()));
      productRepository.saveAll(linkedProducts);
    }
    return toResponse(saved);
  }

  /** Aplica e valida a definição mantendo código estável quando já existem produtos vinculados. */
  private void apply(ProductTypeDefinition type, SaveProductTypeRequest request, boolean creating) {
    ProductTypeStatus previousStatus = type.getStatus();
    String name = normalizeRequired(request.name(), "Informe o nome do tipo de produto.");
    String internalName =
        normalizeRequired(request.internalName(), "Informe o nome interno do tipo de produto.");
    String requestedCode = normalizeCode(request.code(), name);
    long productCount =
        type.getId() == null ? 0 : productRepository.countByProductTypeDefinition_Id(type.getId());
    if (!creating
        && productCount > 0
        && type.getCode() != null
        && !type.getCode().equals(requestedCode)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O código do tipo não pode mudar enquanto houver produtos vinculados.");
    }
    Set<String> aliases = normalizeAliases(request.aliases(), requestedCode, name, internalName);
    validateUniqueIdentity(type.getId(), requestedCode, name, internalName, aliases);
    ProductTypeStatus status =
        request.status() == null ? ProductTypeStatus.PROPOSED : request.status();
    String description = normalizeOptional(request.description());
    if (status == ProductTypeStatus.ACTIVE && description == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Explique quando usar o tipo antes de colocá-lo em uso.");
    }
    ProductTypeBlueprintData blueprint = normalizeBlueprint(request.blueprint());
    List<String> missingBlueprintFields = missingBlueprintFields(blueprint);
    boolean activating = creating || previousStatus != ProductTypeStatus.ACTIVE;
    if (status == ProductTypeStatus.ACTIVE
        && (activating || request.blueprint() != null)
        && !missingBlueprintFields.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Complete a base de construção antes de colocar o tipo em uso: "
              + String.join(", ", missingBlueprintFields)
              + ".");
    }
    type.setCode(requestedCode);
    type.setName(name);
    type.setInternalName(internalName);
    type.setDescription(description);
    type.setAliases(aliases);
    type.setStatus(status);
    if (creating || request.blueprint() != null) {
      applyBlueprint(type, blueprint);
    }
  }

  /** Normaliza a base estruturada sem transformar um formulário totalmente vazio em contrato. */
  private ProductTypeBlueprintData normalizeBlueprint(ProductTypeBlueprintData requested) {
    if (requested == null) {
      return null;
    }
    ProductTypeBlueprintData normalized =
        new ProductTypeBlueprintData(
            normalizeOptional(requested.version()),
            normalizeChannel(requested.primaryChannel()),
            normalizeOptional(requested.customerJob()),
            normalizeOptional(requested.valueMechanism()),
            normalizeOptional(requested.experienceFlow()),
            normalizeOptional(requested.requiredInputs()),
            normalizeOptional(requested.expectedOutputs()),
            normalizeOptional(requested.memoryStrategy()),
            normalizeOptional(requested.integrationRequirements()),
            normalizeOptional(requested.safetyGuardrails()),
            normalizeOptional(requested.successMetrics()),
            normalizeOptional(requested.backendSdkModule()),
            normalizeOptional(requested.frontendSdkModule()));
    return hasBlueprintContent(normalized) ? normalized : null;
  }

  /** Persiste a base normalizada ou limpa todos os campos quando a definição foi removida. */
  private void applyBlueprint(ProductTypeDefinition type, ProductTypeBlueprintData blueprint) {
    type.setBlueprintVersion(blueprint == null ? null : blueprint.version());
    type.setPrimaryChannel(blueprint == null ? null : blueprint.primaryChannel());
    type.setCustomerJob(blueprint == null ? null : blueprint.customerJob());
    type.setValueMechanism(blueprint == null ? null : blueprint.valueMechanism());
    type.setExperienceFlow(blueprint == null ? null : blueprint.experienceFlow());
    type.setRequiredInputs(blueprint == null ? null : blueprint.requiredInputs());
    type.setExpectedOutputs(blueprint == null ? null : blueprint.expectedOutputs());
    type.setMemoryStrategy(blueprint == null ? null : blueprint.memoryStrategy());
    type.setIntegrationRequirements(blueprint == null ? null : blueprint.integrationRequirements());
    type.setSafetyGuardrails(blueprint == null ? null : blueprint.safetyGuardrails());
    type.setSuccessMetrics(blueprint == null ? null : blueprint.successMetrics());
    type.setBackendSdkModule(blueprint == null ? null : blueprint.backendSdkModule());
    type.setFrontendSdkModule(blueprint == null ? null : blueprint.frontendSdkModule());
  }

  /** Informa os campos ausentes para que a tela apresente a verdade calculada pelo backend. */
  private List<String> missingBlueprintFields(ProductTypeBlueprintData blueprint) {
    if (blueprint == null) {
      return BLUEPRINT_FIELD_LABELS;
    }
    List<String> values =
        List.of(
            valueOrEmpty(blueprint.version()),
            valueOrEmpty(blueprint.primaryChannel()),
            valueOrEmpty(blueprint.customerJob()),
            valueOrEmpty(blueprint.valueMechanism()),
            valueOrEmpty(blueprint.experienceFlow()),
            valueOrEmpty(blueprint.requiredInputs()),
            valueOrEmpty(blueprint.expectedOutputs()),
            valueOrEmpty(blueprint.memoryStrategy()),
            valueOrEmpty(blueprint.integrationRequirements()),
            valueOrEmpty(blueprint.safetyGuardrails()),
            valueOrEmpty(blueprint.successMetrics()),
            valueOrEmpty(blueprint.backendSdkModule()));
    List<String> missing = new java.util.ArrayList<>();
    for (int index = 0; index < values.size(); index++) {
      if (values.get(index).isBlank()) {
        missing.add(BLUEPRINT_FIELD_LABELS.get(index));
      }
    }
    if ("PWA".equals(blueprint.primaryChannel()) && isBlank(blueprint.frontendSdkModule())) {
      missing.add("SDK React");
    }
    return List.copyOf(missing);
  }

  /** Confirma se ao menos um campo da base foi informado pelo usuário. */
  private boolean hasBlueprintContent(ProductTypeBlueprintData blueprint) {
    return !isBlank(blueprint.version())
        || !isBlank(blueprint.primaryChannel())
        || !isBlank(blueprint.customerJob())
        || !isBlank(blueprint.valueMechanism())
        || !isBlank(blueprint.experienceFlow())
        || !isBlank(blueprint.requiredInputs())
        || !isBlank(blueprint.expectedOutputs())
        || !isBlank(blueprint.memoryStrategy())
        || !isBlank(blueprint.integrationRequirements())
        || !isBlank(blueprint.safetyGuardrails())
        || !isBlank(blueprint.successMetrics())
        || !isBlank(blueprint.backendSdkModule())
        || !isBlank(blueprint.frontendSdkModule());
  }

  /** Normaliza apelidos, remove redundâncias e limita o tamanho operacional do catálogo. */
  private Set<String> normalizeAliases(
      List<String> requestedAliases, String code, String name, String internalName) {
    if (requestedAliases == null) {
      return new LinkedHashSet<>();
    }
    if (requestedAliases.size() > 20) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe no máximo 20 apelidos por tipo de produto.");
    }
    Set<String> reserved =
        new LinkedHashSet<>(
            List.of(
                canonicalIdentity(code), canonicalIdentity(name), canonicalIdentity(internalName)));
    Map<String, String> uniqueAliases = new LinkedHashMap<>();
    for (String rawAlias : requestedAliases) {
      String alias = normalizeOptional(rawAlias);
      if (alias == null) {
        continue;
      }
      if (alias.length() > 191) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Cada apelido de tipo deve ter no máximo 191 caracteres.");
      }
      String canonicalAlias = canonicalIdentity(alias);
      if (!reserved.contains(canonicalAlias)) {
        uniqueAliases.putIfAbsent(canonicalAlias, alias);
      }
    }
    return new LinkedHashSet<>(uniqueAliases.values());
  }

  /** Impede que código, nome interno, nome canônico ou apelido resolvam para mais de um tipo. */
  private void validateUniqueIdentity(
      Long currentId, String code, String name, String internalName, Set<String> aliases) {
    Map<String, String> requestedIdentities = new LinkedHashMap<>();
    addRequestedIdentity(requestedIdentities, code);
    addRequestedIdentity(requestedIdentities, name);
    addRequestedIdentity(requestedIdentities, internalName);
    aliases.forEach(alias -> requestedIdentities.put(canonicalIdentity(alias), alias));
    for (ProductTypeDefinition existing : repository.findAllByOrderByNameAsc()) {
      if (existing.getId() != null && existing.getId().equals(currentId)) {
        continue;
      }
      Set<String> existingIdentities = new LinkedHashSet<>();
      existingIdentities.add(canonicalIdentity(existing.getCode()));
      existingIdentities.add(canonicalIdentity(existing.getName()));
      existingIdentities.add(canonicalIdentity(existing.getInternalName()));
      existing.getAliases().forEach(alias -> existingIdentities.add(canonicalIdentity(alias)));
      for (Map.Entry<String, String> requested : requestedIdentities.entrySet()) {
        if (existingIdentities.contains(requested.getKey())) {
          throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              "O nome, código ou apelido '"
                  + requested.getValue()
                  + "' já identifica outro tipo de produto.");
        }
      }
    }
  }

  /** Impede que código, nome e codinome sejam variações da mesma identidade dentro do tipo. */
  private void addRequestedIdentity(Map<String, String> identities, String value) {
    String previous = identities.putIfAbsent(canonicalIdentity(value), value);
    if (previous != null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "O nome interno, nome canônico e código do tipo devem ser identidades distintas.");
    }
  }

  /** Verifica se um tipo corresponde à busca canônica informada. */
  private boolean matches(ProductTypeDefinition type, String query) {
    return canonicalIdentity(type.getCode()).contains(query)
        || canonicalIdentity(type.getName()).contains(query)
        || canonicalIdentity(type.getInternalName()).contains(query)
        || type.getAliases().stream()
            .map(this::canonicalIdentity)
            .anyMatch(value -> value.contains(query));
  }

  /** Converte a entidade no contrato administrativo com a quantidade de produtos vinculados. */
  private ProductTypeCatalogItemResponse toResponse(ProductTypeDefinition type) {
    ProductTypeBlueprintData blueprint = toBlueprint(type);
    List<String> missingBlueprintFields = missingBlueprintFields(blueprint);
    return new ProductTypeCatalogItemResponse(
        type.getId(),
        type.getCode(),
        type.getName(),
        type.getInternalName(),
        type.getDescription(),
        type.getAliases().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
        type.getStatus(),
        blueprint,
        missingBlueprintFields.isEmpty(),
        missingBlueprintFields,
        productRepository.countByProductTypeDefinition_Id(type.getId()),
        type.getCreatedAt(),
        type.getUpdatedAt());
  }

  /** Converte os campos persistidos na base estruturada usada pelo contrato administrativo. */
  private ProductTypeBlueprintData toBlueprint(ProductTypeDefinition type) {
    ProductTypeBlueprintData blueprint =
        new ProductTypeBlueprintData(
            type.getBlueprintVersion(),
            type.getPrimaryChannel(),
            type.getCustomerJob(),
            type.getValueMechanism(),
            type.getExperienceFlow(),
            type.getRequiredInputs(),
            type.getExpectedOutputs(),
            type.getMemoryStrategy(),
            type.getIntegrationRequirements(),
            type.getSafetyGuardrails(),
            type.getSuccessMetrics(),
            type.getBackendSdkModule(),
            type.getFrontendSdkModule());
    return hasBlueprintContent(blueprint) ? blueprint : null;
  }

  /** Busca uma definição e devolve erro de negócio quando ela não existe. */
  private ProductTypeDefinition getEntity(Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Tipo de produto não encontrado."));
  }

  /** Gera ou normaliza um código estável em maiúsculas e sublinhados. */
  private String normalizeCode(String requestedCode, String name) {
    String source = normalizeOptional(requestedCode);
    String decomposed = Normalizer.normalize(source == null ? name : source, Normalizer.Form.NFD);
    String withoutDiacritics = DIACRITICS_PATTERN.matcher(decomposed).replaceAll("");
    String code =
        NON_CODE_PATTERN.matcher(withoutDiacritics.toUpperCase(Locale.ROOT)).replaceAll("_");
    code = code.replaceAll("^_+|_+$", "");
    if (code.isBlank() || code.length() > 64) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O código do tipo deve ter entre 1 e 64 caracteres.");
    }
    return code;
  }

  /** Canonicaliza identidades para comparar caixa, acentos e espaços sem ambiguidade. */
  private String canonicalIdentity(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
    String withoutDiacritics = DIACRITICS_PATTERN.matcher(decomposed).replaceAll("");
    return WHITESPACE_PATTERN.matcher(withoutDiacritics).replaceAll(" ").toLowerCase(Locale.ROOT);
  }

  /** Exige texto útil para os campos que identificam o tipo. */
  private String normalizeRequired(String value, String message) {
    String normalized = normalizeOptional(value);
    if (normalized == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    return normalized;
  }

  /** Remove espaços laterais de campos opcionais antes de persistir. */
  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** Normaliza o canal como identidade técnica estável. */
  private String normalizeChannel(String value) {
    String normalized = normalizeOptional(value);
    if (normalized == null) {
      return null;
    }
    return NON_CODE_PATTERN
        .matcher(normalized.toUpperCase(Locale.ROOT))
        .replaceAll("_")
        .replaceAll("^_+|_+$", "");
  }

  /** Converte nulo em vazio para validar campos sem listas que rejeitam nulos. */
  private String valueOrEmpty(String value) {
    return value == null ? "" : value;
  }

  /** Verifica ausência textual sem alterar o valor persistido. */
  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
