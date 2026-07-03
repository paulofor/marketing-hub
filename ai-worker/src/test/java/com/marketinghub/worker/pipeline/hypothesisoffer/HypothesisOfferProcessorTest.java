package com.marketinghub.worker.pipeline.hypothesisoffer;

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

/** Responsabilidade: validar a montagem do request OpenAI da etapa Oferta da hipótese. */
class HypothesisOfferProcessorTest {

    /** Deve usar modo standard como default operacional da etapa Oferta. */
    @Test
    void shouldBuildAuditableResponsesApiRequestWithStandardServiceTier() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HypothesisOfferProcessor processor = new HypothesisOfferProcessor(
                objectMapper,
                properties("standard"),
                org.mockito.Mockito.mock(OpenAiClientPort.class),
                new HypothesisOfferResponseValidator(objectMapper),
                org.mockito.Mockito.mock(HypothesisOfferBackendClient.class));
        Method method = HypothesisOfferProcessor.class.getDeclaredMethod("buildOpenAiRequest", StageContext.class);
        method.setAccessible(true);

        OpenAiRequest openAiRequest = (OpenAiRequest) method.invoke(processor, context());
        Map<String, Object> requestBody = objectMapper.readValue(openAiRequest.requestBodyJson(), new TypeReference<>() {});

        assertThat(openAiRequest.serviceTier()).isEqualTo("default");
        assertThat(requestBody)
                .containsEntry("model", "gpt-5.5")
                .containsKey("text");
    }

    /** Cria propriedades mínimas para montar o request OpenAI em teste unitário. */
    private HypothesisOfferWorkerProperties properties(String serviceTier) {
        return new HypothesisOfferWorkerProperties(
                true,
                5,
                "http://backend",
                "/api",
                "prompts/hypothesis-pipeline/hypothesis-offer.md",
                "prompts/hypothesis-pipeline/hypothesis-offer-schema.json",
                "hypothesis_pipeline_offer",
                "gpt-5.5",
                serviceTier,
                Duration.ofMinutes(30));
    }

    /** Cria contexto mínimo com dados suficientes para resolver o template da etapa Oferta. */
    private StageContext<HypothesisOfferInput> context() {
        HypothesisOfferInput input = new HypothesisOfferInput(
                29L,
                "hypothesis-offer",
                "job-1",
                Map.of(
                        "CASE_DATA_BLOCK", "marketNicheId: 29",
                        "painModelResponse", Map.of("summary", "Dor validada"),
                        "resultModelResponse", Map.of("summary", "Resultado validado"),
                        "mechanismModelResponse", Map.of("summary", "Mecanismo validado"),
                        "proofModelResponse", Map.of("summary", "Prova validada")));
        StageExecution<HypothesisOfferInput> execution = new StageExecution<>(
                "job-1",
                29L,
                "hypothesis-offer",
                "INICIADO",
                Instant.now(),
                input,
                Map.of());
        ArtifactStore artifactStore = (type, name, contentType, content, metadata) ->
                new StageArtifact(type, name, contentType, name, "sha256", metadata);
        return new StageContext<>(execution, input, artifactStore, Map.of());
    }
}
