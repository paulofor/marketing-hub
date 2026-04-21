package com.marketinghub.experiment;

import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class ExperimentRepositoryTest {

    @Autowired
    ExperimentRepository repository;

    @Autowired
    MarketNicheRepository nicheRepository;

    @Autowired
    HypothesisRepository hypothesisRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    JourneyTemplateRepository journeyTemplateRepository;

    @Test
    void findAllToGenerateCreativesFetchesHypothesis() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("N1").build());
        Hypothesis hyp = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("T")
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        Experiment exp = repository.save(Experiment.builder()
                .niche(niche)
                .hypothesisRef(hyp)
                .name("E1")
                .creativesToGenerate(1)
                .journeyTemplate(template)
                .build());

        entityManager.flush();
        entityManager.clear();

        List<Experiment> result = repository.findAllToGenerateCreatives();
        assertThat(result).hasSize(1);
        Experiment fetched = result.get(0);

        entityManager.clear();

        assertThat(fetched.getHypothesisRef().getTitle()).isEqualTo("T");
    }

    @Test
    void findReadyForPixelIncludesReleasedExperimentsWithoutPixel() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Pixel niche").build());
        Hypothesis hyp = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("Hypothesis")
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        java.time.Instant releaseTime = java.time.Instant.now();

        repository.save(Experiment.builder()
                .niche(niche)
                .hypothesisRef(hyp)
                .name("Pixel planned")
                .journeyTemplate(template)
                .platform(ExperimentPlatform.FACEBOOK)
                .status(ExperimentStatus.PLANNED)
                .creativeApproved(true)
                .facebookReleaseRequestedAt(releaseTime)
                .build());

        repository.save(Experiment.builder()
                .niche(niche)
                .hypothesisRef(hyp)
                .name("Pixel running")
                .journeyTemplate(template)
                .platform(ExperimentPlatform.FACEBOOK)
                .status(ExperimentStatus.RUNNING)
                .creativeApproved(true)
                .facebookReleaseRequestedAt(releaseTime)
                .build());

        repository.save(Experiment.builder()
                .niche(niche)
                .hypothesisRef(hyp)
                .name("Without release")
                .journeyTemplate(template)
                .platform(ExperimentPlatform.FACEBOOK)
                .status(ExperimentStatus.PAUSED)
                .creativeApproved(true)
                .build());

        entityManager.flush();
        entityManager.clear();

        java.util.List<MarketNiche> result = nicheRepository.findReadyForPixel(
                java.util.List.of(ExperimentStatus.PLANNED, ExperimentStatus.RUNNING, ExperimentStatus.PAUSED),
                ExperimentPlatform.FACEBOOK
        );

        assertThat(result)
                .extracting(MarketNiche::getId)
                .containsExactly(niche.getId());
    }

    @Test
    void readyQueriesAcceptExperimentWithOnlyApprovedJobTitle() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Niche JT").build());
        Hypothesis hypothesis = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("Hyp JT")
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        InstagramAccount instagram = entityManager.merge(InstagramAccount.builder()
                .name("IG")
                .handle("job-title-only")
                .code("ig-1")
                .build());

        Experiment experiment11 = repository.save(Experiment.builder()
                .name("Experiment 11")
                .niche(niche)
                .hypothesisRef(hypothesis)
                .journeyTemplate(template)
                .platform(ExperimentPlatform.FACEBOOK)
                .status(ExperimentStatus.PLANNED)
                .creativeApproved(true)
                .instagramAccount(instagram)
                .build());

        entityManager.persist(TargetingElement.builder()
                .niche(niche)
                .hypothesis(hypothesis)
                .type(TargetingElementType.JOB_TITLE)
                .term("CMO")
                .status(TargetingElementStatus.APPROVED)
                .build());

        entityManager.flush();
        entityManager.clear();

        List<Experiment> campaignReady = repository.findReadyForCampaign(
                ExperimentStatus.PLANNED, ExperimentPlatform.FACEBOOK);
        List<Experiment> adSetReady = repository.findAllReadyForAdSets(
                ExperimentPlatform.FACEBOOK, List.of(ExperimentStatus.PLANNED));

        assertThat(campaignReady).extracting(Experiment::getId).contains(experiment11.getId());
        assertThat(adSetReady).extracting(Experiment::getId).contains(experiment11.getId());
    }

    @Test
    void readyQueriesRejectExperimentWithoutApprovedJobTitle() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Niche no JT").build());
        Hypothesis hypothesis = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("Hypothesis")
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        InstagramAccount instagram = entityManager.merge(InstagramAccount.builder()
                .name("IG")
                .handle("missing-job-title")
                .code("ig-2")
                .build());

        Experiment experiment = repository.save(Experiment.builder()
                .name("Missing JT")
                .niche(niche)
                .hypothesisRef(hypothesis)
                .journeyTemplate(template)
                .platform(ExperimentPlatform.FACEBOOK)
                .status(ExperimentStatus.PLANNED)
                .creativeApproved(true)
                .instagramAccount(instagram)
                .build());

        entityManager.persist(TargetingElement.builder()
                .niche(niche)
                .hypothesis(hypothesis)
                .type(TargetingElementType.INTEREST)
                .term("Remarketing")
                .status(TargetingElementStatus.APPROVED)
                .build());

        entityManager.flush();
        entityManager.clear();

        List<Experiment> campaignReady = repository.findReadyForCampaign(
                ExperimentStatus.PLANNED, ExperimentPlatform.FACEBOOK);
        List<Experiment> adSetReady = repository.findAllReadyForAdSets(
                ExperimentPlatform.FACEBOOK, List.of(ExperimentStatus.PLANNED));

        assertThat(campaignReady).extracting(Experiment::getId).doesNotContain(experiment.getId());
        assertThat(adSetReady).extracting(Experiment::getId).doesNotContain(experiment.getId());
    }
}
