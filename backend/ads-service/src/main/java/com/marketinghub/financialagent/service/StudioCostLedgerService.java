package com.marketinghub.financialagent.service;

import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import com.marketinghub.repository.jpa.financialagent.StudioProviderEfficiencyProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: manter uma entrada idempotente de custo para cada tentativa do Estúdio. */
@Service
public class StudioCostLedgerService {
  private final StudioCostLedgerEntryRepository repository;

  /** Inicializa o serviço com a fonte canônica do ledger. */
  public StudioCostLedgerService(StudioCostLedgerEntryRepository repository) {
    this.repository = repository;
  }

  /** Registra ou atualiza uma tentativa de imagem sem inventar custo não informado. */
  @Transactional
  public void recordImage(
      String jobId,
      Long productId,
      Long commercialPlanId,
      Long experimentId,
      String model,
      String status,
      Instant startedAt,
      Instant finishedAt) {
    StudioCostLedgerEntry entry =
        repository
            .findBySourceTypeAndSourceId("IMAGE_GENERATION_REQUEST", jobId)
            .orElseGet(StudioCostLedgerEntry::new);
    entry.setCommercialPlanId(commercialPlanId);
    entry.setProductId(productId);
    entry.setExperimentId(experimentId);
    entry.setAssetType("IMAGE");
    entry.setSourceType("IMAGE_GENERATION_REQUEST");
    entry.setSourceId(jobId);
    entry.setProvider("OPENAI");
    entry.setModel(model);
    entry.setStatus(status);
    entry.setCurrency("USD");
    entry.setCostEvidence("PROVIDER_COST_NOT_REPORTED");
    entry.setStartedAt(startedAt);
    entry.setFinishedAt(finishedAt);
    repository.save(entry);
  }

  /** Registra qualquer tentativa legada de áudio ou vídeo antes do consumo externo. */
  @Transactional
  public void recordMedia(
      Long assetId,
      Long productId,
      Long commercialPlanId,
      Long experimentId,
      String assetType,
      String provider,
      String model,
      String status,
      BigDecimal costUsd,
      boolean providerReported,
      Instant startedAt,
      Instant finishedAt) {
    StudioCostLedgerEntry entry =
        repository
            .findBySourceTypeAndSourceId("MEDIA_ASSET", String.valueOf(assetId))
            .orElseGet(StudioCostLedgerEntry::new);
    entry.setCommercialPlanId(commercialPlanId);
    entry.setProductId(productId);
    entry.setExperimentId(experimentId);
    entry.setAssetType(assetType);
    entry.setSourceType("MEDIA_ASSET");
    entry.setSourceId(String.valueOf(assetId));
    entry.setProvider(provider == null ? "UNKNOWN" : provider);
    entry.setModel(model);
    entry.setStatus(status);
    entry.setProviderCostUsd(providerReported ? costUsd : null);
    entry.setEstimatedCostUsd(providerReported ? null : costUsd);
    entry.setCurrency("USD");
    entry.setCostEvidence(
        costUsd == null
            ? "PROVIDER_COST_NOT_REPORTED"
            : providerReported ? "PROVIDER_REPORTED" : "PROVIDER_RATE_CARD_ESTIMATE");
    entry.setStartedAt(startedAt);
    entry.setFinishedAt(finishedAt);
    repository.save(entry);
  }

  /** Atualiza o estado de mídia legada usando a atribuição já preservada no ledger. */
  @Transactional
  public void updateMediaStatus(Long assetId, String status, Instant finishedAt) {
    repository
        .findBySourceTypeAndSourceId("MEDIA_ASSET", String.valueOf(assetId))
        .ifPresent(
            entry -> {
              entry.setStatus(status);
              entry.setFinishedAt(finishedAt);
              repository.save(entry);
            });
  }

  /** Registra ou atualiza uma tentativa de vídeo atribuída ao projeto comercial do Estúdio. */
  @Transactional
  public void recordVideo(
      Long jobId,
      Long productId,
      Long commercialPlanId,
      Long experimentId,
      String assetType,
      String provider,
      String model,
      String status,
      BigDecimal costUsd,
      boolean providerReported,
      Instant startedAt,
      Instant finishedAt) {
    StudioCostLedgerEntry entry =
        repository
            .findBySourceTypeAndSourceId("SALES_VIDEO_JOB", String.valueOf(jobId))
            .orElseGet(StudioCostLedgerEntry::new);
    entry.setCommercialPlanId(commercialPlanId);
    entry.setProductId(productId);
    entry.setExperimentId(experimentId);
    entry.setAssetType(assetType);
    entry.setSourceType("SALES_VIDEO_JOB");
    entry.setSourceId(String.valueOf(jobId));
    entry.setProvider(provider == null ? "UNKNOWN" : provider);
    entry.setModel(model);
    entry.setStatus(status);
    entry.setProviderCostUsd(providerReported ? costUsd : null);
    entry.setEstimatedCostUsd(providerReported ? null : costUsd);
    entry.setCurrency("USD");
    entry.setCostEvidence(
        costUsd == null
            ? "PROVIDER_COST_NOT_REPORTED"
            : providerReported ? "PROVIDER_REPORTED" : "PROVIDER_RATE_CARD_ESTIMATE");
    entry.setStartedAt(startedAt);
    entry.setFinishedAt(finishedAt);
    repository.save(entry);
  }

