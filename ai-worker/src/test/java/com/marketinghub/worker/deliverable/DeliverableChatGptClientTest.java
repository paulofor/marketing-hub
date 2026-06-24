package com.marketinghub.worker.deliverable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.deliverable.dto.CreateDeliverableRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.worker.openai.AiGenerationRecorder;
import com.marketinghub.worker.openai.BackendAiGenerationClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeliverableChatGptClientTest {
    private MockWebServer server;
    private DeliverableChatGptClient client;
    private BackendAiGenerationClient generationClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        generationClient = mock(BackendAiGenerationClient.class);
        AiGenerationRecorder recorder = new AiGenerationRecorder(generationClient);
        client = new DeliverableChatGptClient(
                WebClientFactory.testBuilder(),
                objectMapper,
                "test-key",
                server.url("/").toString(),
                "gpt-4o-mini",
                recorder);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void generateDeliverablesBuildsPromptWithContextAndParsesResponse() throws Exception {
        enqueueResponse("""
                [
                  {
                    "title": "Kit de boas-vindas",
                    "description": ["Sequência inicial", "para o time comercial"],
                    "content": ["Enviar e-mail personalizado", "Preparar material impresso"]
                  }
                ]
                """);

        MarketNiche niche = MarketNiche.builder()
                .id(8L)
                .name("Consultorias B2B")
                .description("Empresas que vendem projetos complexos")
                .baseSegmentation("Interesses em gestão e inovação")
                .build();
        Hypothesis hypothesis = new Hypothesis();
        hypothesis.setTitle("Acelerar agendamentos");
        hypothesis.setPromise("Gerar 30% mais reuniões qualificadas");
        hypothesis.setPersona("Diretores comerciais");
        Experiment experiment = new Experiment();
        experiment.setId(22L);
        experiment.setName("Sequência de onboarding");
        experiment.setHypothesis("Alinhar expectativas de novos leads");
        experiment.setPlatform(com.marketinghub.experiment.ExperimentPlatform.FACEBOOK);
        experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
        experiment.setNiche(niche);
        experiment.setHypothesisRef(hypothesis);

        List<CreateDeliverableRequest> deliverables = client.generateDeliverables(experiment, 2);

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-key");
        JsonNode body = objectMapper.readTree(recordedRequest.getBody().readUtf8());
        JsonNode promptNode = body.path("input").get(1).path("content").get(0).path("text");
        assertThat(promptNode.asText())
                .contains("Consultorias B2B")
                .contains("Acelerar agendamentos")
                .contains("Sequência de onboarding")
                .contains("Gere 2 entregáveis")
                .contains("iscas digitais")
                .contains("brinde digital");

        assertThat(deliverables).hasSize(1);
        CreateDeliverableRequest request = deliverables.get(0);
        assertThat(request.getTitle()).isEqualTo("Kit de boas-vindas");
        assertThat(request.getDescription()).isEqualTo("Sequência inicial para o time comercial");
        assertThat(request.getContent()).isEqualTo("Enviar e-mail personalizado\nPreparar material impresso");
        assertThat(request.getModel()).isEqualTo("gpt-4o-mini");
        assertThat(request.getPrompt()).contains("Gere 2 entregáveis");

        ArgumentCaptor<com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest> captor = ArgumentCaptor.forClass(com.marketinghub.ai.generation.dto.AiWorkerGenerationRequest.class);
        verify(generationClient).record(captor.capture());
        assertThat(captor.getValue().getDomain()).isEqualTo("EXPERIMENT_DELIVERABLE");
        assertThat(captor.getValue().getPrompt()).contains("Consultorias B2B");
    }

    private void enqueueResponse(String content) {
        try {
            String body = objectMapper.writeValueAsString(
                    Map.of(
                            "output", List.of(Map.of(
                                    "type", "message",
                                    "role", "assistant",
                                    "content", List.of(Map.of(
                                            "type", "output_text",
                                            "text", content)))),
                            "output_text", content));
            server.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(body));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility wrapper to provide a WebClient.Builder without pulling Spring context in tests.
     */
    private static final class WebClientFactory {
        static org.springframework.web.reactive.function.client.WebClient.Builder testBuilder() {
            return org.springframework.web.reactive.function.client.WebClient.builder();
        }
    }
}
