package com.marketinghub.worker.targeting;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import com.marketinghub.targeting.service.TargetingElementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TargetingElementGenerationService {
    private static final Logger log = LoggerFactory.getLogger(TargetingElementGenerationService.class);

    private final MarketNicheRepository nicheRepository;
    private final TargetingElementService targetingElementService;
    private final TargetingElementChatGptClient chatGptClient;

    public TargetingElementGenerationService(MarketNicheRepository nicheRepository,
                                             TargetingElementService targetingElementService,
                                             TargetingElementChatGptClient chatGptClient) {
        this.nicheRepository = nicheRepository;
        this.targetingElementService = targetingElementService;
        this.chatGptClient = chatGptClient;
    }

    public Map<Long, List<TargetingElement>> generate() {
        List<MarketNiche> niches = findNichesToGenerate();
        if (niches.isEmpty()) {
            return Map.of();
        }

        List<TargetingElementChatGptClient.TargetingBatchRequest> batchRequests = new ArrayList<>();
        for (MarketNiche niche : niches) {
            int interests = normalize(niche.getInterestsToGenerate());
            int jobTitles = normalize(niche.getJobTitlesToGenerate());
            int behaviors = normalize(niche.getBehaviorsToGenerate());
            if (interests > 0) {
                batchRequests.add(new TargetingElementChatGptClient.TargetingBatchRequest(
                        niche,
                        TargetingElementType.INTEREST,
                        interests,
                        niche.getInterestModel()
                ));
            }
            if (jobTitles > 0) {
                batchRequests.add(new TargetingElementChatGptClient.TargetingBatchRequest(
                        niche,
                        TargetingElementType.JOB_TITLE,
                        jobTitles,
                        niche.getJobTitleModel()
                ));
            }
            if (behaviors > 0) {
                batchRequests.add(new TargetingElementChatGptClient.TargetingBatchRequest(
                        niche,
                        TargetingElementType.BEHAVIOR,
                        behaviors,
                        niche.getBehaviorModel()
                ));
            }
        }

        Map<Long, List<CreateTargetingElementRequest>> generated = chatGptClient.generateBatch(batchRequests);
        Map<Long, List<TargetingElement>> persisted = new LinkedHashMap<>();

        for (MarketNiche niche : niches) {
            List<CreateTargetingElementRequest> requests = generated.getOrDefault(niche.getId(), List.of());
            List<TargetingElement> saved = new ArrayList<>();
            for (CreateTargetingElementRequest request : requests) {
                try {
                    saved.add(targetingElementService.create(request));
                } catch (Exception e) {
                    log.error("Failed to persist targeting element for niche {}: {}", niche.getId(), request, e);
                }
            }
            resetCounters(niche);
            nicheRepository.save(niche);
            persisted.put(niche.getId(), saved);
        }
        return persisted;
    }

    private List<MarketNiche> findNichesToGenerate() {
        Map<Long, MarketNiche> niches = new LinkedHashMap<>();
        nicheRepository.findAllToGenerateInterests().forEach(n -> niches.put(n.getId(), n));
        nicheRepository.findAllToGenerateJobTitles().forEach(n -> niches.put(n.getId(), n));
        nicheRepository.findAllToGenerateBehaviors().forEach(n -> niches.put(n.getId(), n));
        return niches.values().stream().filter(Objects::nonNull).toList();
    }

    private void resetCounters(MarketNiche niche) {
        if (normalize(niche.getInterestsToGenerate()) > 0) {
            niche.setInterestsToGenerate(0);
        }
        if (normalize(niche.getJobTitlesToGenerate()) > 0) {
            niche.setJobTitlesToGenerate(0);
        }
        if (normalize(niche.getBehaviorsToGenerate()) > 0) {
            niche.setBehaviorsToGenerate(0);
        }
    }

    private int normalize(Integer value) {
        return value != null ? value : 0;
    }
}