  /** Soma o custo conhecido do Estúdio no plano, sem converter moeda implicitamente. */
  @Transactional(readOnly = true)
  public BigDecimal totalKnownCostUsd(Long planId) {
    return repository.totalCostUsdByPlanId(planId);
  }

  /** Mede a cobertura sem confundir ausencia de tentativas com custo comprovadamente zero. */
  @Transactional(readOnly = true)
  public Map<String, Object> coverage(Long planId) {
    return coverageOf(repository.findByCommercialPlanIdOrderByCreatedAtAsc(planId));
  }

  /** Soma custos conhecidos ainda sem plano para impedir que consumo real fique invisível. */
  @Transactional(readOnly = true)
  public BigDecimal totalUnassignedCostUsd() {
    return repository.totalUnassignedCostUsd();
  }

  /** Mede a cobertura das tentativas que ainda exigem atribuição comercial. */
  @Transactional(readOnly = true)
  public Map<String, Object> unassignedCoverage() {
    return coverageOf(repository.findByCommercialPlanIdIsNullOrderByCreatedAtAsc());
  }

  /** Consolida eficiencia financeira e aprovacao comercial por provedor do plano. */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> providerEfficiency(Long planId) {
    return repository.providerEfficiencyByPlanId(planId).stream()
        .map(this::providerEfficiencyOf)
        .toList();
  }

  /** Calcula indicadores sem inventar custo ou taxa quando falta denominador confiavel. */
  private Map<String, Object> providerEfficiencyOf(StudioProviderEfficiencyProjection row) {
    long attempts = value(row.getTotalAttempts());
    long knownCostAttempts = value(row.getKnownCostAttempts());
    long reviewedAssets = value(row.getReviewedAssets());
    long approvedAssets = value(row.getApprovedAssets());
    BigDecimal knownCost = row.getKnownCostUsd() == null ? BigDecimal.ZERO : row.getKnownCostUsd();
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    result.put("provider", row.getProvider());
    result.put("totalAttempts", attempts);
    result.put("knownCostAttempts", knownCostAttempts);
    result.put("unknownCostAttempts", attempts - knownCostAttempts);
    result.put("knownCostUsd", knownCost);
    result.put("reviewedAssets", reviewedAssets);
    result.put("approvedAssets", approvedAssets);
    result.put("pendingReviewAssets", value(row.getPendingReviewAssets()));
    result.put(
        "commercialApprovalRatePercent",
        reviewedAssets == 0
            ? null
            : BigDecimal.valueOf(approvedAssets)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(reviewedAssets), 2, RoundingMode.HALF_UP));
    result.put(
        "knownCostPerApprovedAssetUsd",
        approvedAssets == 0
            ? null
            : knownCost.divide(BigDecimal.valueOf(approvedAssets), 6, RoundingMode.HALF_UP));
    result.put(
        "decisionCoverage",
        attempts == 0 || knownCostAttempts < attempts
            ? "INCOMPLETE_COSTS"
            : reviewedAssets == 0 ? "NO_COMMERCIAL_REVIEWS" : "READY_FOR_COMPARISON");
    return result;
  }

  /** Converte agregados nulos do banco em contagens vazias. */
  private long value(Long value) {
    return value == null ? 0L : value;
  }

  /** Consolida cobertura para uma coleção de entradas do ledger. */
  private Map<String, Object> coverageOf(java.util.List<StudioCostLedgerEntry> entries) {
    long known =
        entries.stream()
            .filter(e -> e.getProviderCostUsd() != null || e.getEstimatedCostUsd() != null)
            .count();
    long images = entries.stream().filter(e -> "IMAGE".equals(e.getAssetType())).count();
    long videos = entries.stream().filter(e -> "VIDEO".equals(e.getAssetType())).count();
    long audios = entries.stream().filter(e -> "AUDIO".equals(e.getAssetType())).count();
    String status =
        entries.isEmpty()
            ? "NO_ATTEMPTS_RECORDED"
            : known == entries.size() ? "COMPLETE" : "PARTIAL";
    LinkedHashMap<String, Object> coverage = new LinkedHashMap<>();
    coverage.put("status", status);
    coverage.put("knownCostAttempts", known);
    coverage.put("totalAttempts", entries.size());
    coverage.put("unknownCostAttempts", entries.size() - known);
    coverage.put("imageAttempts", images);
    coverage.put("videoAttempts", videos);
    coverage.put("audioAttempts", audios);
    return coverage;
  }
}
