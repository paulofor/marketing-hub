package com.marketinghub.niche.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.dto.CreateMarketNicheRequest;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for market niches.
 */
@Service
public class MarketNicheService {
    private final MarketNicheRepository repository;

    public MarketNicheService(MarketNicheRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates and stores a market niche.
     */
    @Transactional
    public MarketNiche create(CreateMarketNicheRequest request) {
        MarketNiche niche = MarketNiche.builder()
                .name(request.getName())
                .description(request.getDescription())
                .demandVolume(request.getDemandVolume())
                .promises(request.getPromises())
                .offers(request.getOffers())
                .build();
        return repository.save(niche);
    }

    public MarketNiche get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<MarketNiche> list() {
        return repository.findAll();
    }
}
