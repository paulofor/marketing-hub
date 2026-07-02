package com.marketinghub.experiment;

import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.repository.jpa.journey.JourneyTemplateRepository;
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

/**
 * Valida consultas de repositório usadas pelos fluxos comerciais de experimentos.
 */
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

    /** Garante que analytics de pagina de venda estatica encontra o experimento pelo slug publicado. */
    @Test
    void findFirstByFollowUpActionUrlFlowSlugAcceptsStaticSalesPageSlug() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Sales page niche").build());
        Hypothesis hypothesis = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("Sales page hypothesis")
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Venda direta").build());
        Experiment experiment = repository.save(Experiment.builder()
                .name("Static sales page")
                .niche(niche)
                .hypothesisRef(hypothesis)
                .journeyTemplate(template)
                .followUpActionUrl("https://pagamentopalf.site/sales-page-exp52-protocolo-manutencao-segura-7d.html")
                .build());

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findFirstByFollowUpActionUrlFlowSlug(
                "sales-page-exp52-protocolo-manutencao-segura-7d"))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(experiment.getId()));
    }

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
    void findPendingPixelRequestsIncludesCommerciallyReadyExperimentsWithoutPixel() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder()
                .name("Pixel niche")
                .facebookPixelRequestedAt(java.time.Instant.now())
                .facebookPixelRequestStatus("PENDING")
                .build());
        Hypothesis hyp = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("Hypothesis")
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        MarketNiche nicheWithoutLanding = nicheRepository.save(MarketNiche.builder()
                .name("Sem landing")
                .facebookPixelRequestedAt(java.time.Instant.now())
                .facebookPixelRequestStatus("PENDING")
                .build());

        repository.save(Experiment.builder()
                .niche(niche)
                .hypothesisRef(hyp)
                .name("Pixel planned")
                .journeyTemplate(template)
                .platform(ExperimentPlatform.FACEBOOK)
                .status(ExperimentStatus.PLANNED)
                .creativeApproved(true)
                .followUpActionUrl("https://example.test/landing-planned")
                .build());

        repository.save(Experiment.builder()
                .niche(niche)
                .hypothesisRef(hyp)
                .name("Pixel running")
                .journeyTemplate(template)
                .platform(ExperimentPlatform.FACEBOOK)
                .status(ExperimentStatus.RUNNING)
                .creativeApproved(true)
                .followUpActionUrl("https://example.test/landing-running")
                .build());

        repository.save(Experiment.builder()
                .niche(nicheWithoutLanding)
                .hypothesisRef(hyp)
                .name("Without landing")
                .journeyTemplate(template)
                .platform(ExperimentPlatform.FACEBOOK)
                .status(ExperimentStatus.PAUSED)
                .creativeApproved(true)
                .build());

        entityManager.flush();
        entityManager.clear();

        java.util.List<MarketNiche> result = nicheRepository.findPendingPixelRequests(
                java.util.List.of(ExperimentStatus.PLANNED, ExperimentStatus.RUNNING, ExperimentStatus.PAUSED),
                ExperimentPlatform.FACEBOOK
        );

        assertThat(result)
                .extracting(MarketNiche::getId)
                .containsExactly(niche.getId())
                .doesNotContain(nicheWithoutLanding.getId());
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
                .metaId("1419795191647433")
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

    /** Verifica se as consultas aceitam público Meta publicável em INTEREST, sem exigir JOB_TITLE isolado. */
    @Test
    void readyQueriesAcceptExperimentWithOnlyApprovedInterest() {
        MarketNiche niche = nicheRepository.save(MarketNiche.builder().name("Niche Interest").build());
        Hypothesis hypothesis = hypothesisRepository.save(Hypothesis.builder()
                .marketNiche(niche)
                .title("Hypothesis")
                .build());
        JourneyTemplate template = journeyTemplateRepository.save(JourneyTemplate.builder().name("Lifecycle").build());
        InstagramAccount instagram = entityManager.merge(InstagramAccount.builder()
                .name("IG")
                .handle("interest-only")
                .code("ig-2")
                .build());

        Experiment experiment = repository.save(Experiment.builder()
                .name("Interest only")
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
                .metaId("6001234567890")
                .build());

        entityManager.flush();
        entityManager.clear();

        List<Experiment> campaignReady = repository.findReadyForCampaign(
                ExperimentStatus.PLANNED, ExperimentPlatform.FACEBOOK);
        List<Experiment> adSetReady = repository.findAllReadyForAdSets(
                ExperimentPlatform.FACEBOOK, List.of(ExperimentStatus.PLANNED));

        assertThat(campaignReady).extracting(Experiment::getId).contains(experiment.getId());
        assertThat(adSetReady).extracting(Experiment::getId).contains(experiment.getId());
        assertThat(repository.findForAdSetTargetingById(experiment.getId(), ExperimentPlatform.FACEBOOK)).isPresent();
    }
}
