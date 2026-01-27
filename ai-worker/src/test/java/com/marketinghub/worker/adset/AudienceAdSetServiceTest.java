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
    void generateCreatesAdSetsForTargetingPackage() throws Exception {
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

        when(chatGptClient.planAdSet(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                .thenReturn(new AdSetPlan("BR", List.of("Interest"), List.of(), BigDecimal.TEN, 7,
                        BR_TARGETING, "prompt1", "gpt-4"));

        Map<Long, List<AdSetDto>> result = service.generate();

        assertThat(result).containsKey(10L);
        assertThat(result.get(10L)).hasSize(1);
        assertThat(result.get(10L).stream().map(AdSetDto::getId)).containsExactly(101L);

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

        ObjectNode targeting = payload.putObject("targeting");
        ArrayNode interests = targeting.putArray("interests");
        interests.add(targetingElementNode(1, "Interest", "Descrição"));
        ArrayNode jobTitles = targeting.putArray("jobTitles");
        jobTitles.add(targetingElementNode(2, "Manager", "Cargo"));
        ArrayNode behaviors = targeting.putArray("behaviors");
        behaviors.add(targetingElementNode(3, "Online", "Comportamento"));

        ArrayNode root = OBJECT_MAPPER.createArrayNode();
        root.add(payload);
        return OBJECT_MAPPER.writeValueAsString(root);
    }

    private static ObjectNode targetingElementNode(long id, String term, String description) {
        ObjectNode element = OBJECT_MAPPER.createObjectNode();
        element.put("id", id);
        element.put("term", term);
        element.put("description", description);
        element.put("model", "gpt-4");
        element.put("prompt", "prompt");
        return element;
    }

    private static String escape(String json) {
        return json.replace("\"", "\\\"");
    }
}
