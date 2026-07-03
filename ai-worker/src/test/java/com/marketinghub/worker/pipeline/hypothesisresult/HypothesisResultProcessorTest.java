package com.marketinghub.worker.pipeline.hypothesisresult;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Responsabilidade: validar a montagem do request OpenAI da etapa Resultado da hipótese. */
class HypothesisResultProcessorTest {

    /** Deve montar request auditável em Flex, deixando fallback Standard para o client comum. */
    @Test
    void shouldBuildAuditableResponsesApiRequestWithFlexServiceTier() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HypothesisResultProcessor processor = new HypothesisResultProcessor(
                objectMapper,
                properties(),
                org.mockito.Mockito.mock(OpenAiClientPort.class),
                new HypothesisResultResponseValidator(objectMapper),
                org.mockito.Mockito.mock(HypothesisResultBackendClient.class));
        Method method = HypothesisResultProcessor.class.getDeclaredMethod("buildOpenAiRequest", StageContext.class);
        method.setAccessible(true);

        var request = method.invoke(processor, context());
        var openAiRequest = (com.marketinghub.worker.openai.core.model.OpenAiRequest) request;
        Map<String, Object> requestBody = objectMapper.readValue(openAiRequest.requestBodyJson(), new TypeReference<>() {});

        assertThat(openAiRequest.serviceTier()).isEqualTo("flex");
        assertThat(requestBody)
                .containsEntry("model", "gpt-5.5")
                .containsKey("text");
    }

    /** Cria propriedades mínimas para montar o request OpenAI em teste unitário. */
    private HypothesisResultWorkerProperties properties() {
        return new HypothesisResultWorkerProperties(
                true,
                5,
                "http://backend",
                "/api",
                "prompts/hypothesis-pipeline/hypothesis-result.md",
                "prompts/hypothesis-pipeline/hypothesis-result-schema.json",
                "hypothesis_pipeline_result",
                "gpt-5.5",
                Duration.ofMinutes(30));
    }

    /** Cria contexto mínimo com dados suficientes para resolver o template da etapa Resultado. */
    private StageContext<HypothesisResultInput> context() {
        HypothesisResultInput input = new HypothesisResultInput(
                29L,
                "hypothesis-result",
                "job-1",
                Map.of(
                        "CASE_DATA_BLOCK", "marketNicheId: 29",
                        "painModelResponse", Map.of("summary", "Dor validada")));
        StageExecution<HypothesisResultInput> execution = new StageExecution<>(
                "job-1",
                29L,
                "hypothesis-result",
                "INICIADO",
                Instant.now(),
                input,
                Map.of());
        ArtifactStore artifactStore = (type, name, contentType, content, metadata) ->
                new StageArtifact(type, name, contentType, name, "sha256", metadata);
        return new StageContext<>(execution, input, artifactStore, Map.of());
    }
}
