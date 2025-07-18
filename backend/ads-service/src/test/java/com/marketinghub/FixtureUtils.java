package com.marketinghub;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.repository.CreativeRepository;
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

    public MarketNiche createAndSaveNiche() {
        MarketNiche niche = MarketNiche.builder()
                .name("Niche")
                .build();
        return nicheRepository.save(niche);
    }

    public Experiment createAndSaveExperiment(MarketNiche niche) {
        Experiment exp = Experiment.builder()
                .niche(niche)
                .name("Experiment")
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
