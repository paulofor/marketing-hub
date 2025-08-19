package com.marketinghub.worker;

import com.marketinghub.creative.label.Angle;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketNicheHypothesisGenerator {

    private static final Logger log = LoggerFactory.getLogger(MarketNicheHypothesisGenerator.class);

    private final MarketNicheRepository nicheRepository;
    private final HypothesisRepository hypothesisRepository;
    private final HypothesisChatGptClient client;
    private final AngleRepository angleRepository;

    public MarketNicheHypothesisGenerator(
            MarketNicheRepository nicheRepository,
            HypothesisRepository hypothesisRepository,
            HypothesisChatGptClient client,
            AngleRepository angleRepository) {
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.client = client;
        this.angleRepository = angleRepository;
    }

    @Scheduled(fixedDelayString = "${worker.niche-hypotheses.delay:600000}")
    @Transactional
    public void generateForNiches() {
        List<MarketNiche> niches = nicheRepository.findAll().stream()
                .filter(n -> n.getHypothesesToGenerate() != null && n.getHypothesesToGenerate() > 0)
                .toList();
        if (niches.isEmpty()) {
            log.debug("No niches with hypotheses to generate");
            return;
        }
        Angle defaultAngle = angleRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No angles available"));
        log.info("Generating hypotheses for {} niches", niches.size());
        for (MarketNiche niche : niches) {
            int qty = niche.getHypothesesToGenerate();
            List<Hypothesis> generated = client.generate(niche, qty);
            log.info("Generated {} hypotheses for niche {}", generated.size(), niche.getId());
            for (Hypothesis h : generated) {
                h.setMarketNiche(niche);
                if (h.getPremiseAngle() == null) {
                    h.setPremiseAngle(defaultAngle);
                }
                hypothesisRepository.save(h);
            }
            niche.setHypothesesToGenerate(0);
            nicheRepository.save(niche);
        }
    }
}
