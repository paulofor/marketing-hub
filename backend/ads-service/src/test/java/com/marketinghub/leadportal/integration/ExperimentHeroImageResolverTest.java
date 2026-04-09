package com.marketinghub.leadportal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJob;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStatus;
import com.marketinghub.experiment.frameworkimage.repository.FrameworkImageGenerationJobRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExperimentHeroImageResolverTest {

    @Mock
    private FrameworkImageGenerationJobRepository jobRepository;

    private ExperimentHeroImageResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ExperimentHeroImageResolver(jobRepository, new ObjectMapper());
    }

    @Test
    void returnsWebUrlFromCompletedHeroJob() {
        Experiment experiment = Experiment.builder()
                .id(10L)
                .landingPageImagePlanning("{" +
                        "\"landingPageImagePlanning\": {" +
                        "  \"images\": [{\"sectionId\": \"s0-hero\", \"placement\": \"hero\"}]" +
                        " }" +
                        "}")
                .build();

        FrameworkImageGenerationJob job = new FrameworkImageGenerationJob();
        job.setPlanningItemKey("s0-hero");
        job.setWebUrl("https://cdn.example/hero-web.jpg");

        when(jobRepository.findByExperimentIdAndPlanningItemKeyInAndStatusOrderByCreatedAtDesc(
                eq(10L), anyCollection(), eq(FrameworkImageGenerationJobStatus.COMPLETED)))
                .thenReturn(List.of(job));

        Optional<String> resolved = resolver.resolve(experiment);

        assertThat(resolved).contains("https://cdn.example/hero-web.jpg");
    }

    @Test
    void fallsBackToSourceUrlWhenWebUrlIsMissing() {
        Experiment experiment = Experiment.builder()
                .id(55L)
                .landingPageImagePlanning("{" +
                        "\"landingPageImagePlanning\": {" +
                        "  \"images\": [{\"sectionId\": \"item-1\", \"placement\": \"hero\"}]" +
                        " }" +
                        "}")
                .build();

        FrameworkImageGenerationJob job = new FrameworkImageGenerationJob();
        job.setPlanningItemKey("item-1");
        job.setSourceUrl("https://cdn.example/hero-source.jpg");

        when(jobRepository.findByExperimentIdAndPlanningItemKeyInAndStatusOrderByCreatedAtDesc(
                eq(55L), anyCollection(), eq(FrameworkImageGenerationJobStatus.COMPLETED)))
                .thenReturn(List.of(job));

        Optional<String> resolved = resolver.resolve(experiment);

        assertThat(resolved).contains("https://cdn.example/hero-source.jpg");
    }

    @Test
    void returnsEmptyWhenNoCompletedJobsExist() {
        Experiment experiment = Experiment.builder()
                .id(42L)
                .landingPageImagePlanning("{}")
                .build();

        when(jobRepository.findByExperimentIdAndPlanningItemKeyInAndStatusOrderByCreatedAtDesc(
                eq(42L), anyCollection(), eq(FrameworkImageGenerationJobStatus.COMPLETED)))
                .thenReturn(List.of());

        Optional<String> resolved = resolver.resolve(experiment);

        assertThat(resolved).isEmpty();
    }
}
