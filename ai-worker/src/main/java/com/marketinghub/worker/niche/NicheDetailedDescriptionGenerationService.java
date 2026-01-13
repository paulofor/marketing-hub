package com.marketinghub.worker.niche;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.description.NicheDetailedDescription;
import com.marketinghub.niche.description.dto.CreateNicheDetailedDescriptionRequest;
import com.marketinghub.niche.description.service.NicheDetailedDescriptionService;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NicheDetailedDescriptionGenerationService {
    private static final Logger log = LoggerFactory.getLogger(NicheDetailedDescriptionGenerationService.class);
    private final MarketNicheRepository nicheRepository;
    private final NicheDescriptionChatGptClient chatGptClient;
    private final NicheDetailedDescriptionService descriptionService;

    public NicheDetailedDescriptionGenerationService(MarketNicheRepository nicheRepository,
                                                     NicheDescriptionChatGptClient chatGptClient,
                                                     NicheDetailedDescriptionService descriptionService) {
        this.nicheRepository = nicheRepository;
        this.chatGptClient = chatGptClient;
        this.descriptionService = descriptionService;
    }

    public Map<Long, List<NicheDetailedDescription>> generate() {
        Map<Long, List<NicheDetailedDescription>> result = new HashMap<>();
        List<MarketNiche> niches = new ArrayList<>();
        nicheRepository.findAllToGenerateDetailedDescriptions().forEach(niches::add);
        if (niches.isEmpty()) {
            return result;
        }

        List<NicheDescriptionChatGptClient.DescriptionBatchRequest> batchRequests = niches.stream()
                .map(n -> new NicheDescriptionChatGptClient.DescriptionBatchRequest(
                        n,
                        n.getDetailedDescriptionsToGenerate() != null ? n.getDetailedDescriptionsToGenerate() : 0,
                        n.getDetailedDescriptionModel()))
                .toList();

        Map<Long, List<CreateNicheDetailedDescriptionRequest>> generated = chatGptClient.generateDescriptionsBatch(batchRequests);

        for (MarketNiche niche : niches) {
            List<CreateNicheDetailedDescriptionRequest> requests = generated.getOrDefault(niche.getId(), List.of());
            List<NicheDetailedDescription> saved = new ArrayList<>();
            for (CreateNicheDetailedDescriptionRequest req : requests) {
                try {
                    saved.add(descriptionService.create(req));
                } catch (Exception e) {
                    log.error("Failed to persist detailed description for niche {}: {}", niche.getId(), req, e);
                }
            }
            niche.setDetailedDescriptionsToGenerate(0);
            nicheRepository.save(niche);
            result.put(niche.getId(), saved);
        }
        return result;
    }
}
