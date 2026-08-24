package com.marketinghub.product.service.valuechainposition;

import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: localizar produtos na cadeia de valor PDE vigente sem inferência no frontend.
 */
@Service
@RequiredArgsConstructor
public class ProductValueChainPositionService {
  private static final String PDE_CHAIN_CODE = "pde-value-creation-delivery";
  private static final String PUBLISHED_STATUS = "PUBLISHED";
  private static final Map<String, String> LEGACY_STATUS_PROCESS_CODES =
      Map.ofEntries(
          Map.entry("OPORTUNIDADE_EM_DESCOBERTA", "pde-opportunity-discovery"),
          Map.entry("DESCOBERTA_E_PRIORIZACAO", "pde-opportunity-discovery"),
          Map.entry("IDEIA_PRIORIZADA_PARA_TESTE", "pde-commercial-plan-offer"),
          Map.entry("PLANO_COMERCIAL", "pde-commercial-plan-offer"),
          Map.entry("PLANO_COMERCIAL_E_OFERTA", "pde-commercial-plan-offer"),
          Map.entry("CONSTRUCAO_E_APROVACAO", "pde-construction-approval"),
          Map.entry("COMUNICACAO_E_JORNADA", "pde-communication-sales-journey"),
          Map.entry("COMUNICACAO_E_JORNADA_DE_VENDA", "pde-communication-sales-journey"),
          Map.entry("VALIDACAO_COMERCIAL", "pde-commercial-homologation-activation"),
          Map.entry("HOMOLOGACAO_E_ATIVACAO", "pde-commercial-homologation-activation"),
          Map.entry("HOMOLOGACAO_COMERCIAL_E_ATIVACAO", "pde-commercial-homologation-activation"),
          Map.entry("ATIVO", "pde-sales-delivery-learning"),
          Map.entry("ACTIVE", "pde-sales-delivery-learning"),
          Map.entry("RUNNING", "pde-sales-delivery-learning"),
          Map.entry("ESCALA", "pde-sales-delivery-learning"),
          Map.entry("ESCALANDO", "pde-sales-delivery-learning"),
          Map.entry("VENDA_ENTREGA_E_APRENDIZADO", "pde-sales-delivery-learning"));

  private final ProductRepository productRepository;
  private final BusinessProcessChainDefinitionRepository chainRepository;
  private final ProductSubprocessPositionResolver subprocessResolver;

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
        .map(product -> resolvePosition(product, chain, orderedItems.size(), itemByCode))
        .toList();
  }

  /** Resolve um produto por código canônico ou por um status legado conhecido. */
  private ProductValueChainPositionResponse resolvePosition(
      Product product,
      BusinessProcessChainDefinition chain,
      int processCount,
      Map<String, BusinessProcessChainItem> itemByCode) {
    String processCode = resolveProcessCode(product.getCommercialStatus(), itemByCode);
    BusinessProcessChainItem item = processCode == null ? null : itemByCode.get(processCode);
    if (item == null) {
      return unresolvedPosition(product, chain, processCount);
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
        processCount,
        subprocessResolver.resolve(product, process));
  }

  /**
   * Converte o status comercial em código de processo, aceitando também o código canônico direto.
   */
  private String resolveProcessCode(
      String commercialStatus, Map<String, BusinessProcessChainItem> itemByCode) {
    String normalizedStatus = normalize(commercialStatus);
    if (normalizedStatus == null) {
      return null;
    }
    return itemByCode.keySet().stream()
        .filter(processCode -> normalize(processCode).equals(normalizedStatus))
        .findFirst()
        .orElse(LEGACY_STATUS_PROCESS_CODES.get(normalizedStatus));
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
        null);
  }

  /** Normaliza códigos antigos, acentos e separadores antes da resolução do vínculo. */
  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .trim()
        .toUpperCase(Locale.ROOT)
        .replaceAll("[^A-Z0-9]+", "_")
        .replaceAll("^_+|_+$", "");
  }
}
