package com.marketinghub.mds.search;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MechanismCandidateBuilderTest {

    private final MechanismCandidateBuilder builder = new MechanismCandidateBuilder(new ActiveComponentExtractor());

    @Test
    void shouldBuildCandidateAndMechanismSpec() {
        ScreenedEvidence first = new ScreenedEvidence(
                "pubmed",
                "doc-1",
                "Calorie deficit and resistance training",
                "Includes protein intake strategy.",
                "",
                "",
                "2024",
                List.of("small sample"),
                "alta",
                "alta",
                List.of("randomized"),
                0.8,
                0.6,
                0.74
        );
        ScreenedEvidence second = new ScreenedEvidence(
                "crossref",
                "doc-2",
                "Calorie deficit with sleep quality protocol",
                "Additional behavioral adherence recommendation.",
                "",
                "",
                "2023",
                List.of("pilot"),
                "moderada",
                "moderada",
                List.of("cohort"),
                0.6,
                0.4,
                0.58
        );

        var result = builder.build(
                55L,
                List.of(first, second),
                Map.of("doc-1", "alta", "doc-2", "moderada"),
                Map.of("doc-1", 101L, "doc-2", 102L)
        );

        assertThat(result.candidateArtifacts()).hasSize(1);
        assertThat(result.mechanismSpecDraft()).isNotNull();
        assertThat(result.supportingEvidenceArtifactIds()).containsExactlyInAnyOrder(101L, 102L);

        var spec = builder.buildMechanismSpec(result.mechanismSpecDraft(), 201L, result.supportingEvidenceArtifactIds());

        assertThat(spec).isNotNull();
        assertThat(spec.artifactType()).isEqualTo("mechanismSpec");
        assertThat(spec.parentArtifactIds()).contains(201L, 101L, 102L);
    }
}
