package com.marketinghub.pde.service.versionoverview;

import com.marketinghub.pde.PdeProductionSlotStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Consolida a visão de negócio e a trajetória de uma versão PDE do produto Opala. */
public record ProductPdeVersionOverviewDto(
    Long id,
    String slotCode,
    String name,
    String experienceVersion,
    String lifecycleStage,
    String lifecycleLabel,
    PdeProductionSlotStatus operationalStatus,
    String hypothesis,
    String primaryChange,
    Long sourceExperimentId,
    String sourceExperimentName,
    String sourceExperimentStatus,
    int videoCount,
    int approvedVideoCount,
    BigDecimal priceBrl,
    String primaryCta,
    String checkoutUrl,
    String publicUrl,
    String validationStatus,
    String validationSummary,
    Instant validationCheckedAt,
    String homologationSummary,
    boolean publishedContract,
    List<String> pendingItems,
    List<PdeVersionLifecycleStepDto> lifecycle,
    Instant updatedAt) {}
