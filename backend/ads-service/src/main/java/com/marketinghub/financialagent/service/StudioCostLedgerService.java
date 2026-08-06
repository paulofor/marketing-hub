package com.marketinghub.financialagent.service;

import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
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
