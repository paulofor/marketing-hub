package com.marketinghub.experiment.pipeline.ads;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Validates extraction of ad creative plans from pipeline artifacts stored by the backend.
 */
class ExperimentPipelineAdExtractorTest {

    /**
     * Ensures artifacts wrapped as JSON strings are still usable by the generation button endpoint.
     */
    @Test
    void extractsVariantsFromJsonEmbeddedInTextFields() {
        Experiment experiment = new Experiment();
        experiment.setAdCopy("""
                Resposta do modelo:
                ```json
                {"adCopy":{"primaryTextVariants":[{"label":"dor","primaryText":"Texto","headline":"Headline","description":"Descrição","ctaText":"Saiba mais"}]}}
                ```
                """);
        experiment.setAdImageBriefing("""
                Texto antes {"adImageBriefing":{"briefings":[{"mustMatchAdVariant":"dor","visualBriefing":"Use contraste simples","assetType":"story"}]}} texto depois
                """);
        ExperimentPipelineAdExtractor extractor = new ExperimentPipelineAdExtractor(new ObjectMapper());

        List<PipelineAdCreativePlan> plans = extractor.extract(experiment);

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).variantKey()).isEqualTo("dor");
        assertThat(plans.get(0).headline()).isEqualTo("Headline");
        assertThat(plans.get(0).primaryText()).isEqualTo("Texto");
        assertThat(plans.get(0).format()).isEqualTo("STORY");
        assertThat(plans.get(0).imageBriefing()).isNotNull();
    }
}
