package com.marketinghub.hypothesis.pain.provisorio;

import com.marketinghub.niche.MarketNicheEnrichmentProfile;
import com.marketinghub.repository.jpa.niche.MarketNicheEnrichmentProfileRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Responsabilidade: isolar a leitura provisória do perfil enriquecido usado como contexto da hipótese de dor. */
@Component
public class HypothesisPainEnrichmentProfileReader {
    private final MarketNicheEnrichmentProfileRepository enrichmentProfileRepository;

    /** Inicializa o leitor com o repositório de perfis enriquecidos do nicho. */
    public HypothesisPainEnrichmentProfileReader(MarketNicheEnrichmentProfileRepository enrichmentProfileRepository) {
        this.enrichmentProfileRepository = enrichmentProfileRepository;
    }

    /** Busca o perfil enriquecido mais recente do nicho e devolve um snapshot desacoplado. */
    public Optional<HypothesisPainEnrichmentProfileSnapshot> findLatestByMarketNicheId(Long marketNicheId) {
        if (marketNicheId == null) {
            return Optional.empty();
        }
        return enrichmentProfileRepository.findLatestByMarketNicheIds(List.of(marketNicheId)).stream()
                .findFirst()
                .map(this::toSnapshot);
    }

    /** Converte a entidade persistida em snapshot de leitura para o pipeline de hipótese. */
    private HypothesisPainEnrichmentProfileSnapshot toSnapshot(MarketNicheEnrichmentProfile profile) {
        return new HypothesisPainEnrichmentProfileSnapshot(
                profile.getId(),
                profile.getResearchCycleId(),
                profile.getCnaeCode(),
                profile.getCnaeDescription(),
                profile.getSourceScore(),
                profile.getQualityStatus(),
                profile.getSpecificityScore(),
                profile.getConfidenceScore(),
                profile.getDuplicationScore(),
                profile.getRoutineEvidenceScore(),
                profile.getDifficultyEvidenceScore(),
                profile.getSourceDiversityScore(),
                profile.getSolutionLanguageRiskScore(),
                profile.getRoutineSummary(),
                profile.getPersonaDailyTasks(),
                profile.getEvidenceSummary(),
                profile.getSourceDomains(),
                profile.getPersonaSummary(),
                profile.getLanguagePatterns(),
                profile.getCommercialTriggers(),
                profile.getObjections());
    }
}
