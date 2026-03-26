package com.marketinghub;

import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.repository.CreativeRepository;
import com.marketinghub.creative.label.repository.AngleRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.repository.*;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.ads.InstagramAccountRepository;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
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
    private final com.marketinghub.experiment.repository.MetricPresetRepository metricPresetRepository;
    private final InstagramAccountRepository instagramAccountRepository;
    private final JourneyTemplateRepository journeyTemplateRepository;
    private final LeadPortalFlowRepository leadPortalFlowRepository;

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
                .promise("Promessa")
                .problem("Problema")
                .persona("Persona")
                .uniqueMechanism("Mecanismo")
                .successRule("Regra")
                .offerType(com.marketinghub.hypothesis.OfferType.LEAD)
                .kpiTargetCpl(java.math.BigDecimal.ONE)
                .build();
        return hypothesisRepository.save(h);
    }

    public Experiment createAndSaveExperiment(MarketNiche niche) {
        var hyp = createAndSaveHypothesis(niche);
        MetricPreset preset = metricPresetRepository.save(
                MetricPreset.builder()
                        .id("LEAN_150")
                        .name("Lean-Startup 150")
                        .sampleSize(150)
                        .stopLossFactor(java.math.BigDecimal.valueOf(2))
                        .defaultMdePp(java.math.BigDecimal.valueOf(12))
                        .build());
        String name = "Experiment-" + java.util.UUID.randomUUID();
        JourneyTemplate template = journeyTemplateRepository.save(
                JourneyTemplate.builder()
                        .name("Template Jornada")
                        .build());
        LeadPortalFlow flow = leadPortalFlowRepository.save(
                LeadPortalFlow.builder()
                        .name("Fluxo " + java.util.UUID.randomUUID())
                        .slug("flow-" + java.util.UUID.randomUUID())
                        .marketNiche(niche)
                        .build());
        Experiment exp = Experiment.builder()
                .niche(niche)
                .name(name)
                .hypothesis("H")
                .hypothesisRef(hyp)
                .kpiTargetCpl(java.math.BigDecimal.valueOf(45))
                .metricPreset(preset)
                .baselineCvr(java.math.BigDecimal.valueOf(3))
                .targetCvr(java.math.BigDecimal.valueOf(5))
                .status(ExperimentStatus.PLANNED)
                .platform(ExperimentPlatform.FACEBOOK)
                .stage(ExperimentStage.AD)
                .primaryVariable("Ângulo padrão")
                .primaryMetric("CTR de link (%)")
                .creativesToGenerate(0)
                .instagramAccount(createAndSaveInstagramAccount())
                .journeyTemplate(template)
                .leadPortalFlow(flow)
                .build();
        return experimentRepository.save(exp);
    }

    public InstagramAccount createAndSaveInstagramAccount() {
        InstagramAccount account = InstagramAccount.builder()
                .name("Instagram Test")
                .handle("@instagramtest")
                .code("IG-TEST")
                .build();
        return instagramAccountRepository.save(account);
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
