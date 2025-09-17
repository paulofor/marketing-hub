package com.marketinghub.audience.service;

import com.marketinghub.audience.Audience;
import com.marketinghub.audience.dto.CreateAudienceRequest;
import com.marketinghub.audience.dto.UpdateAudienceRequest;
import com.marketinghub.audience.repository.AudienceRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for audiences.
 */
@Service
public class AudienceService {
    private final AudienceRepository repository;
    private final MarketNicheRepository nicheRepository;
    private final HypothesisRepository hypothesisRepository;

    public AudienceService(AudienceRepository repository, MarketNicheRepository nicheRepository, HypothesisRepository hypothesisRepository) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
    }

    /**
     * Creates an audience associated with a niche or hypothesis.
     */
    @Transactional
    public Audience create(CreateAudienceRequest request) {
        com.marketinghub.niche.MarketNiche niche = null;
        if (request.getMarketNicheId() != null) {
            niche = nicheRepository.findById(request.getMarketNicheId()).orElseThrow();
        }
        com.marketinghub.hypothesis.Hypothesis hypothesis = null;
        if (request.getHypothesisId() != null) {
            hypothesis = hypothesisRepository.findById(request.getHypothesisId()).orElseThrow();
        }
        Audience audience = Audience.builder()
                .name(request.getName())
                .description(request.getDescription())
                .prompt(request.getPrompt())
                .model(request.getModel())
                .niche(niche)
                .hypothesis(hypothesis)
                .build();
        return repository.save(audience);
    }

    public Audience get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<Audience> list() {
        return repository.findAll();
    }

    public Iterable<Audience> listByMarketNiche(Long nicheId) {
        return repository.findByNicheId(nicheId);
    }

    /**
     * Updates mutable fields of an audience.
     */
    @Transactional
    public Audience update(Long id, UpdateAudienceRequest request) {
        Audience audience = repository.findById(id).orElseThrow();
        if (request.getApproved() != null) {
            audience.setApproved(request.getApproved());
        }
        return audience;
    }
}
