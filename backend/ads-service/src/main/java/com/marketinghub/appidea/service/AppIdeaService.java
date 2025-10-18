package com.marketinghub.appidea.service;

import com.marketinghub.appidea.AppIdea;
import com.marketinghub.appidea.dto.CreateAppIdeaRequest;
import com.marketinghub.appidea.repository.AppIdeaRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service layer that orchestrates creation and retrieval of application ideas.
 */
@Service
public class AppIdeaService {
    private final AppIdeaRepository repository;
    private final MarketNicheRepository nicheRepository;

    public AppIdeaService(AppIdeaRepository repository, MarketNicheRepository nicheRepository) {
        this.repository = repository;
        this.nicheRepository = nicheRepository;
    }

    @Transactional
    public AppIdea createAppIdea(CreateAppIdeaRequest request) {
        if (request.getMarketNicheId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "marketNicheId is required");
        }
        MarketNiche niche = nicheRepository.findById(request.getMarketNicheId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Market niche not found: " + request.getMarketNicheId()));
        AppIdea idea = AppIdea.builder()
                .name(request.getName())
                .niche(niche)
                .targetAudience(request.getTargetAudience())
                .problemToSolve(request.getProblemToSolve())
                .valueProposition(request.getValueProposition())
                .coreFeatures(request.getCoreFeatures())
                .differentiator(request.getDifferentiator())
                .monetization(request.getMonetization())
                .goToMarket(request.getGoToMarket())
                .technologyStack(request.getTechnologyStack())
                .model(request.getModel())
                .prompt(request.getPrompt())
                .build();
        return repository.save(idea);
    }

    public AppIdea getAppIdea(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "AppIdea not found: " + id));
    }

    public List<AppIdea> listAppIdeas() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<AppIdea> listAppIdeasByNiche(Long nicheId) {
        if (!nicheRepository.existsById(nicheId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Market niche not found: " + nicheId);
        }
        return repository.findByNicheIdOrderByCreatedAtDesc(nicheId);
    }
}
