package com.marketinghub.niche.service;

import com.marketinghub.chat.ChatDialog;
import com.marketinghub.chat.repository.ChatDialogRepository;
import com.marketinghub.differentiatedtechnology.DifferentiatedTechnology;
import com.marketinghub.differentiatedtechnology.repository.DifferentiatedTechnologyRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.dto.CreateMarketNicheRequest;
import com.marketinghub.niche.description.NicheDetailedDescription;
import com.marketinghub.niche.description.repository.NicheDetailedDescriptionRepository;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service layer for market niches.
 */
@Service
public class MarketNicheService {
    private final MarketNicheRepository repository;
    private final ChatDialogRepository chatDialogRepository;
    private final DifferentiatedTechnologyRepository differentiatedTechnologyRepository;
    private final NicheDetailedDescriptionRepository detailedDescriptionRepository;

    public MarketNicheService(MarketNicheRepository repository,
                              ChatDialogRepository chatDialogRepository,
                              DifferentiatedTechnologyRepository differentiatedTechnologyRepository,
                              NicheDetailedDescriptionRepository detailedDescriptionRepository) {
        this.repository = repository;
        this.chatDialogRepository = chatDialogRepository;
        this.differentiatedTechnologyRepository = differentiatedTechnologyRepository;
        this.detailedDescriptionRepository = detailedDescriptionRepository;
    }

    /**
     * Creates and stores a market niche.
     */
    @Transactional
    public MarketNiche create(CreateMarketNicheRequest request) {
        ChatDialog chat = null;
        if (request.getChatDialogId() != null) {
            chat = chatDialogRepository.findById(request.getChatDialogId()).orElseThrow();
        }
        DifferentiatedTechnology differentiatedTechnology =
                resolveDifferentiatedTechnology(request.getDifferentiatedTechnologyId());
        MarketNiche niche = MarketNiche.builder()
                .name(request.getName())
                .description(request.getDescription())
                .interestCategory(request.getInterestCategory())
                .roleCategory(request.getRoleCategory())
                .demandVolume(request.getDemandVolume())
                .promises(request.getPromises())
                .offers(request.getOffers())
                .cost(request.getCost())
                .expense(request.getExpense())
                .totalCost(request.getTotalCost())
                .totalRevenue(request.getTotalRevenue())
                .baseSegmentation(request.getBaseSegmentation())
                .interests(request.getInterests())
                .demographicFilters(request.getDemographicFilters())
                .extraTips(request.getExtraTips())
                .hypothesesToGenerate(request.getHypothesesToGenerate())
                .audiencesToGenerate(request.getAudiencesToGenerate())
                .detailedDescriptionsToGenerate(request.getDetailedDescriptionsToGenerate())
                .hypothesisModel(normalizeModel(request.getHypothesisModel()))
                .detailedDescriptionModel(normalizeModel(request.getDetailedDescriptionModel()))
                .differentiatedTechnology(differentiatedTechnology)
                .chatDialog(chat)
                .build();
        return repository.save(niche);
    }

    public MarketNiche get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public MarketNiche update(Long id, CreateMarketNicheRequest request) {
        MarketNiche niche = repository.findById(id).orElseThrow();
        niche.setName(request.getName());
        niche.setDescription(request.getDescription());
        niche.setInterestCategory(request.getInterestCategory());
        niche.setRoleCategory(request.getRoleCategory());
        niche.setDemandVolume(request.getDemandVolume());
        niche.setPromises(request.getPromises());
        niche.setOffers(request.getOffers());
        niche.setCost(request.getCost());
        niche.setExpense(request.getExpense());
        niche.setTotalCost(request.getTotalCost());
        niche.setTotalRevenue(request.getTotalRevenue());
        niche.setBaseSegmentation(request.getBaseSegmentation());
        niche.setInterests(request.getInterests());
        niche.setDemographicFilters(request.getDemographicFilters());
        niche.setExtraTips(request.getExtraTips());
        niche.setHypothesesToGenerate(request.getHypothesesToGenerate());
        niche.setAudiencesToGenerate(request.getAudiencesToGenerate());
        niche.setDetailedDescriptionsToGenerate(request.getDetailedDescriptionsToGenerate());
        niche.setHypothesisModel(normalizeModel(request.getHypothesisModel()));
        niche.setDetailedDescriptionModel(normalizeModel(request.getDetailedDescriptionModel()));
        niche.setDifferentiatedTechnology(
                resolveDifferentiatedTechnology(request.getDifferentiatedTechnologyId()));
        ChatDialog chat = null;
        if (request.getChatDialogId() != null) {
            chat = chatDialogRepository.findById(request.getChatDialogId()).orElseThrow();
        }
        niche.setChatDialog(chat);
        return repository.save(niche);
    }

    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return null;
        }
        return model;
    }

    /**
     * Requests generation of new audiences by setting the pending quantity.
     */
    @Transactional
    public MarketNiche requestAudiences(Long id, int quantity) {
        MarketNiche niche = repository.findById(id).orElseThrow();
        niche.setAudiencesToGenerate(quantity);
        return niche;
    }

    /**
     * Requests generation of detailed descriptions by setting the pending quantity.
     */
    @Transactional
    public MarketNiche requestDetailedDescriptions(Long id, int quantity, String model) {
        MarketNiche niche = repository.findById(id).orElseThrow();
        niche.setDetailedDescriptionsToGenerate(Math.max(0, quantity));
        if (model != null) {
            niche.setDetailedDescriptionModel(normalizeModel(model));
        }
        return niche;
    }

    private DifferentiatedTechnology resolveDifferentiatedTechnology(Long id) {
        if (id == null) {
            return null;
        }
        return differentiatedTechnologyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Differentiated technology not found: " + id));
    }

    private NicheDetailedDescription resolveDetailedDescription(Long nicheId, Long descriptionId) {
        if (descriptionId == null) {
            return null;
        }
        return detailedDescriptionRepository.findByIdAndMarketNicheIdAndActiveTrue(descriptionId, nicheId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active detailed description not found for niche: " + descriptionId));
    }

    /**
     * Requests generation of new hypotheses by setting the pending quantity.
     */
    @Transactional
    public MarketNiche requestHypotheses(Long id,
                                         int quantity,
                                         String model,
                                         Long differentiatedTechnologyId,
                                         Long detailedDescriptionId) {
        MarketNiche niche = repository.findById(id).orElseThrow();
        niche.setHypothesesToGenerate(quantity);
        if (model != null) {
            niche.setHypothesisModel(normalizeModel(model));
        }
        if (differentiatedTechnologyId != null) {
            niche.setDifferentiatedTechnology(resolveDifferentiatedTechnology(differentiatedTechnologyId));
        }
        if (detailedDescriptionId != null) {
            niche.setHypothesisDetailedDescription(resolveDetailedDescription(id, detailedDescriptionId));
        }
        return niche;
    }

    public Iterable<MarketNiche> list() {
        return repository.findAll();
    }
}
