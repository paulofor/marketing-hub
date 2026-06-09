package com.marketinghub.facebookads.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.marketinghub.ads.mapper.FacebookInstantFormMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentTargetingSelection;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.facebookads.dto.ExperimentReadyForAdSetDto;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.mapper.HypothesisMapper;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkMapperSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.mapper.MarketNicheMapper;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.TargetingElementDto;
import com.marketinghub.targeting.mapper.TargetingElementMapper;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FacebookAdSetExperimentServiceTest {
    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private ExperimentTargetingSelectionRepository targetingSelectionRepository;
    @Mock
    private TargetingElementRepository targetingElementRepository;

    private FacebookAdSetExperimentService service;

    @BeforeEach
    void setUp() {
        ExperimentMapper experimentMapper = Mappers.getMapper(ExperimentMapper.class);
        injectFacebookInstantFormMapper(experimentMapper);
        MarketNicheMapper marketNicheMapper = Mappers.getMapper(MarketNicheMapper.class);
        HypothesisMapper hypothesisMapper = Mappers.getMapper(HypothesisMapper.class);
        injectFrameworkMapper(hypothesisMapper);
        TargetingElementMapper targetingElementMapper = Mappers.getMapper(TargetingElementMapper.class);
        service = new FacebookAdSetExperimentService(
                experimentRepository,
                targetingSelectionRepository,
                targetingElementRepository,
                experimentMapper,
                marketNicheMapper,
                hypothesisMapper,
                targetingElementMapper);
    }

    private void injectFrameworkMapper(HypothesisMapper hypothesisMapper) {
        HypothesisFrameworkMapperSupport support = new HypothesisFrameworkMapperSupport(new ObjectMapper());
        try {
            hypothesisMapper
                    .getClass()
                    .getMethod("setHypothesisFrameworkMapperSupport", HypothesisFrameworkMapperSupport.class)
                    .invoke(hypothesisMapper, support);
            return;
        } catch (NoSuchMethodException ignored) {
            // fallback to field injection
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível configurar HypothesisFrameworkMapperSupport no HypothesisMapper", e);
        }
        try {
            var field = hypothesisMapper.getClass().getDeclaredField("hypothesisFrameworkMapperSupport");
            field.setAccessible(true);
            field.set(hypothesisMapper, support);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível configurar HypothesisFrameworkMapperSupport no HypothesisMapper", e);
        }
    }

    private void injectFacebookInstantFormMapper(ExperimentMapper experimentMapper) {
        FacebookInstantFormMapper formMapper = Mappers.getMapper(FacebookInstantFormMapper.class);
        try {
            experimentMapper
                    .getClass()
                    .getMethod("setFacebookInstantFormMapper", FacebookInstantFormMapper.class)
                    .invoke(experimentMapper, formMapper);
            return;
        } catch (NoSuchMethodException ignored) {
            // fall back to field injection below
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível configurar o FacebookInstantFormMapper no ExperimentMapper", e);
        }
        try {
            var field = experimentMapper.getClass().getDeclaredField("facebookInstantFormMapper");
            field.setAccessible(true);
            field.set(experimentMapper, formMapper);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Não foi possível configurar o FacebookInstantFormMapper no ExperimentMapper", e);
        }
    }

    @Test
    void listExperimentsReadyIncludesTargetingPackage() {
        MarketNiche niche = new MarketNiche();
        niche.setId(5L);
        niche.setName("Health");

        UUID hypothesisId = UUID.randomUUID();
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(hypothesisId);
        hypothesis.setTitle("Lose weight fast");
        hypothesis.setMarketNiche(niche);

        Experiment experiment = new Experiment();
        experiment.setId(10L);
        experiment.setName("Weight loss test");
        experiment.setNiche(niche);
        experiment.setHypothesisRef(hypothesis);
        experiment.setStatus(ExperimentStatus.PLANNED);
        experiment.setPlatform(ExperimentPlatform.FACEBOOK);
        experiment.setCreativeApproved(true);

        when(experimentRepository.findAllReadyForAdSets(eq(ExperimentPlatform.FACEBOOK), any()))
                .thenReturn(List.of(experiment));

        TargetingElement interest = TargetingElement.builder()
                .id(1L)
                .niche(niche)
                .hypothesis(hypothesis)
                .type(TargetingElementType.INTEREST)
                .term("Remarketing")
                .status(TargetingElementStatus.APPROVED)
                .metaId("interest-1")
                .build();
        TargetingElement jobTitle = TargetingElement.builder()
                .id(2L)
                .niche(niche)
                .hypothesis(hypothesis)
                .type(TargetingElementType.JOB_TITLE)
                .term("CMO")
                .status(TargetingElementStatus.APPROVED)
                .metaId("job-2")
                .build();
        TargetingElement behavior = TargetingElement.builder()
                .id(3L)
                .niche(niche)
                .hypothesis(hypothesis)
                .type(TargetingElementType.BEHAVIOR)
                .term("Engaged Shoppers")
                .status(TargetingElementStatus.APPROVED)
                .metaId("behavior-3")
                .build();

        when(targetingElementRepository.findApprovedForExperiment(5L, TargetingElementType.INTEREST, hypothesisId))
                .thenReturn(List.of(interest));
        when(targetingElementRepository.findApprovedForExperiment(5L, TargetingElementType.JOB_TITLE, hypothesisId))
                .thenReturn(List.of(jobTitle));
        when(targetingElementRepository.findApprovedForExperiment(5L, TargetingElementType.BEHAVIOR, hypothesisId))
                .thenReturn(List.of(behavior));

        List<ExperimentReadyForAdSetDto> result = service.listExperimentsReadyForAdSets();

        assertThat(result).hasSize(1);
        ExperimentReadyForAdSetDto dto = result.getFirst();
        assertThat(dto.getExperiment().getId()).isEqualTo(10L);
        assertThat(dto.getHypothesis()).isNotNull();
        assertThat(dto.getHypothesis().getId()).isEqualTo(hypothesisId);
        assertThat(dto.getTargeting().getInterests()).extracting(TargetingElementDto::getTerm)
                .containsExactly("Remarketing");
        assertThat(dto.getTargeting().getJobTitles()).extracting(TargetingElementDto::getTerm)
                .containsExactly("CMO");
        assertThat(dto.getTargeting().getBehaviors()).extracting(TargetingElementDto::getTerm)
                .containsExactly("Engaged Shoppers");
    }

    @Test
    void listExperimentsReadyAcceptsOnlyApprovedJobTitle() {
        MarketNiche niche = new MarketNiche();
        niche.setId(5L);

        UUID hypothesisId = UUID.randomUUID();
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(hypothesisId);
        hypothesis.setMarketNiche(niche);

        Experiment experiment = new Experiment();
        experiment.setId(11L);
        experiment.setName("Job title only");
        experiment.setNiche(niche);
        experiment.setHypothesisRef(hypothesis);
        experiment.setStatus(ExperimentStatus.PLANNED);
        experiment.setPlatform(ExperimentPlatform.FACEBOOK);
        experiment.setCreativeApproved(true);

        when(experimentRepository.findAllReadyForAdSets(eq(ExperimentPlatform.FACEBOOK), any()))
                .thenReturn(List.of(experiment));
        when(targetingElementRepository.findApprovedForExperiment(5L, TargetingElementType.INTEREST, hypothesisId))
                .thenReturn(List.of());

        TargetingElement jobTitle = TargetingElement.builder()
                .id(2L)
                .niche(niche)
                .hypothesis(hypothesis)
                .type(TargetingElementType.JOB_TITLE)
                .term("CMO")
                .status(TargetingElementStatus.APPROVED)
                .metaId("job-2")
                .build();
        when(targetingElementRepository.findApprovedForExperiment(5L, TargetingElementType.JOB_TITLE, hypothesisId))
                .thenReturn(List.of(jobTitle));
        when(targetingElementRepository.findApprovedForExperiment(5L, TargetingElementType.BEHAVIOR, hypothesisId))
                .thenReturn(List.of());

        List<ExperimentReadyForAdSetDto> result = service.listExperimentsReadyForAdSets();

        assertThat(result).hasSize(1);
        ExperimentReadyForAdSetDto dto = result.getFirst();
        assertThat(dto.getExperiment().getId()).isEqualTo(11L);
        assertThat(dto.getTargeting().getInterests()).isEmpty();
        assertThat(dto.getTargeting().getJobTitles()).extracting(TargetingElementDto::getTerm)
                .containsExactly("CMO");
        assertThat(dto.getTargeting().getBehaviors()).isEmpty();
    }

    @Test
    void listExperimentsReadyPrioritizesSelectedExperimentTargeting() {
        MarketNiche niche = new MarketNiche();
        niche.setId(5L);

        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(UUID.randomUUID());
        hypothesis.setMarketNiche(niche);

        Experiment experiment = new Experiment();
        experiment.setId(13L);
        experiment.setName("Selected targeting");
        experiment.setNiche(niche);
        experiment.setHypothesisRef(hypothesis);
        experiment.setStatus(ExperimentStatus.PLANNED);
        experiment.setPlatform(ExperimentPlatform.FACEBOOK);
        experiment.setCreativeApproved(true);

        TargetingElement selectedJobTitle = TargetingElement.builder()
                .id(21L)
                .niche(niche)
                .type(TargetingElementType.JOB_TITLE)
                .term("Personal Trainer")
                .status(TargetingElementStatus.APPROVED)
                .metaId("1419795191647433")
                .metaKey("Certified Personal Trainer")
                .build();
        TargetingElement ignoredMissingMetaId = TargetingElement.builder()
                .id(22L)
                .niche(niche)
                .type(TargetingElementType.JOB_TITLE)
                .term("Profissionais de Educação Física")
                .status(TargetingElementStatus.APPROVED)
                .build();

        when(experimentRepository.findAllReadyForAdSets(eq(ExperimentPlatform.FACEBOOK), any()))
                .thenReturn(List.of(experiment));
        when(targetingSelectionRepository.findByExperimentIdWithTargetingElement(13L))
                .thenReturn(List.of(
                        ExperimentTargetingSelection.builder()
                                .experiment(experiment)
                                .candidateType(TargetingCandidateType.WORK_POSITION)
                                .term("Personal Trainer")
                                .targetingElement(selectedJobTitle)
                                .build(),
                        ExperimentTargetingSelection.builder()
                                .experiment(experiment)
                                .candidateType(TargetingCandidateType.WORK_POSITION)
                                .term("Profissionais de Educação Física")
                                .targetingElement(ignoredMissingMetaId)
                                .build()));

        List<ExperimentReadyForAdSetDto> result = service.listExperimentsReadyForAdSets(13L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTargeting().getJobTitles())
                .extracting(TargetingElementDto::getMetaId)
                .containsExactly("1419795191647433");
        assertThat(result.getFirst().getTargeting().getJobTitles())
                .extracting(TargetingElementDto::getTerm)
                .containsExactly("Personal Trainer");
    }

    @Test
    void listExperimentsReadySkipsWhenApprovedJobTitleIsMissing() {
        MarketNiche niche = new MarketNiche();
        niche.setId(5L);

        UUID hypothesisId = UUID.randomUUID();
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setId(hypothesisId);
        hypothesis.setMarketNiche(niche);

        Experiment experiment = new Experiment();
        experiment.setId(12L);
        experiment.setNiche(niche);
        experiment.setHypothesisRef(hypothesis);
        experiment.setStatus(ExperimentStatus.PLANNED);
        experiment.setPlatform(ExperimentPlatform.FACEBOOK);
        experiment.setCreativeApproved(true);

        when(experimentRepository.findAllReadyForAdSets(eq(ExperimentPlatform.FACEBOOK), any()))
                .thenReturn(List.of(experiment));
        when(targetingElementRepository.findApprovedForExperiment(5L, TargetingElementType.INTEREST, hypothesisId))
                .thenReturn(List.of());
        when(targetingElementRepository.findApprovedForExperiment(5L, TargetingElementType.JOB_TITLE, hypothesisId))
                .thenReturn(List.of());

        List<ExperimentReadyForAdSetDto> result = service.listExperimentsReadyForAdSets();

        assertThat(result).isEmpty();
    }
}
