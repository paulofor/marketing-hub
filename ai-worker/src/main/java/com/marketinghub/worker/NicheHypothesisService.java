package com.marketinghub.worker;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.worker.WorkerMarketNicheRepository;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NicheHypothesisService {
    private static final Logger log = LoggerFactory.getLogger(NicheHypothesisService.class);
    private final WorkerMarketNicheRepository nicheRepository;
    private final HypothesisRepository hypothesisRepository;
    private final HypothesisChatGptClient chatClient;

    public NicheHypothesisService(WorkerMarketNicheRepository nicheRepository,
                                  HypothesisRepository hypothesisRepository,
                                  HypothesisChatGptClient chatClient) {
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.chatClient = chatClient;
    }

    @Transactional
    public void generateHypothesesForNiches() {
        List<MarketNiche> niches = nicheRepository.findByHypothesesToGenerateGreaterThan(0);
        log.info("Found {} niches to generate hypotheses", niches.size());
        for (MarketNiche niche : niches) {
            int qty = niche.getHypothesesToGenerate();
            log.info("Generating {} hypotheses for niche {}", qty, niche.getId());
            List<Hypothesis> hypotheses = chatClient.generate(niche, qty);
            for (Hypothesis h : hypotheses) {
                h.setMarketNiche(niche);
                hypothesisRepository.save(h);
            }
            niche.setHypothesesToGenerate(0);
            nicheRepository.save(niche);
        }
    }
}

