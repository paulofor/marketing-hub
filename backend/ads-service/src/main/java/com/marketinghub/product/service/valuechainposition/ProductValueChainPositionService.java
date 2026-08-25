package com.marketinghub.product.service.valuechainposition;

import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: localizar produtos na cadeia de valor PDE vigente sem inferência no frontend.
 */
@Service
public class ProductValueChainPositionService {
  private static final String PDE_CHAIN_CODE = "pde-value-creation-delivery";
  private static final String PUBLISHED_STATUS = "PUBLISHED";

  private final ProductRepository productRepository;
  private final BusinessProcessChainDefinitionRepository chainRepository;
  private final ProductSubprocessPositionResolver subprocessResolver;
  private final PdeProcessCodeResolver processCodeResolver;
  private final ProductStageMeasurementResolver stageMeasurementResolver;

  /** Configura as fontes canônicas usadas para resolver a cadeia e os subprocessos. */
  public ProductValueChainPositionService(
      ProductRepository productRepository,
      BusinessProcessChainDefinitionRepository chainRepository,
      ProductSubprocessPositionResolver subprocessResolver,
      PdeProcessCodeResolver processCodeResolver,
      ProductStageMeasurementResolver stageMeasurementResolver) {
    this.productRepository = productRepository;
    this.chainRepository = chainRepository;
    this.subprocessResolver = subprocessResolver;
    this.processCodeResolver = processCodeResolver;
    this.stageMeasurementResolver = stageMeasurementResolver;
  }

  /** Lista a posição de todos os produtos usando somente a versão publicada da cadeia PDE. */
  @Transactional(readOnly = true)
  public List<ProductValueChainPositionResponse> listPositions() {
    List<Product> products = productRepository.findAll();
    return chainRepository
        .findAllByChainCodeAndStatusOrderByVersionNumberDesc(PDE_CHAIN_CODE, PUBLISHED_STATUS)
        .stream()
        .findFirst()
        .map(chain -> positionsInChain(products, chain))
        .orElseGet(() -> products.stream().map(this::chainUnavailablePosition).toList());
  }

  /** Retorna o histórico e a posição de um único produto na cadeia PDE publicada. */
  @Transactional(readOnly = true)
  public ProductValueChainPositionResponse getPosition(Long productId) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado: " + productId));
    return chainRepository
        .findAllByChainCodeAndStatusOrderByVersionNumberDesc(PDE_CHAIN_CODE, PUBLISHED_STATUS)
        .stream()
        .findFirst()
        .map(chain -> positionsInChain(List.of(product), chain).getFirst())
        .orElseGet(() -> chainUnavailablePosition(product));
  }

  /** Resolve todos os produtos contra os processos ordenados da cadeia vigente. */
  private List<ProductValueChainPositionResponse> positionsInChain(
      List<Product> products, BusinessProcessChainDefinition chain) {
    List<BusinessProcessChainItem> orderedItems =
        chain.getItems().stream()
            .sorted(Comparator.comparing(BusinessProcessChainItem::getSequenceNumber))
            .toList();
    Map<String, BusinessProcessChainItem> itemByCode =
        orderedItems.stream()
            .collect(
                Collectors.toMap(
                    item -> item.getProcessDefinition().getProcessCode(), Function.identity()));
    return products.stream()
        .map(product -> resolvePosition(product, chain, orderedItems, itemByCode))
        .toList();
  }

  /** Resolve um produto por código canônico ou por um status legado conhecido. */
  private ProductValueChainPositionResponse resolvePosition(
      Product product,
      BusinessProcessChainDefinition chain,
      List<BusinessProcessChainItem> orderedItems,
      Map<String, BusinessProcessChainItem> itemByCode) {
    String processCode = resolveProcessCode(product.getCommercialStatus(), itemByCode);
    BusinessProcessChainItem item = processCode == null ? null : itemByCode.get(processCode);
    if (item == null) {
      return unresolvedPosition(product, chain, orderedItems.size());
    }
    var process = item.getProcessDefinition();
    return new ProductValueChainPositionResponse(
        product.getId(),
        product.getCommercialStatus(),
        "IDENTIFIED",
        "Posição identificada na cadeia de valor vigente.",
        chain.getId(),
        chain.getName(),
        chain.getVersionNumber(),
        process.getId(),
        process.getProcessCode(),
        process.getName(),
        process.getVersionNumber(),
        item.getSequenceNumber(),
        orderedItems.size(),
        stageMeasurementResolver.resolveProcessMeasurements(product, orderedItems, process),
        subprocessResolver.resolve(product, process));
  }

  /**
   * Converte o status comercial em código de processo, aceitando também o código canônico direto.
   */
  private String resolveProcessCode(
      String commercialStatus, Map<String, BusinessProcessChainItem> itemByCode) {
    return processCodeResolver.resolve(commercialStatus, itemByCode.keySet());
  }

  /** Expõe explicitamente quando o status do produto ainda não possui vínculo canônico. */
  private ProductValueChainPositionResponse unresolvedPosition(
      Product product, BusinessProcessChainDefinition chain, int processCount) {
    String message =
        product.getCommercialStatus() == null || product.getCommercialStatus().isBlank()
            ? "Status comercial não informado; processo atual ainda não identificado."
            : "Status comercial sem vínculo com um processo da cadeia vigente.";
    return new ProductValueChainPositionResponse(
        product.getId(),
        product.getCommercialStatus(),
        "NOT_IDENTIFIED",
        message,
        chain.getId(),
        chain.getName(),
        chain.getVersionNumber(),
        null,
        null,
        null,
        null,
        null,
        processCount,
        List.of(),
        null);
  }

  /** Expõe explicitamente a indisponibilidade da cadeia em vez de fabricar uma posição. */
  private ProductValueChainPositionResponse chainUnavailablePosition(Product product) {
    return new ProductValueChainPositionResponse(
        product.getId(),
        product.getCommercialStatus(),
        "CHAIN_UNAVAILABLE",
        "Cadeia de valor PDE publicada não encontrada.",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(),
        null);
  }
}
