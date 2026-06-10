package com.marketinghub.repository.jpa.oprm.generalaudience;

import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

/** Repositório responsável por materializar um MarketNiche a partir do fluxo OPRM de público geral. */
@Repository
public class OprmGeneralAudienceMarketNicheMaterializationRepository {

    private final EntityManager entityManager;

    /** Inicializa o repositório com o EntityManager canônico de persistência JPA. */
    public OprmGeneralAudienceMarketNicheMaterializationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Cria ou atualiza o MarketNiche que representa o subnicho geral aprovado. */
    public OprmGeneralAudienceMaterializedMarketNiche saveMarketNiche(
            Long existingMarketNicheId,
            String name,
            String description,
            String baseSegmentation,
            String interests,
            String demographicFilters,
            String extraTips) {
        MarketNiche marketNiche = resolveMarketNiche(existingMarketNicheId);
        boolean reusedExisting = marketNiche.getId() != null;
        marketNiche.setName(name);
        marketNiche.setDescription(description);
        marketNiche.setBaseSegmentation(baseSegmentation);
        marketNiche.setInterests(interests);
        marketNiche.setDemographicFilters(demographicFilters);
        marketNiche.setExtraTips(extraTips);
        MarketNiche savedMarketNiche;
        if (marketNiche.getId() == null) {
            entityManager.persist(marketNiche);
            savedMarketNiche = marketNiche;
        } else {
            savedMarketNiche = entityManager.merge(marketNiche);
        }
        return new OprmGeneralAudienceMaterializedMarketNiche(
                savedMarketNiche.getId(),
                savedMarketNiche.getName(),
                reusedExisting);
    }

    /** Busca o MarketNiche existente quando o subnicho já possui vínculo materializado. */
    private MarketNiche resolveMarketNiche(Long existingMarketNicheId) {
        if (existingMarketNicheId == null) {
            return new MarketNiche();
        }
        MarketNiche marketNiche = entityManager.find(MarketNiche.class, existingMarketNicheId);
        return marketNiche == null ? new MarketNiche() : marketNiche;
    }
}
