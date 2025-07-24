package com.marketinghub;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.repository.*;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Utility methods to create and persist test fixtures respecting
 * entity relationships.
 */
@Component
@RequiredArgsConstructor
public class FixtureUtils {
    private final MarketNicheRepository nicheRepository;
    private final ExperimentRepository experimentRepository;
    private final CreativeRepository creativeRepository;
    private final AdSetRepository adSetRepository;
    private final com.marketinghub.creative.label.repository.AngleRepository angleRepository;
    private final com.marketinghub.hypothesis.repository.HypothesisRepository hypothesisRepository;

    public MarketNiche createAndSaveNiche() {
        MarketNiche niche = MarketNiche.builder()
                .name("Niche")
                .build();
        return nicheRepository.save(niche);
    }

    public com.marketinghub.hypothesis.Hypothesis createAndSaveHypothesis(MarketNiche niche) {
        var angle = angleRepository.save(com.marketinghub.creative.label.Angle.builder().name("A").build());
        com.marketinghub.hypothesis.Hypothesis h = com.marketinghub.hypothesis.Hypothesis.builder()
                .marketNiche(niche)
                .title("H")
                .premiseAngle(angle)
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(java.math.BigDecimal.ONE)
                .build();
        return hypothesisRepository.save(h);
    }

    public Experiment createAndSaveExperiment(MarketNiche niche) {
        var hyp = createAndSaveHypothesis(niche);
        String name = "Experiment-" + java.util.UUID.randomUUID();
        Experiment exp = Experiment.builder()
                .niche(niche)
                .name(name)
                .hypothesis("H")
                .hypothesisRef(hyp)
                .status(ExperimentStatus.PLANNED)
                .platform(ExperimentPlatform.FACEBOOK)
                .build();
        return experimentRepository.save(exp);
    }

    public Creative createAndSaveCreative(Experiment exp) {
        Creative creative = Creative.builder()
                .experiment(exp)
                .headline("h")
                .primaryText("p")
                .imageUrl("i")
                .status(CreativeStatus.DRAFT)
                .build();
        return creativeRepository.save(creative);
    }

    public AdSet createAndSaveAdSet(Experiment exp) {
        AdSet adSet = AdSet.builder()
                .experiment(exp)
                .location("BR")
                .durationDays(1)
                .build();
        return adSetRepository.save(adSet);
    }
}
