package com.marketinghub.videomanagement.pdeaudiovisualv1;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.net.URI;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: comprovar o contrato HTTP especializado usado por Apolo no BPM. */
class ApolloPdeAudiovisualBackendClientTest {
    private MockWebServer server;

    /** Inicializa o backend efêmero antes de cada contrato HTTP. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o backend efêmero e libera sua porta. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Reserva pela combinação exata de agente, processo, atividade e recurso. */
    @Test
    void shouldClaimOnlySpecializedPdeAudiovisualTask() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{
                          "taskId":336,
                          "agentKey":"videomaker",
                          "processCode":"pde-construction-approval",
                          "processVersion":6,
                          "activityId":"audiovisual",
                          "sourceReference":"product:10@private-validation-v1",
                          "executionResource":{"resourceCode":"video-management-service"},
                          "taskTarget":{
                            "productId":10,
                            "productInternalName":"Mira",
                            "pdeContext":{"harness":{"audiovisualRequired":false}}
                          },
                          "processContextJson":"{}"
                        }]
                        """));

        ApolloPdeAudiovisualTask task = client().claim();

        var request = server.takeRequest();
        assertThat(task.taskId()).isEqualTo(336L);
        assertThat(task.taskTarget().pdeContext().path("harness").path("audiovisualRequired").asBoolean())
                .isFalse();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo(
                "/api/internal/agent-tasks/videomaker/stage-executions/pending"
                        + "?processCode=pde-construction-approval&activityId=audiovisual"
                        + "&executionResourceCode=video-management-service");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer internal-test-token");
    }

    /** Reporta conclusão e bloqueio somente pelos callbacks oficiais da mesma tarefa. */
    @Test
    void shouldUseCanonicalResultAndFailureCallbacks() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));
        server.enqueue(new MockResponse().setResponseCode(204));
        ApolloPdeAudiovisualBackendClient client = client();

        client.complete(336L, Map.of("resultJson", "{}"));
        client.block(337L, Map.of("error", "bloqueado"));

        var result = server.takeRequest();
        var failure = server.takeRequest();
        assertThat(result.getMethod()).isEqualTo("POST");
        assertThat(result.getPath())
                .isEqualTo("/api/internal/agent-tasks/videomaker/stage-executions/336/result");
        assertThat(result.getBody().readUtf8()).contains("\"resultJson\":\"{}\"");
        assertThat(failure.getMethod()).isEqualTo("POST");
        assertThat(failure.getPath())
                .isEqualTo("/api/internal/agent-tasks/videomaker/stage-executions/337/failure");
    }

    /** Não repete a reserva mutável quando a resposta pode ter se perdido após o claim. */
    @Test
    void shouldNotRetryAmbiguousClaimFailure() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("temporarily unavailable"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client().claim())
                .isInstanceOf(org.springframework.web.reactive.function.client.WebClientResponseException.class);

        assertThat(server.takeRequest().getPath()).isEqualTo(expectedPendingPath());
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    /** Repete callback transitório cuja transação foi rejeitada antes de concluir a tarefa. */
    @Test
    void shouldRetryTransientResultCallback() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("transaction rolled back"));
        server.enqueue(new MockResponse().setResponseCode(204));

        client().complete(336L, Map.of("resultJson", "{}"));

        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/internal/agent-tasks/videomaker/stage-executions/336/result");
        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/internal/agent-tasks/videomaker/stage-executions/336/result");
    }

    /** Cria o cliente apontado somente para o servidor do teste. */
    private ApolloPdeAudiovisualBackendClient client() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.setBackendBaseUrl(URI.create(server.url("/").toString()));
        properties.setAuthToken("internal-test-token");
        properties.getJobs().setBackendCallBackoff(java.time.Duration.ofMillis(1));
        return new ApolloPdeAudiovisualBackendClient(WebClient.builder(), properties);
    }

    /** Retorna a rota especializada completa usada nas asserções de retentativa. */
    private String expectedPendingPath() {
        return "/api/internal/agent-tasks/videomaker/stage-executions/pending"
                + "?processCode=pde-construction-approval&activityId=audiovisual"
                + "&executionResourceCode=video-management-service";
    }
}
