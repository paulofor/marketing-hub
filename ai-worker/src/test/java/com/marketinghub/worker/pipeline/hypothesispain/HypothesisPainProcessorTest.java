package com.marketinghub.worker.pipeline.hypothesispain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a montagem do request OpenAI da etapa Dor da hipótese. */
class HypothesisPainProcessorTest {

    /** Deve incluir modo flex no request persistido e enviado para a Responses API. */
    @Test
    void shouldBuildAuditableResponsesApiRequestWithFlexServiceTier() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HypothesisPainProcessor processor = new HypothesisPainProcessor(
                objectMapper,
                properties(),
                org.mockito.Mockito.mock(OpenAiClientPort.class),
                new HypothesisPainResponseValidator(objectMapper),
                org.mockito.Mockito.mock(HypothesisPainBackendClient.class));
        Method method = HypothesisPainProcessor.class.getDeclaredMethod("buildResponsesApiRequest", String.class, String.class);
        method.setAccessible(true);

        String requestJson = (String) method.invoke(processor, "Prompt", "{\"type\":\"object\"}");

        Map<String, Object> request = objectMapper.readValue(requestJson, new TypeReference<>() {});
        assertThat(request)
                .containsEntry("model", "gpt-5.5")
                .containsEntry("input", "Prompt")
                .containsEntry("service_tier", "flex")
                .containsKey("text");
    }

    /** Cria propriedades mínimas para montar o request OpenAI em teste unitário. */
    private HypothesisPainWorkerProperties properties() {
        return new HypothesisPainWorkerProperties(
                true,
                5,
                "http://backend",
                "/api",
                "prompts/hypothesis-pipeline/hypothesis-pain.md",
                "prompts/hypothesis-pipeline/hypothesis-pain-schema.json",
                "hypothesis_pipeline_pain",
                "gpt-5.5",
                Duration.ofMinutes(30));
    }
}
