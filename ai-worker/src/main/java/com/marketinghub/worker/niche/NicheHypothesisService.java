package com.marketinghub.worker.niche;

import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.service.HypothesisService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Service that loops through all niches with {@code hypothesesToGenerate > 0}
 * and asks ChatGPT to generate hypotheses for each one.
 */
@Service
public class NicheHypothesisService {
    private final MarketNicheRepository nicheRepository;
    private final ChatGptClient chatGptClient;
    private final HypothesisService hypothesisService;

    public NicheHypothesisService(MarketNicheRepository nicheRepository,
                                  ChatGptClient chatGptClient,
                                  HypothesisService hypothesisService) {
        this.nicheRepository = nicheRepository;
        this.chatGptClient = chatGptClient;
        this.hypothesisService = hypothesisService;
    }

    /**
     * Generates hypotheses for all configured niches.
     *
     * @return map keyed by niche id containing the generated hypotheses
     */
    public Map<Long, List<Hypothesis>> generate() {
        Map<Long, List<Hypothesis>> result = new HashMap<>();
        Iterable<MarketNiche> niches = nicheRepository.findAllToGenerateHypotheses();
        for (MarketNiche niche : niches) {
            Integer qty = niche.getHypothesesToGenerate();
            List<CreateHypothesisRequest> requests = chatGptClient.generateHypotheses(niche, qty);
            List<Hypothesis> saved = new ArrayList<>();
            for (CreateHypothesisRequest req : requests) {
                saved.add(hypothesisService.create(req));
            }
            result.put(niche.getId(), saved);
        }
        return result;
    }
}
