package com.marketinghub.facebookads.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.marketinghub.audience.Audience;
import com.marketinghub.audience.repository.AudienceRepository;
import com.marketinghub.ads.mapper.FacebookInstantFormMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.facebookads.dto.ExperimentReadyForAdSetDto;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.mapper.HypothesisMapper;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.mapper.MarketNicheMapper;
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
    private AudienceRepository audienceRepository;

    private FacebookAdSetExperimentService service;

    @BeforeEach
    void setUp() {
        ExperimentMapper experimentMapper = Mappers.getMapper(ExperimentMapper.class);
        injectFacebookInstantFormMapper(experimentMapper);
        MarketNicheMapper marketNicheMapper = Mappers.getMapper(MarketNicheMapper.class);
        HypothesisMapper hypothesisMapper = Mappers.getMapper(HypothesisMapper.class);
        com.marketinghub.audience.mapper.AudienceMapper audienceMapper =
                Mappers.getMapper(com.marketinghub.audience.mapper.AudienceMapper.class);
        service = new FacebookAdSetExperimentService(
                experimentRepository,
                audienceRepository,
                experimentMapper,
                marketNicheMapper,
                hypothesisMapper,
                audienceMapper);
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
    void listExperimentsReadyFiltersAudiences() {
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

        Audience matching = new Audience();
        matching.setId(1L);
        matching.setApproved(true);
        matching.setName("Matching audience");
        matching.setHypothesis(hypothesis);
        matching.setNiche(niche);

        Audience generic = new Audience();
        generic.setId(2L);
        generic.setApproved(true);
        generic.setName("Generic audience");
        generic.setHypothesis(null);
        generic.setNiche(niche);

        Hypothesis otherHypothesis = new Hypothesis();
        otherHypothesis.setId(UUID.randomUUID());
        Audience other = new Audience();
        other.setId(3L);
        other.setApproved(true);
        other.setName("Other audience");
        other.setHypothesis(otherHypothesis);
        other.setNiche(niche);

        when(experimentRepository.findAllReadyForAdSets(eq(ExperimentPlatform.FACEBOOK), anyList()))
                .thenReturn(List.of(experiment));
        when(audienceRepository.findDetailedByNicheId(5L))
                .thenReturn(List.of(matching, generic, other));

        List<ExperimentReadyForAdSetDto> result = service.listExperimentsReadyForAdSets();

        assertThat(result).hasSize(1);
        ExperimentReadyForAdSetDto dto = result.getFirst();
        assertThat(dto.getExperiment().getId()).isEqualTo(10L);
        assertThat(dto.getHypothesis()).isNotNull();
        assertThat(dto.getHypothesis().getId()).isEqualTo(hypothesisId);
        assertThat(dto.getAudiences()).extracting("id").containsExactlyInAnyOrder(1L, 2L);
    }
}

