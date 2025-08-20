package com.marketinghub.worker;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NicheHypothesisService {
    private static final Logger log = LoggerFactory.getLogger(NicheHypothesisService.class);
    private final MarketNicheRepository nicheRepository;
    private final HypothesisRepository hypothesisRepository;
    private final HypothesisChatGptClient chatGptClient;

    public NicheHypothesisService(MarketNicheRepository nicheRepository,
                                  HypothesisRepository hypothesisRepository,
                                  HypothesisChatGptClient chatGptClient) {
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.chatGptClient = chatGptClient;
    }

    @Transactional
    public void generateHypotheses() {
        List<MarketNiche> niches = nicheRepository.findAll();
        for (MarketNiche niche : niches) {
            Integer quantity = niche.getHypothesesToGenerate();
            if (quantity != null && quantity > 0) {
                log.info("Generating {} hypotheses for niche {}", quantity, niche.getId());
                List<Hypothesis> generated = chatGptClient.generate(niche, quantity);
                for (Hypothesis hypothesis : generated) {
                    hypothesis.setMarketNiche(niche);
                    hypothesisRepository.save(hypothesis);
                }
            }
        }
    }
}

