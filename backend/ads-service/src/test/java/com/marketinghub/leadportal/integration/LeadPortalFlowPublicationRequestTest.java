package com.marketinghub.leadportal.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyle;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeadPortalFlowPublicationRequestTest {

    @Test
    void fromPrioritizesPipelineImageFromImagePlanning() {
        String pipelineImage = "https://cdn.pipeline.test/hero-generated.jpg";
        Experiment experiment = Experiment.builder()
                .landingPageImagePlanning("""
                        {
                          "images": [
                            {
                              "sectionKey": "hero",
                              "imageUrl": "%s"
                            }
                          ]
                        }
                        """.formatted(pipelineImage))
                .landingPageHtml("<img src='https://fallback.test/hero.jpg' />")
                .build();

        LeadPortalSimpleFormStyle style = LeadPortalSimpleFormStyle.builder()
                .slug("style-premium")
                .name("Style Premium")
                .definition(sampleDefinition("https://images.unsplash.com/photo-legacy"))
                .build();

        LeadPortalFlow flow = LeadPortalFlow.builder()
                .slug("exp-10-landing")
                .name("Flow")
                .experiment(experiment)
                .simpleFormStyle(style)
                .questions(List.of())
                .build();

        LeadPortalFlowPublicationRequest payload = LeadPortalFlowPublicationRequest.from(flow);

        assertThat(payload.simpleFormStyle()).isNotNull();
        assertThat(payload.simpleFormStyle().definition()).isNotNull();
        assertThat(payload.simpleFormStyle().definition().heroImageUrl()).isEqualTo(pipelineImage);
    }

    @Test
    void fromFallsBackToStyleImageWhenNoPipelineImageIsAvailable() {
        String styleImage = "https://images.unsplash.com/photo-style";
        Experiment experiment = Experiment.builder()
                .landingPageImagePlanning("{\"images\":[]}")
                .landingPageHtml("<section>sem imagens</section>")
                .build();

        LeadPortalSimpleFormStyle style = LeadPortalSimpleFormStyle.builder()
                .slug("style-premium")
                .name("Style Premium")
                .definition(sampleDefinition(styleImage))
                .build();

        LeadPortalFlow flow = LeadPortalFlow.builder()
                .slug("exp-11-landing")
                .name("Flow")
                .experiment(experiment)
                .simpleFormStyle(style)
                .questions(List.of())
                .build();

        LeadPortalFlowPublicationRequest payload = LeadPortalFlowPublicationRequest.from(flow);

        assertThat(payload.simpleFormStyle()).isNotNull();
        assertThat(payload.simpleFormStyle().definition()).isNotNull();
        assertThat(payload.simpleFormStyle().definition().heroImageUrl()).isEqualTo(styleImage);
    }

    private LeadPortalSimpleFormStyleDefinition sampleDefinition(String heroImageUrl) {
        return new LeadPortalSimpleFormStyleDefinition(
                "#050816",
                null,
                null,
                "#0f172a",
                "#1e293b",
                "0 24px 60px rgba(15, 23, 42, 0.45)",
                "#f8fafc",
                "#dbeafe",
                "#93c5fd",
                "#22d3ee",
                "#38bdf8",
                "#22d3ee",
                "#04121f",
                null,
                "16px",
                null,
                "#0b1220",
                "#1d4ed8",
                "image-right",
                heroImageUrl,
                "rgba(5,8,22,0.62)");
    }
}
