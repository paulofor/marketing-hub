package com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.gateway;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Porta exclusiva da etapa persona-routine-materializer v3 para materializar dados reutilizáveis de nicho. */
public interface PersonaRoutineMaterializerNicheGateway {
    /** Busca materialização existente pelo CNAE e nome neutro normalizado. */
    Optional<MarketNicheSnapshot> findPersonaRoutineMaterializedNiche(String cnaeCode, String normalizedNeutralNicheName);

    /** Materializa ou atualiza o nicho e grava o perfil enriquecido produzido pela etapa v3. */
    NicheMaterializationResult materialize(MarketNicheDraft nicheDraft, EnrichedNicheProfileDraft profileDraft);

    /** Representa a referência mínima de nicho já materializado para a etapa v3. */
    record MarketNicheSnapshot(Long marketNicheId) {}

    /** Representa os campos funcionais do nicho que a etapa v3 pode solicitar por contrato próprio. */
    record MarketNicheDraft(
            Long marketNicheId,
            String name,
            String description,
            String sourceCnaeCode,
            String sourceCnaeDescription,
            String demandVolume,
            String baseSegmentation,
            String demographicFilters,
            List<String> interestList,
            List<String> roleList,
            List<String> behaviorList,
            String interests,
            String extraTips,
            BigDecimal identificationCostBrl) {}

    /** Representa os campos funcionais do perfil enriquecido produzidos pela etapa v3. */
    record EnrichedNicheProfileDraft(
            Long sourceNicheCandidateId,
            Long researchCycleId,
            Long sourceRoutineCardId,
            String cnaeCode,
            String cnaeDescription,
            BigDecimal sourceScore,
            String qualityStatus,
            Integer specificityScore,
            Integer confidenceScore,
            Integer duplicationScore,
            Integer routineEvidenceScore,
            Integer difficultyEvidenceScore,
            Integer sourceDiversityScore,
            Integer solutionLanguageRiskScore,
            String originalNicheName,
            String neutralNicheName,
            String researchMode,
            String routineSummary,
            String personaDailyTasks,
            String evidenceSummary,
            String sourceDomains,
            String personaSummary,
            String languagePatterns,
            String commercialTriggers,
            String objections,
            String researchReportMarkdown,
            String createdBy,
            Instant createdAt,
            Instant updatedAt) {}

    /** Representa o resultado da materialização da etapa v3. */
    record NicheMaterializationResult(Long marketNicheId, Long enrichmentProfileId, Instant createdAt) {}
}
