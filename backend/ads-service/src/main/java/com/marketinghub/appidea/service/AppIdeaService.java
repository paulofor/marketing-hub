package com.marketinghub.appidea.service;

import com.marketinghub.appidea.AppIdea;
import com.marketinghub.appidea.dto.CreateAppIdeaRequest;
import com.marketinghub.appidea.repository.AppIdeaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer that orchestrates creation and retrieval of application ideas.
 */
@Service
public class AppIdeaService {
    private final AppIdeaRepository repository;

    public AppIdeaService(AppIdeaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AppIdea createAppIdea(CreateAppIdeaRequest request) {
        AppIdea idea = AppIdea.builder()
                .name(request.getName())
                .niche(request.getNiche())
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
        return repository.findById(id).orElseThrow();
    }

    public List<AppIdea> listAppIdeas() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
