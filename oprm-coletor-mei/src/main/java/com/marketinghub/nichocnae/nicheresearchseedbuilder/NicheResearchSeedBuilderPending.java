package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Representa um ciclo em execução pronto para a etapa dois gerar seed e queries de pesquisa do nicho. */
public record NicheResearchSeedBuilderPending(
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        String cnaeDescription,
        String nicheName,
        BigDecimal sourceScore,
        Long meiVolume,
        String openAiModelCode,
        String openAiModelName,
        String triggerSource,
        String previousQualityStatus,
        String previousNextMoveCode,
        String previousNextMove,
        String previousLearningNotes,
        List<String> existingSubnichesForCnae,
        String status,
        Instant startedAt,
        Instant createdAt) {}
