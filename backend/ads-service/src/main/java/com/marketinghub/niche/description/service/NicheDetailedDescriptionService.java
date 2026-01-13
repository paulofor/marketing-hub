package com.marketinghub.niche.description.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.description.NicheDetailedDescription;
import com.marketinghub.niche.description.dto.CreateNicheDetailedDescriptionRequest;
import com.marketinghub.niche.description.repository.NicheDetailedDescriptionRepository;
import com.marketinghub.niche.repository.MarketNicheRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class NicheDetailedDescriptionService {
    private final NicheDetailedDescriptionRepository repository;
    private final MarketNicheRepository nicheRepository;
    private final EntityManager em;

    public NicheDetailedDescriptionService(NicheDetailedDescriptionRepository repository,
                                           MarketNicheRepository nicheRepository,
                                           EntityManager em) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
        this.em = em;
    }

    private MarketNiche attachNiche(Long id) {
        if (id == null) return null;
        if (!nicheRepository.existsById(id)) {
            throw new EntityNotFoundException("MarketNiche not found: " + id);
        }
        return em.getReference(MarketNiche.class, id);
    }

    private void validate(CreateNicheDetailedDescriptionRequest request) {
        if (request.getMarketNicheId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "marketNicheId is required");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
        }
    }

    @Transactional
    public NicheDetailedDescription create(CreateNicheDetailedDescriptionRequest request) {
        validate(request);
        MarketNiche niche = attachNiche(request.getMarketNicheId());
        NicheDetailedDescription description = NicheDetailedDescription.builder()
                .marketNiche(niche)
                .title(request.getTitle())
                .description(request.getDescription())
                .pains(request.getPains())
                .desires(request.getDesires())
                .needs(request.getNeeds())
                .prompt(request.getPrompt())
                .model(request.getModel())
                .costUsd(request.getCostUsd())
                .inputTokens(request.getInputTokens())
                .outputTokens(request.getOutputTokens())
                .build();
        NicheDetailedDescription saved = repository.save(description);
        BigDecimal delta = request.getCostUsd();
        if (delta != null && niche != null) {
            BigDecimal current = niche.getTotalCost();
            if (current == null) {
                current = BigDecimal.ZERO;
            }
            niche.setTotalCost(current.add(delta));
        }
        return saved;
    }

    public List<NicheDetailedDescription> listByNiche(Long nicheId) {
        return repository.findByMarketNicheId(nicheId);
    }
}
