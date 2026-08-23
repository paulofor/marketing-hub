package com.marketinghub.producttype.service;

import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.producttype.ProductTypeStatus;
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

/** Responsabilidade: gerenciar o catálogo extensível de tipos de produto e seus apelidos. */
@Service
public class ProductTypeService {
  private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
  private static final Pattern NON_CODE_PATTERN = Pattern.compile("[^A-Z0-9]+");

  private final ProductTypeDefinitionRepository repository;
  private final ProductRepository productRepository;

  /** Inicializa o serviço com as fontes canônicas de tipos e produtos. */
  public ProductTypeService(
      ProductTypeDefinitionRepository repository, ProductRepository productRepository) {
    this.repository = repository;
    this.productRepository = productRepository;
  }

  /** Lista tipos ativos ou todo o histórico, com pesquisa por código, nome e apelido. */
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

  /** Cria uma nova classificação sem depender de enum ou alteração de código. */
  @Transactional
  public ProductTypeCatalogItemResponse create(SaveProductTypeRequest request) {
    ProductTypeDefinition type = new ProductTypeDefinition();
    apply(type, request, true);
    return toResponse(repository.save(type));
  }

  /** Atualiza nome, explicação, apelidos e disponibilidade sem perder os produtos vinculados. */
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
    String name = normalizeRequired(request.name(), "Informe o nome do tipo de produto.");
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
    Set<String> aliases = normalizeAliases(request.aliases(), requestedCode, name);
    validateUniqueIdentity(type.getId(), requestedCode, name, aliases);
    ProductTypeStatus status =
        request.status() == null ? ProductTypeStatus.PROPOSED : request.status();
    String description = normalizeOptional(request.description());
    if (status == ProductTypeStatus.ACTIVE && description == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Explique quando usar o tipo antes de colocá-lo em uso.");
    }
    type.setCode(requestedCode);
    type.setName(name);
    type.setDescription(description);
    type.setAliases(aliases);
    type.setStatus(status);
  }

  /** Normaliza apelidos, remove redundâncias e limita o tamanho operacional do catálogo. */
  private Set<String> normalizeAliases(List<String> requestedAliases, String code, String name) {
    if (requestedAliases == null) {
      return new LinkedHashSet<>();
    }
    if (requestedAliases.size() > 20) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe no máximo 20 apelidos por tipo de produto.");
    }
    Set<String> reserved = Set.of(canonicalIdentity(code), canonicalIdentity(name));
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

  /** Impede que código, nome ou apelido resolvam para mais de um tipo. */
  private void validateUniqueIdentity(
      Long currentId, String code, String name, Set<String> aliases) {
    Map<String, String> requestedIdentities = new LinkedHashMap<>();
    requestedIdentities.put(canonicalIdentity(code), code);
    requestedIdentities.put(canonicalIdentity(name), name);
    aliases.forEach(alias -> requestedIdentities.put(canonicalIdentity(alias), alias));
    for (ProductTypeDefinition existing : repository.findAllByOrderByNameAsc()) {
      if (existing.getId() != null && existing.getId().equals(currentId)) {
        continue;
      }
      Set<String> existingIdentities = new LinkedHashSet<>();
      existingIdentities.add(canonicalIdentity(existing.getCode()));
      existingIdentities.add(canonicalIdentity(existing.getName()));
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

  /** Verifica se um tipo corresponde à busca canônica informada. */
  private boolean matches(ProductTypeDefinition type, String query) {
    return canonicalIdentity(type.getCode()).contains(query)
        || canonicalIdentity(type.getName()).contains(query)
        || type.getAliases().stream()
            .map(this::canonicalIdentity)
            .anyMatch(value -> value.contains(query));
  }

  /** Converte a entidade no contrato administrativo com a quantidade de produtos vinculados. */
  private ProductTypeCatalogItemResponse toResponse(ProductTypeDefinition type) {
    return new ProductTypeCatalogItemResponse(
        type.getId(),
        type.getCode(),
        type.getName(),
        type.getDescription(),
        type.getAliases().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
        type.getStatus(),
        productRepository.countByProductTypeDefinition_Id(type.getId()),
        type.getCreatedAt(),
        type.getUpdatedAt());
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
}
