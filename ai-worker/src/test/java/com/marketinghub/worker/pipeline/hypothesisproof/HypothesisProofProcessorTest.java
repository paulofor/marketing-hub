package com.marketinghub.worker.pipeline.hypothesisproof;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.model.OpenAiRequest;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import com.marketinghub.worker.pipeline.ArtifactStore;
import com.marketinghub.worker.pipeline.StageArtifact;
import com.marketinghub.worker.pipeline.StageContext;
import com.marketinghub.worker.pipeline.StageExecution;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a montagem do request OpenAI da etapa Prova da hipótese. */
class HypothesisProofProcessorTest {

    /** Deve usar modo standard como default operacional da etapa Prova. */
    @Test
    void shouldBuildAuditableResponsesApiRequestWithStandardServiceTier() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HypothesisProofProcessor processor = new HypothesisProofProcessor(
                objectMapper,
                properties("standard"),
                org.mockito.Mockito.mock(OpenAiClientPort.class),
                new HypothesisProofResponseValidator(objectMapper),
                org.mockito.Mockito.mock(HypothesisProofBackendClient.class));
        Method method = HypothesisProofProcessor.class.getDeclaredMethod("buildOpenAiRequest", StageContext.class);
        method.setAccessible(true);

        OpenAiRequest openAiRequest = (OpenAiRequest) method.invoke(processor, context());
        Map<String, Object> requestBody = objectMapper.readValue(openAiRequest.requestBodyJson(), new TypeReference<>() {});

        assertThat(openAiRequest.serviceTier()).isEqualTo("default");
        assertThat(requestBody)
                .containsEntry("model", "gpt-5.5")
                .containsKey("text");
    }

    /** Cria propriedades mínimas para montar o request OpenAI em teste unitário. */
    private HypothesisProofWorkerProperties properties(String serviceTier) {
        return new HypothesisProofWorkerProperties(
                true,
                5,
                "http://backend",
                "/api",
                "prompts/hypothesis-pipeline/hypothesis-proof.md",
                "prompts/hypothesis-pipeline/hypothesis-proof-schema.json",
                "hypothesis_pipeline_proof",
                "gpt-5.5",
                serviceTier,
                Duration.ofMinutes(30));
    }

    /** Cria contexto mínimo com dados suficientes para resolver o template da etapa Prova. */
    private StageContext<HypothesisProofInput> context() {
        HypothesisProofInput input = new HypothesisProofInput(
                29L,
                "hypothesis-proof",
                "job-1",
                Map.of(
                        "CASE_DATA_BLOCK", "marketNicheId: 29",
                        "painModelResponse", Map.of("summary", "Dor validada"),
                        "resultModelResponse", Map.of("summary", "Resultado validado"),
                        "mechanismModelResponse", Map.of("summary", "Mecanismo validado")));
        StageExecution<HypothesisProofInput> execution = new StageExecution<>(
                "job-1",
                29L,
                "hypothesis-proof",
                "INICIADO",
                Instant.now(),
                input,
                Map.of());
        ArtifactStore artifactStore = (type, name, contentType, content, metadata) ->
                new StageArtifact(type, name, contentType, name, "sha256", metadata);
        return new StageContext<>(execution, input, artifactStore, Map.of());
    }
}
