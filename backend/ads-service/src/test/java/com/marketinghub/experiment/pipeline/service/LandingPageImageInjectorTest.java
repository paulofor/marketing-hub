package com.marketinghub.experiment.pipeline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJob;
import com.marketinghub.experiment.frameworkimage.repository.FrameworkImageGenerationJobRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandingPageImageInjectorTest {

    @Mock
    private FrameworkImageGenerationJobRepository jobRepository;

    private LandingPageImageInjector injector;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        injector = new LandingPageImageInjector(jobRepository, objectMapper);
    }

    @Test
    void injectsImagesIntoJsonPayloadWhenHtmlDocumentIsPresent() throws Exception {
        String payload = """
                {"landingPageHtml":{"htmlDocument":"<!doctype html><html><body><section id=\\"s0-hero\\"><img src=\\"https://images.unsplash.com/placeholder.jpg\\" /></section><section id=\\"s1-pain\\"><img src=\\"https://images.unsplash.com/placeholder-2.jpg\\" /></section></body></html>"}}
                """;

        when(jobRepository.findByExperimentIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(
                completedJob("s0-hero", "https://cdn.example.com/hero.webp", null),
                completedJob("s1-pain", null, "https://cdn.example.com/pain.jpg")));

        String enriched = injector.injectImages(10L, payload);

        JsonNode root = objectMapper.readTree(enriched);
        String html = root.at("/landingPageHtml/htmlDocument").asText();
        assertThat(html).contains("https://cdn.example.com/hero.webp");
        assertThat(html).contains("https://cdn.example.com/pain.jpg");
        assertThat(html).doesNotContain("images.unsplash.com/placeholder");
    }

    @Test
    void injectsImagesWhenPayloadIsPureHtml() {
        String html = "<!doctype html><html><body><section id=\"s0-hero\"><img src=\"about:blank\" /></section></body></html>";
        when(jobRepository.findByExperimentIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of(completedJob("s0-hero", null, "https://cdn.example.com/hero.jpg")));

        String enriched = injector.injectImages(55L, html);

        assertThat(enriched).contains("https://cdn.example.com/hero.jpg");
    }

    @Test
    void injectsByDataPlanningItemKeyWhenSectionIdDoesNotMatch() {
        String html = """
                <!doctype html>
                <html><body>
                  <section id="hero-top">
                    <img src="https://placehold.co/1600x900/png?text=Hero" data-planning-item-key="s0-hero" />
                  </section>
                </body></html>
                """;
        when(jobRepository.findByExperimentIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of(completedJob("s0-hero", "https://cdn.example.com/hero-final.jpg", null)));

        String enriched = injector.injectImages(123L, html);

        assertThat(enriched).contains("https://cdn.example.com/hero-final.jpg");
        assertThat(enriched).doesNotContain("placehold.co");
    }

    @Test
    void replacesPlaceholderWithNextGeneratedImageWhenNoDirectMatchExists() {
        String html = """
                <!doctype html>
                <html><body>
                  <section id="intro">
                    <img src="https://placehold.co/1600x1000/png?text=Timeline" alt="Timeline visual" />
                  </section>
                </body></html>
                """;
        when(jobRepository.findByExperimentIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of(completedJob("s9-proof", "https://cdn.example.com/timeline-final.jpg", null)));

        String enriched = injector.injectImages(987L, html);

        assertThat(enriched).contains("https://cdn.example.com/timeline-final.jpg");
        assertThat(enriched).doesNotContain("placehold.co");
    }

    @Test
    void injectsImageByImageSectionAttributes() {
        String html = """
                <!doctype html>
                <html><body>
                  <section id=\"hero\">
                    <img
                      src=\"https://images.unsplash.com/photo-placeholder?auto=format&fit=crop&w=1600&q=70\"
                      data-image-section-id=\"s0-hero-pt-variant10-v1\"
                      data-image-binding-key=\"hero-dor-quanto-custa\"
                    />
                  </section>
                </body></html>
                """;
        when(jobRepository.findByExperimentIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of(completedJob("s0-hero-pt-variant10-v1",
                        "https://cdn.example.com/hero-runtime.jpg", null)));

        String enriched = injector.injectImages(42L, html);

        assertThat(enriched).contains("https://cdn.example.com/hero-runtime.jpg");
        assertThat(enriched).doesNotContain("images.unsplash.com/photo-placeholder");
    }

    @Test
    void keepsOriginalPayloadWhenNoImagesAreAvailable() {
        String payload = "{}";
        when(jobRepository.findByExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());

        assertThat(injector.injectImages(1L, payload)).isEqualTo(payload);
    }

    private FrameworkImageGenerationJob completedJob(String sectionId, String webUrl, String sourceUrl) {
        FrameworkImageGenerationJob job = new FrameworkImageGenerationJob();
        job.setId(UUID.randomUUID());
        job.setPlanningItemKey(sectionId);
        job.setWebUrl(webUrl);
        job.setSourceUrl(sourceUrl);
        Experiment experiment = new Experiment();
        experiment.setId(999L);
        job.setExperiment(experiment);
        return job;
    }
}
