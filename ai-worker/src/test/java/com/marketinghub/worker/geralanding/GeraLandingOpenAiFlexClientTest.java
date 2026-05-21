package com.marketinghub.worker.geralanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class GeraLandingOpenAiFlexClientTest {

    @Test
    void prepareRequestBodyForFlex_usesJsonRequestBodyWhenAlreadyStructured() throws Exception {
        GeraLandingOpenAiFlexClient client = new GeraLandingOpenAiFlexClient(
                WebClient.builder(),
                new ObjectMapper(),
                "test-key",
                "http://localhost",
                Duration.ofSeconds(30));

        GeraLandingJobDto job = new GeraLandingJobDto(
                UUID.randomUUID(),
                24L,
                "landing-page-design-preset",
                "gpt-5.2",
                "ignored",
                "{\"model\":\"gpt-5.2\",\"input\":[{\"role\":\"user\",\"content\":\"{}\"}]}",
                Instant.now());

        Map<String, Object> payload = client.prepareRequestBodyForFlex(job);

        assertEquals("gpt-5.2", payload.get("model"));
        assertEquals(1, ((List<?>) payload.get("input")).size());
    }

    @Test
    void prepareRequestBodyForFlex_buildsStructuredPayloadWhenBodyIsMarkdownPrompt() throws Exception {
        GeraLandingOpenAiFlexClient client = new GeraLandingOpenAiFlexClient(
                WebClient.builder(),
                new ObjectMapper(),
                "test-key",
                "http://localhost",
                Duration.ofSeconds(30));

        String prompt = "# Tarefa\nGerar preset design em JSON.";
        GeraLandingJobDto job = new GeraLandingJobDto(
                UUID.randomUUID(),
                24L,
                "landing-page-design-preset",
                "gpt-5.2",
                prompt,
                prompt,
                Instant.now());

        Map<String, Object> payload = client.prepareRequestBodyForFlex(job);

        assertEquals("gpt-5.2", payload.get("model"));
        List<?> input = assertInstanceOf(List.class, payload.get("input"));
        Map<?, ?> userMsg = assertInstanceOf(Map.class, input.get(1));
        List<?> content = assertInstanceOf(List.class, userMsg.get("content"));
        Map<?, ?> textItem = assertInstanceOf(Map.class, content.get(0));
        assertEquals(prompt, textItem.get("text"));
    }
}
