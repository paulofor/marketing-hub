package com.marketinghub.mds.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveComponentExtractorTest {

    private final ActiveComponentExtractor extractor = new ActiveComponentExtractor();

    @Test
    void shouldExtractKnownComponentsWhenHintsArePresent() {
        ScreenedEvidence evidence = new ScreenedEvidence(
                "pubmed",
                "doc-1",
                "Calorie deficit and resistance training improves outcomes",
                "Protocol combines protein intake with sleep quality support.",
                "",
                "",
                "2024",
                List.of(),
                "alta",
                "alta",
                List.of("randomized"),
                0.7,
                0.6,
                0.72
        );

        List<String> components = extractor.extract(evidence);

        assertThat(components)
                .contains("calorie deficit", "resistance training", "protein intake", "sleep quality");
    }
}
