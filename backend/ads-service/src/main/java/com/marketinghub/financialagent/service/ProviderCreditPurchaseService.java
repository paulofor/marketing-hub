package com.marketinghub.financialagent.service;

import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.financialagent.StudioProviderCreditPurchase;
import com.marketinghub.financialagent.service.listVideoProviderCreditBalances.VideoProviderCreditBalanceResponse;
import com.marketinghub.financialagent.service.listVideoProviderCreditBalances.VideoProviderSceneRequestResponse;
import com.marketinghub.financialagent.service.registerProviderCreditPurchase.ProviderCreditPurchaseResponse;
import com.marketinghub.financialagent.service.registerProviderCreditPurchase.RegisterProviderCreditPurchaseRequest;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.financialagent.StudioProviderCreditPurchaseRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobEventRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProviderAccountRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobEvent;
import com.marketinghub.salesvideo.VideoProviderAccount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: gerenciar créditos de provedores sem misturar recarga e consumo de render. */
@Service
public class ProviderCreditPurchaseService {
  private static final BigDecimal RUNWAY_USD_PER_CREDIT = new BigDecimal("0.01");
  private static final int RUNWAY_REFERENCE_CLIP_SECONDS = 10;
  private static final int RUNWAY_REFERENCE_CLIP_CREDITS = 50;
  private static final Pattern SCENE_PATTERN =
      Pattern.compile(
          "(?:processando|aceitou|liquidou) cena (\\d+)/(\\d+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern TASK_PATTERN =
      Pattern.compile("taskId=([^;\\s]+)", Pattern.CASE_INSENSITIVE);
  private final StudioProviderCreditPurchaseRepository repository;
  private final StudioCostLedgerEntryRepository ledgerRepository;
  private final SalesVideoJobRepository videoJobRepository;
  private final SalesVideoJobEventRepository videoJobEventRepository;
  private final VideoProviderAccountRepository providerAccountRepository;

  /** Inicializa o serviço com o repositório canônico das recargas. */
  ProviderCreditPurchaseService(StudioProviderCreditPurchaseRepository repository) {
    this(repository, null, null, null, null);
  }

  /** Inicializa o serviço com recargas, consumos e recusas operacionais. */
  @Autowired
  public ProviderCreditPurchaseService(
      StudioProviderCreditPurchaseRepository repository,
      StudioCostLedgerEntryRepository ledgerRepository,
      SalesVideoJobRepository videoJobRepository,
      SalesVideoJobEventRepository videoJobEventRepository,
      VideoProviderAccountRepository providerAccountRepository) {
    this.repository = repository;
    this.ledgerRepository = ledgerRepository;
    this.videoJobRepository = videoJobRepository;
    this.videoJobEventRepository = videoJobEventRepository;
    this.providerAccountRepository = providerAccountRepository;
  }

  /** Registra uma recarga idempotente com os dados comprovados pelo usuário. */
  @Transactional
  public ProviderCreditPurchaseResponse register(
      String provider, RegisterProviderCreditPurchaseRequest request) {
    String normalizedProvider = normalizeProvider(provider);
    String normalizedCurrency = request.currency().trim().toUpperCase(Locale.ROOT);
    StudioProviderCreditPurchase purchase =
        repository
            .findByProviderAndPurchasedAtAndAmountAndCurrencyAndCreditsPurchased(
                normalizedProvider,
                request.purchasedAt(),
                request.amount(),
                normalizedCurrency,
                request.creditsPurchased())
            .orElseGet(StudioProviderCreditPurchase::new);
    purchase.setProvider(normalizedProvider);
    purchase.setPurchasedAt(request.purchasedAt());
    purchase.setAmount(request.amount());
    purchase.setCurrency(normalizedCurrency);
    purchase.setCreditsPurchased(request.creditsPurchased());
    purchase.setEvidenceReference(normalizeEvidence(request.evidenceReference()));
    return toResponse(repository.save(purchase));
  }

  /** Lista o histórico de recargas do provedor selecionado. */
  @Transactional(readOnly = true)
  public List<ProviderCreditPurchaseResponse> list(String provider) {
    return repository.findByProviderFamily(normalizeProvider(provider)).stream()
        .map(this::toResponse)
        .toList();
  }

  /** Consolida o monitor financeiro transversal dos provedores de vídeo. */
  @Transactional(readOnly = true)
  public List<VideoProviderCreditBalanceResponse> listVideoProviderBalances() {
    if (ledgerRepository == null || videoJobRepository == null || videoJobEventRepository == null) {
      throw new IllegalStateException("Fontes do monitor financeiro não configuradas");
    }
    LinkedHashSet<String> providers = new LinkedHashSet<>();
    providers.add("RUNWAY");
    repository.findDistinctProviders().stream()
        .map(this::normalizeProvider)
        .forEach(providers::add);
    return providers.stream().map(this::balanceOf).toList();
  }

  /** Calcula saldo estimado sem apresentar custo desconhecido como crédito consumido zero. */
  private VideoProviderCreditBalanceResponse balanceOf(String provider) {
    List<StudioProviderCreditPurchase> purchases = repository.findByProviderFamily(provider);
    List<StudioCostLedgerEntry> ledger = ledgerRepository.findByProviderFamily(provider);
    List<SalesVideoJob> failures =
        videoJobRepository.findRecentCreditFailures(provider, PageRequest.of(0, 1));
    SalesVideoJob failure = failures.isEmpty() ? null : failures.getFirst();
    long purchased =
        purchases.stream().mapToLong(StudioProviderCreditPurchase::getCreditsPurchased).sum();
    long unknownCosts = ledger.stream().filter(entry -> costForBalance(entry) == null).count();
    BigDecimal knownCost =
        ledger.stream()
            .map(this::costForBalance)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Long consumed = creditsFromKnownCost(provider, knownCost, unknownCosts);
    Long available = consumed == null ? null : Math.max(0L, purchased - consumed);
    Instant lastPurchase = purchases.isEmpty() ? null : purchases.getFirst().getPurchasedAt();
    Instant lastFailure = failure == null ? null : failure.getFinishedAt();
    boolean divergent =
        lastFailure != null && (lastPurchase == null || !lastFailure.isBefore(lastPurchase));
    String status = statusOf(purchased, available, divergent, unknownCosts);
    boolean runway = "RUNWAY".equals(provider);
    List<VideoProviderSceneRequestResponse> sceneRequests =
        runway ? sceneRequests(provider) : List.of();
    VideoProviderAccount account =
        runway && providerAccountRepository != null
            ? providerAccountRepository.findByAccountKey("RUNWAY_PRIMARY").orElse(null)
            : null;
    BigDecimal officialAvailable =
        account == null || account.getOfficialBalanceCredits() == null
            ? null
            : account
                .getOfficialBalanceCredits()
                .subtract(
                    account.getReservedCredits() == null
                        ? BigDecimal.ZERO
                        : account.getReservedCredits())
                .max(BigDecimal.ZERO);
    return new VideoProviderCreditBalanceResponse(
        provider,
        status,
        "ESTIMATED_FROM_PURCHASES_AND_LEDGER",
        purchased,
        consumed,
        available,
        runway ? "Runway Gen-4 Turbo" : null,
        runway ? RUNWAY_REFERENCE_CLIP_SECONDS : null,
        runway ? RUNWAY_REFERENCE_CLIP_CREDITS : null,
        runway && available != null ? available / RUNWAY_REFERENCE_CLIP_CREDITS : null,
        lastPurchase,
        lastFailure,
        failure == null ? null : failure.getId(),
        failure == null ? null : failure.getFailureDetail(),
        knownCost,
        unknownCosts,
        sceneRequests.size(),
        sceneRequests,
        account != null ? account.getRechargeUrl() : runway ? "https://dev.runwayml.com/" : null,
        account == null ? null : account.getAggregatorName(),
        account == null ? null : account.getAccountKey(),
        account == null ? "UNKNOWN" : account.getSnapshotStatus(),
        account == null ? null : account.getOfficialBalanceCredits(),
        account == null ? null : account.getReservedCredits(),
        officialAvailable,
        account == null ? null : account.getMaxMonthlyCreditSpend(),
        account == null ? null : account.getQuotaSnapshotJson(),
        account == null ? null : account.getSnapshotObservedAt(),
        account == null ? null : account.getSnapshotExpiresAt(),
        account == null ? null : account.getSourceUrl());
  }

  /** Deduplica progresso e heartbeat para contar somente cenas realmente aceitas. */
  private List<VideoProviderSceneRequestResponse> sceneRequests(String provider) {
    List<SalesVideoJobEvent> events = new java.util.ArrayList<>();
    events.addAll(videoJobEventRepository.findAcceptedSceneEvents(provider));
    events.addAll(videoJobEventRepository.findExplicitAcceptedSceneEvents(provider));
    events.addAll(videoJobEventRepository.findSettledSceneEvents(provider));
    java.util.LinkedHashMap<String, VideoProviderSceneRequestResponse> unique =
        new java.util.LinkedHashMap<>();
    events.stream()
        .sorted(java.util.Comparator.comparing(SalesVideoJobEvent::getCreatedAt))
        .forEach(event -> parseSceneRequest(event, unique));
    return List.copyOf(unique.values());
  }

  /** Converte a evidência operacional de uma cena no contrato financeiro público. */
  private void parseSceneRequest(
      SalesVideoJobEvent event, java.util.Map<String, VideoProviderSceneRequestResponse> requests) {
    String message = event.getMessage() == null ? "" : event.getMessage();
    Matcher scene = SCENE_PATTERN.matcher(message);
    if (!scene.find()) return;
    int number = Integer.parseInt(scene.group(1));
    int total = Integer.parseInt(scene.group(2));
    Matcher task = TASK_PATTERN.matcher(message);
    String taskId = task.find() ? task.group(1) : null;
    String details = event.getDetailsJson() == null ? "" : event.getDetailsJson();
    String model = readJsonText(details, "model");
    Integer duration = readJsonInteger(details, "durationSeconds");
    Integer credits = readJsonInteger(details, "estimatedCredits");
    BigDecimal cost = readJsonDecimal(details, "estimatedCostUsd");
    Integer billedCredits = readJsonInteger(details, "billedCredits");
    BigDecimal billedCost = readJsonDecimal(details, "billedCostUsd");
    String settlementStatus = readJsonText(details, "settlementStatus");
    String settlementBasis = readJsonText(details, "settlementBasis");
    String billingEvidence = readJsonText(details, "billingEvidence");
    Long jobId = event.getJob().getId();
    Long cycleId = readCycleId(event.getJob().getMetadataJson());
    String key = jobId + ":" + number;
    VideoProviderSceneRequestResponse previous = requests.get(key);
    if (previous == null
        || billedCredits != null
        || (previous.providerTaskId() == null && taskId != null)) {
      requests.put(
          key,
          new VideoProviderSceneRequestResponse(
              jobId,
              cycleId,
              number,
              total,
              taskId,
              model,
              duration,
              credits != null ? credits : previous == null ? null : previous.estimatedCredits(),
              cost != null ? cost : previous == null ? null : previous.estimatedCostUsd(),
              billedCredits != null
                  ? billedCredits
                  : previous == null ? null : previous.billedCredits(),
              billedCost != null ? billedCost : previous == null ? null : previous.billedCostUsd(),
              settlementStatus != null
                  ? settlementStatus
                  : previous == null ? null : previous.settlementStatus(),
              settlementBasis != null
                  ? settlementBasis
                  : previous == null ? null : previous.settlementBasis(),
              billingEvidence != null
                  ? billingEvidence
                  : previous == null ? null : previous.billingEvidence(),
              previous == null || (previous.providerTaskId() == null && taskId != null)
                  ? event.getCreatedAt()
                  : previous.acceptedAt()));
    }
  }

  /** Lê texto simples do JSON financeiro sem transformar falha de exibição em falha do monitor. */
  private String readJsonText(String json, String field) {
    Matcher matcher =
        Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
    return matcher.find() ? matcher.group(1) : null;
  }

  /** Lê inteiro simples do JSON financeiro preservando eventos legados sem o campo. */
  private Integer readJsonInteger(String json, String field) {
    Matcher matcher = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*(\\d+)").matcher(json);
    return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
  }

  /** Lê custo decimal simples do JSON financeiro preservando eventos legados. */
  private BigDecimal readJsonDecimal(String json, String field) {
    Matcher matcher =
        Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(json);
    return matcher.find() ? new BigDecimal(matcher.group(1)) : null;
  }

  /** Lê o ciclo de produção sem falhar o monitor quando metadados legados forem inválidos. */
  private Long readCycleId(String metadataJson) {
    if (metadataJson == null || metadataJson.isBlank()) return null;
    Matcher matcher =
        Pattern.compile("\\\"videoProductionCycleId\\\"\\s*:\\s*(\\d+)").matcher(metadataJson);
    return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
  }

  /** Converte custo conhecido em créditos somente quando há contrato de conversão comprovado. */
  private Long creditsFromKnownCost(String provider, BigDecimal knownCost, long unknownCosts) {
    if (!"RUNWAY".equals(provider) || unknownCosts > 0) {
      return null;
    }
    return knownCost.divide(RUNWAY_USD_PER_CREDIT, 0, RoundingMode.CEILING).longValue();
  }

  /**
   * Usa estimativa somente para execução concluída; tentativa recusada não vira consumo inventado.
   */
  private BigDecimal costForBalance(StudioCostLedgerEntry entry) {
    if ("PROVIDER_TASKS_PARTIALLY_SETTLED".equals(entry.getCostEvidence())) {
      return null;
    }
    if (entry.getProviderCostUsd() != null) {
      return entry.getProviderCostUsd();
    }
    String status = entry.getStatus() == null ? "" : entry.getStatus().toUpperCase(Locale.ROOT);
    boolean completed =
        status.contains("READY") || status.contains("COMPLETED") || status.contains("SUCCEEDED");
    return completed ? entry.getEstimatedCostUsd() : null;
  }

  /** Classifica a capacidade sem confundir estimativa, divergência e ausência de fonte. */
  private String statusOf(long purchased, Long available, boolean divergent, long unknownCosts) {
    if (divergent) return "DIVERGENT_PROVIDER_REJECTION";
    if (purchased == 0) return "NO_PURCHASE_RECORDED";
    if (available == null || unknownCosts > 0) return "UNKNOWN_CONSUMPTION";
    if (available < RUNWAY_REFERENCE_CLIP_CREDITS) return "INSUFFICIENT";
    if (available < RUNWAY_REFERENCE_CLIP_CREDITS * 3L) return "LOW";
    return "AVAILABLE";
  }

  /** Normaliza a identidade do provedor para impedir históricos paralelos. */
  private String normalizeProvider(String provider) {
    if (provider == null || provider.isBlank()) {
      throw new IllegalArgumentException("Provedor é obrigatório");
    }
    String normalized = provider.trim().toUpperCase(Locale.ROOT);
    return normalized.startsWith("RUNWAY") ? "RUNWAY" : normalized;
  }

  /** Converte referência vazia em ausência explícita. */
  private String normalizeEvidence(String evidenceReference) {
    return evidenceReference == null || evidenceReference.isBlank()
        ? null
        : evidenceReference.trim();
  }

  /** Converte a entidade persistida no contrato público do módulo. */
  private ProviderCreditPurchaseResponse toResponse(StudioProviderCreditPurchase purchase) {
    return new ProviderCreditPurchaseResponse(
        purchase.getId(),
        purchase.getProvider(),
        purchase.getPurchasedAt(),
        purchase.getAmount(),
        purchase.getCurrency(),
        purchase.getCreditsPurchased(),
        purchase.getEvidenceReference(),
        purchase.getCreatedAt());
  }
}
