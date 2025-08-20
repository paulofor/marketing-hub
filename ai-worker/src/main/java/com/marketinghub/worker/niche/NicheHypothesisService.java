package com.marketinghub.worker.niche;

import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that loops through all niches with {@code hypothesesToGenerate > 0}
 * and asks ChatGPT to generate hypotheses for each one.
 */
@Service
public class NicheHypothesisService {
    private final MarketNicheRepository nicheRepository;
    private final ChatGptClient chatGptClient;

    public NicheHypothesisService(MarketNicheRepository nicheRepository, ChatGptClient chatGptClient) {
        this.nicheRepository = nicheRepository;
        this.chatGptClient = chatGptClient;
    }

    /**
     * Generates hypotheses for all configured niches.
     *
     * @return map keyed by niche id containing the generated hypotheses
     */
    public Map<Long, List<CreateHypothesisRequest>> generate() {
        Map<Long, List<CreateHypothesisRequest>> result = new HashMap<>();
        Iterable<MarketNiche> niches = nicheRepository.findAll();
        for (MarketNiche niche : niches) {
            Integer qty = niche.getHypothesesToGenerate();
            if (qty != null && qty > 0) {
                List<CreateHypothesisRequest> hyps = chatGptClient.generateHypotheses(niche, qty);
                result.put(niche.getId(), hyps);
            }
        }
        return result;
    }
}
