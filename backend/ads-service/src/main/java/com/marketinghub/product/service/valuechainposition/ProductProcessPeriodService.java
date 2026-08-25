package com.marketinghub.product.service.valuechainposition;

import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.product.Product;
import com.marketinghub.product.ProductProcessPeriod;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.product.ProductProcessPeriodRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: registrar entradas e saídas dos produtos nos macroprocessos PDE. */
@Service
public class ProductProcessPeriodService {
  private static final String PDE_CHAIN_CODE = "pde-value-creation-delivery";
  private static final String PUBLISHED_STATUS = "PUBLISHED";

  private final BusinessProcessChainDefinitionRepository chainRepository;
  private final ProductProcessPeriodRepository periodRepository;
  private final PdeProcessCodeResolver processCodeResolver;
  private final Clock clock;

  /** Configura catálogo, persistência e relógio usados pelo rastreamento. */
  @Autowired
  public ProductProcessPeriodService(
      BusinessProcessChainDefinitionRepository chainRepository,
      ProductProcessPeriodRepository periodRepository,
      PdeProcessCodeResolver processCodeResolver) {
    this(chainRepository, periodRepository, processCodeResolver, Clock.systemUTC());
  }

  /** Permite testar transições com um relógio determinístico. */
  ProductProcessPeriodService(
      BusinessProcessChainDefinitionRepository chainRepository,
      ProductProcessPeriodRepository periodRepository,
      PdeProcessCodeResolver processCodeResolver,
      Clock clock) {
    this.chainRepository = chainRepository;
    this.periodRepository = periodRepository;
    this.processCodeResolver = processCodeResolver;
    this.clock = clock;
  }

  /** Registra a primeira entrada conhecida de um produto recém-criado. */
  @Transactional
  public void recordInitialPosition(Product product) {
    if (product == null || product.getId() == null) return;
    if (periodRepository
        .findTopByProductIdAndExitedAtIsNullOrderByEnteredAtDescIdDesc(product.getId())
        .isPresent()) return;
    openCurrentPeriod(product, Instant.now(clock), "PRODUCT_CREATED");
  }

  /** Fecha o macroprocesso anterior e abre o novo somente quando o estado realmente muda. */
  @Transactional
  public void recordTransition(Product product, String previousCommercialStatus) {
    if (product == null || product.getId() == null) return;
    BusinessProcessChainDefinition chain = publishedChain();
    if (chain == null) return;
    Set<String> codes = publishedCodes(chain);
    String previousCode = processCodeResolver.resolve(previousCommercialStatus, codes);
    String currentCode = processCodeResolver.resolve(product.getCommercialStatus(), codes);
    if (java.util.Objects.equals(previousCode, currentCode)) return;

    Instant changedAt = Instant.now(clock);
    periodRepository
        .findTopByProductIdAndExitedAtIsNullOrderByEnteredAtDescIdDesc(product.getId())
        .ifPresent(
            period -> {
              period.setExitedAt(changedAt);
              period.setExitEvidence("COMMERCIAL_STATUS_TRANSITION");
              period.setObjectiveAchieved(currentCode != null);
              period.setOpenSlot(null);
              period.setUpdatedAt(changedAt);
              periodRepository.save(period);
            });
    openPeriod(product, chain, currentCode, changedAt, "COMMERCIAL_STATUS_TRANSITION");
  }

  /** Abre a posição atual usando a cadeia publicada quando o estado possui vínculo canônico. */
  private void openCurrentPeriod(Product product, Instant enteredAt, String evidence) {
    BusinessProcessChainDefinition chain = publishedChain();
    if (chain == null) return;
    openPeriod(
        product,
        chain,
        processCodeResolver.resolve(product.getCommercialStatus(), publishedCodes(chain)),
        enteredAt,
        evidence);
  }

  /** Persiste uma permanência aberta para o item canônico informado. */
  private void openPeriod(
      Product product,
      BusinessProcessChainDefinition chain,
      String processCode,
      Instant enteredAt,
      String evidence) {
    if (processCode == null) return;
    BusinessProcessChainItem item =
        chain.getItems().stream()
            .filter(
                candidate -> processCode.equals(candidate.getProcessDefinition().getProcessCode()))
            .findFirst()
            .orElse(null);
    if (item == null) return;
    ProductProcessPeriod period = new ProductProcessPeriod();
    period.setProduct(product);
    period.setProcessDefinition(item.getProcessDefinition());
    period.setProcessCodeSnapshot(item.getProcessDefinition().getProcessCode());
    period.setProcessNameSnapshot(item.getProcessDefinition().getName());
    period.setSequenceNumber(item.getSequenceNumber());
    period.setEnteredAt(enteredAt);
    period.setEntryEvidence(evidence);
    period.setObjectiveAchieved(false);
    period.setOpenSlot(1);
    period.setCreatedAt(enteredAt);
    period.setUpdatedAt(enteredAt);
    periodRepository.save(period);
  }

  /** Localiza a versão publicada mais recente da cadeia PDE. */
  private BusinessProcessChainDefinition publishedChain() {
    return chainRepository
        .findAllByChainCodeAndStatusOrderByVersionNumberDesc(PDE_CHAIN_CODE, PUBLISHED_STATUS)
        .stream()
        .findFirst()
        .orElse(null);
  }

  /** Extrai os códigos publicados disponíveis para a resolução de estados. */
  private Set<String> publishedCodes(BusinessProcessChainDefinition chain) {
    return chain.getItems().stream()
        .sorted(Comparator.comparing(BusinessProcessChainItem::getSequenceNumber))
        .map(item -> item.getProcessDefinition().getProcessCode())
        .collect(Collectors.toSet());
  }
}
