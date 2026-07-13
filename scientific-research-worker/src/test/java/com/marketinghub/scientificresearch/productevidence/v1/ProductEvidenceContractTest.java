package com.marketinghub.scientificresearch.productevidence.v1;

import com.marketinghub.scientificresearch.productevidence.v1.deliverablecomposer.DeliverableComposerProcessor;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageCode;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageContext;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageResult;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageStatus;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida contratos essenciais do pipeline de evidência científica.
 */
class ProductEvidenceContractTest {

    /**
     * Confirma que prompt e schema versionados existem no classpath.
     */
    @Test
    void promptAndSchemaMustBeVersionedResources() throws Exception {
        String prompt = new ClassPathResource("prompts/product-evidence/v1/evidence-synthesis.md")
                .getContentAsString(StandardCharsets.UTF_8);
        String schema = new ClassPathResource("prompts/product-evidence/v1/evidence-synthesis-schema.json")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(prompt).contains("{{productIdea}}", "{{scientificQuestion}}", "{{inputJson}}");
        assertThat(schema).contains("approvedForProductClaim", "forbiddenClaims", "citedSources");
    }

    /**
     * Confirma que a última etapa gera artefatos sem avançar para etapa inexistente.
     */
    @Test
    void deliverableComposerMustFinishWithoutNextStage() {
        DeliverableComposerProcessor processor = new DeliverableComposerProcessor();
        StageContext context = new StageContext(
                "job-1",
                "exec-1",
                "MUSA-H001-E003",
                "Combinação correta de perfumes",
                "Quais princípios científicos sustentam combinação de fragrâncias?",
                Map.of("synthesis", Map.of("scientificPrinciple", "percepção olfativa e famílias aromáticas")),
                "/callback");

        StageResult result = processor.process(context);

        assertThat(result.status()).isEqualTo(StageStatus.COMPLETED);
        assertThat(result.nextStageCode()).isNull();
        assertThat(result.artifacts()).hasSize(2);
        assertThat(result.output()).containsKey("markdown");
    }

    /**
     * Confirma os códigos canônicos esperados pelo backend pending.
     */
    @Test
    void stageCodesMustMatchCanonicalEndpoints() {
        assertThat(StageCode.SOURCE_DISCOVERY.code()).isEqualTo("source-discovery");
        assertThat(StageCode.EVIDENCE_SYNTHESIS.code()).isEqualTo("evidence-synthesis");
        assertThat(StageCode.DELIVERABLE_COMPOSER.code()).isEqualTo("deliverable-composer");
    }
}
