package com.marketinghub.worker.adset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BR_TARGETING =
            "{\"geo_locations\":{\"countries\":[\"BR\"]},\"interests\":[{\"name\":\"Interest\"}]}";
    private static final String MX_TARGETING =
            "{\"geo_locations\":{\"countries\":[\"MX\"]}}";
    private static final String US_TARGETING =
            "{\"geo_locations\":{\"countries\":[\"US\"]},\"interests\":[{\"name\":\"Another\"}]}";

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
        String experimentsPayload = buildExperimentsPayload(hypothesisId);

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
        String firstBody = firstCreate.getBody().readUtf8();
        assertThat(firstBody).contains("\"location\":\"BR\"");
        assertThat(firstBody).contains("\"targetingJson\":\"" + escape(BR_TARGETING) + "\"");
        RecordedRequest secondCreate = server.takeRequest();
        String secondBody = secondCreate.getBody().readUtf8();
        assertThat(secondBody).contains("\"location\":\"US\"");
        assertThat(secondBody).contains("\"targetingJson\":\"" + escape(US_TARGETING) + "\"");
    }

    @Test
    void generateReturnsEmptyWhenBackendFails() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("backend error"));

        Map<Long, List<AdSetDto>> result = service.generate();

        assertThat(result).isEmpty();
        verifyNoInteractions(chatGptClient);
    }

    private static String buildExperimentsPayload(UUID hypothesisId) throws JsonProcessingException {
        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        ObjectNode experiment = payload.putObject("experiment");
        experiment.put("id", 10);
        experiment.put("nicheId", 5);
        experiment.put("hypothesisId", hypothesisId.toString());
        experiment.put("name", "Experiment");
        experiment.put("hypothesis", "Hypothesis");
        experiment.put("status", "PLANNED");
        experiment.put("platform", "FACEBOOK");
        experiment.put("creativeApproved", true);

        ObjectNode niche = payload.putObject("niche");
        niche.put("id", 5);
        niche.put("name", "Health");
        niche.put("baseSegmentation", "Base");
        niche.put("interests", "Interest");
        niche.put("demographicFilters", "Filters");
        niche.put("extraTips", "Tips");

        ObjectNode hypothesis = payload.putObject("hypothesis");
        hypothesis.put("id", hypothesisId.toString());
        hypothesis.put("title", "Title");
        hypothesis.put("promise", "Promise");
        hypothesis.put("persona", "Persona");
        hypothesis.put("mechanism", "Mechanism");
        hypothesis.put("uniqueMechanism", "Unique");

        ArrayNode audiences = payload.putArray("audiences");
        audiences.add(audienceNode(1, hypothesisId, BR_TARGETING));
        audiences.add(audienceNode(2, UUID.randomUUID(), MX_TARGETING));
        audiences.add(audienceNode(3, null, US_TARGETING));

        ArrayNode root = OBJECT_MAPPER.createArrayNode();
        root.add(payload);
        return OBJECT_MAPPER.writeValueAsString(root);
    }

    private static ObjectNode audienceNode(long id, UUID hypothesisId, String targetingSpec) {
        ObjectNode audience = OBJECT_MAPPER.createObjectNode();
        audience.put("id", id);
        audience.put("name", id == 2 ? "Irrelevant" : id == 3 ? "Generic" : "Matching");
        audience.put("description", "Desc");
        audience.put("marketNicheId", 5);
        if (hypothesisId != null) {
            audience.put("hypothesisId", hypothesisId.toString());
        } else {
            audience.putNull("hypothesisId");
        }
        audience.put("approved", true);
        audience.put("targetingStatus", "READY");
        audience.put("targetingSpec", targetingSpec);
        return audience;
    }

    private static String escape(String json) {
        return json.replace("\"", "\\\"");
    }
}
