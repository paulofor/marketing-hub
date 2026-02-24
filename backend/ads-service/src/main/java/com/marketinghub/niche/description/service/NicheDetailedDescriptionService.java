package com.marketinghub.niche.description.service;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.description.NicheDetailedDescription;
import com.marketinghub.niche.description.dto.CreateNicheDetailedDescriptionRequest;
import com.marketinghub.niche.description.repository.NicheDetailedDescriptionRepository;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.prompt.Prompt;
import com.marketinghub.prompt.repository.PromptRepository;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.cost.CostAttributionService;
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
    private final PromptRepository promptRepository;
    private final EntityManager em;
    private final CurrencyConversionService currencyConversionService;
    private final CostAttributionService costAttributionService;

    public NicheDetailedDescriptionService(NicheDetailedDescriptionRepository repository,
                                           MarketNicheRepository nicheRepository,
                                           PromptRepository promptRepository,
                                           EntityManager em,
                                           CurrencyConversionService currencyConversionService,
                                           CostAttributionService costAttributionService) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
        this.promptRepository = promptRepository;
        this.em = em;
        this.currencyConversionService = currencyConversionService;
        this.costAttributionService = costAttributionService;
    }

    private MarketNiche attachNiche(Long id) {
        if (id == null) return null;
        if (!nicheRepository.existsById(id)) {
            throw new EntityNotFoundException("MarketNiche not found: " + id);
        }
        return em.getReference(MarketNiche.class, id);
    }

    private Prompt attachPrompt(Long id) {
        if (id == null) return null;
        return promptRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prompt not found: " + id));
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
        Prompt prompt = attachPrompt(request.getPromptId());
        NicheDetailedDescription description = NicheDetailedDescription.builder()
                .marketNiche(niche)
                .promptTemplate(prompt)
                .title(request.getTitle())
                .description(request.getDescription())
                .pains(request.getPains())
                .desires(request.getDesires())
                .needs(request.getNeeds())
                .prompt(request.getPrompt())
                .model(request.getModel())
                .costUsd(request.getCostUsd())
                .active(true)
                .inputTokens(request.getInputTokens())
                .outputTokens(request.getOutputTokens())
                .build();
        NicheDetailedDescription saved = repository.save(description);
        BigDecimal delta = currencyConversionService.usdToBrl(request.getCostUsd());
        costAttributionService.addCostToNiche(niche, delta);
        return saved;
    }

    public List<NicheDetailedDescription> listByNiche(Long nicheId) {
        return repository.findByMarketNicheId(nicheId);
    }

    public List<NicheDetailedDescription> listActiveByNiche(Long nicheId) {
        return repository.findByMarketNicheIdAndActiveTrue(nicheId);
    }

    @Transactional
    public NicheDetailedDescription updateActive(Long nicheId, Long descriptionId, boolean active) {
        NicheDetailedDescription description = repository
                .findByIdAndMarketNicheId(descriptionId, nicheId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Detailed description not found for niche " + nicheId));
        description.setActive(active);
        return description;
    }

    public NicheDetailedDescription getActiveByNicheAndId(Long nicheId, Long descriptionId) {
        return repository.findByIdAndMarketNicheIdAndActiveTrue(descriptionId, nicheId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active detailed description not found for niche " + nicheId));
    }
}
