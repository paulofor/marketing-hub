package com.marketinghub.worker.adset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.dto.AdSetDto;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(MockitoExtension.class)
class AudienceAdSetServiceTest {
    private MockWebServer server;
    private AudienceAdSetService service;
    private BackendExperimentClient backendClient;

    @Mock
    AudienceAdSetChatGptClient chatGptClient;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        backendClient = new BackendExperimentClient(
                WebClient.builder(),
                server.url("/").toString(),
                "/api");
        service = new AudienceAdSetService(backendClient, chatGptClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void generateCreatesAdSetsForRelevantAudiences() throws Exception {
        UUID hypothesisId = UUID.randomUUID();
        String experimentsPayload = """
                [
                  {
                    "experiment": {
                      "id": 10,
                      "nicheId": 5,
                      "hypothesisId": "%s",
                      "name": "Experiment",
                      "hypothesis": "Hypothesis",
                      "status": "PLANNED",
                      "platform": "FACEBOOK",
                      "creativeApproved": true
                    },
                    "niche": {
                      "id": 5,
                      "name": "Health",
                      "baseSegmentation": "Base",
                      "interests": "Interest",
                      "demographicFilters": "Filters",
                      "extraTips": "Tips"
                    },
                    "hypothesis": {
                      "id": "%s",
                      "title": "Title",
                      "promise": "Promise",
                      "persona": "Persona",
                      "mechanism": "Mechanism",
                      "uniqueMechanism": "Unique"
                    },
                    "audiences": [
                      {
                        "id": 1,
                        "name": "Matching",
                        "description": "Desc",
                        "marketNicheId": 5,
                        "hypothesisId": "%s",
                        "approved": true
                      },
                      {
                        "id": 2,
                        "name": "Irrelevant",
                        "description": "Desc",
                        "marketNicheId": 5,
                        "hypothesisId": "%s",
                        "approved": true
                      },
                      {
                        "id": 3,
                        "name": "Generic",
                        "description": "Desc",
                        "marketNicheId": 5,
                        "hypothesisId": null,
                        "approved": true
                      }
                    ]
                  }
                ]
                """.formatted(hypothesisId, hypothesisId, hypothesisId, UUID.randomUUID());

        server.enqueue(new MockResponse()
                .setBody(experimentsPayload)
                .setHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
                .setBody("[]")
                .setHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
                .setBody("""
                        {"id":101,"experimentId":10,"location":"BR","interests":"Interest","lookalikes":null,
                         "targetingJson":"{}","budget":10,"durationDays":7,"prompt":"prompt1","model":"gpt-4"}
                        """)
                .setHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
                .setBody("""
                        {"id":102,"experimentId":10,"location":"US","interests":"Another","lookalikes":"Look",
                         "targetingJson":"{\\"key\\":\\"value\\"}","budget":5,"durationDays":5,
                         "prompt":"prompt2","model":"gpt-4"}
                        """)
                .setHeader("Content-Type", "application/json"));

        when(chatGptClient.planAdSet(
                        org.mockito.Mockito.any(),
                        org.mockito.Mockito.argThat(a -> a != null && a.getId().equals(1L))))
                .thenReturn(new AdSetPlan("BR", List.of("Interest"), List.of(), BigDecimal.TEN, 7, "{}", "prompt1", "gpt-4"));
        when(chatGptClient.planAdSet(
                        org.mockito.Mockito.any(),
                        org.mockito.Mockito.argThat(a -> a != null && a.getId().equals(3L))))
                .thenReturn(new AdSetPlan("US", List.of("Another"), List.of("Look"), BigDecimal.valueOf(5), 5,
                        "{\"key\":\"value\"}", "prompt2", "gpt-4"));

        Map<Long, List<AdSetDto>> result = service.generate();

        assertThat(result).containsKey(10L);
        assertThat(result.get(10L)).hasSize(2);
        assertThat(result.get(10L).stream().map(AdSetDto::getId)).containsExactlyInAnyOrder(101L, 102L);

        RecordedRequest experimentsRequest = server.takeRequest();
        assertThat(experimentsRequest.getPath()).isEqualTo("/api/facebook-adsets/experiments-ready");
        RecordedRequest existingRequest = server.takeRequest();
        assertThat(existingRequest.getPath()).isEqualTo("/api/adsets?experimentId=10");
        RecordedRequest firstCreate = server.takeRequest();
        assertThat(firstCreate.getMethod()).isEqualTo("POST");
        assertThat(firstCreate.getPath()).isEqualTo("/api/adsets");
        assertThat(firstCreate.getBody().readUtf8()).contains("\"location\":\"BR\"");
        RecordedRequest secondCreate = server.takeRequest();
        assertThat(secondCreate.getBody().readUtf8()).contains("\"location\":\"US\"");
    }

    @Test
    void generateReturnsEmptyWhenBackendFails() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("backend error"));

        Map<Long, List<AdSetDto>> result = service.generate();

        assertThat(result).isEmpty();
        verifyNoInteractions(chatGptClient);
    }
}
