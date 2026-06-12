package com.marketinghub.nichocnae.nicheresearchseedbuilder;

import java.math.BigDecimal;
import java.time.Instant;

/** Representa um ciclo em execução pronto para a etapa dois gerar seed e queries de pesquisa do nicho. */
public record NicheResearchSeedBuilderPending(
        Long researchCycleId,
        Long sourceNicheId,
        String cnaeCode,
        String cnaeDescription,
        String nicheName,
        BigDecimal sourceScore,
        String openAiModelCode,
        String openAiModelName,
        String triggerSource,
        String status,
        Instant startedAt,
        Instant createdAt) {}
