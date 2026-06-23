package com.marketinghub.repository.jpa.oprm.cnae;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Repository;

/** Centraliza a materialização de nichos confirmados pelo OPRM usando o repositório canônico de nichos. */
@Repository
public class OprmConfirmedMarketNicheRepository {
    private final MarketNicheRepository marketNicheRepository;

    /** Inicializa o adaptador OPRM com o repositório canônico de nichos de mercado. */
    public OprmConfirmedMarketNicheRepository(MarketNicheRepository marketNicheRepository) {
        this.marketNicheRepository = marketNicheRepository;
    }

    /** Verifica se já existe nicho com o mesmo nome para preservar unicidade comercial. */
    public boolean existsByNameIgnoreCase(String name) {
        return marketNicheRepository.existsByNameIgnoreCase(name);
    }

    /** Cria o nicho de mercado confirmado e retorna apenas o contrato permitido ao módulo OPRM. */
    public OprmConfirmedMarketNiche createConfirmedNiche(String name, String description, BigDecimal cost) {
        MarketNiche marketNiche = new MarketNiche();
        marketNiche.setName(name);
        marketNiche.setDescription(description);
        marketNiche.setTotalCost(cost);
        marketNiche.setCost(cost);
        MarketNiche saved = marketNicheRepository.save(marketNiche);
        return new OprmConfirmedMarketNiche(saved.getId(), saved.getName(), saved.getCost());
    }
}
