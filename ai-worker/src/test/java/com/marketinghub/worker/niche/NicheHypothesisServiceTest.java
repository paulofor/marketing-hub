package com.marketinghub.worker.niche;

import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NicheHypothesisServiceTest {
    static MockWebServer mockWebServer;

    @Autowired
    MarketNicheRepository nicheRepository;

    @Autowired
    NicheHypothesisService service;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        registry.add("openai.base-url", () -> mockWebServer.url("/").toString());
        registry.add("openai.api-key", () -> "test-key");
    }

    @AfterAll
    void shutdown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void generateHypothesesForNiches() {
        MarketNiche niche = MarketNiche.builder()
                .name("Saúde")
                .hypothesesToGenerate(2)
                .build();
        nicheRepository.save(niche);
        MarketNiche ignored = MarketNiche.builder()
                .name("Ignored")
                .hypothesesToGenerate(0)
                .build();
        nicheRepository.save(ignored);

        String content = "[{\"title\":\"H1\"},{\"title\":\"H2\"}]";
        try {
            String body = new ObjectMapper().writeValueAsString(
                    Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
            mockWebServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(body));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map<Long, List<CreateHypothesisRequest>> result = service.generate();
        assertThat(result).containsKey(niche.getId());
        List<CreateHypothesisRequest> hyps = result.get(niche.getId());
        assertThat(hyps).hasSize(2);
        assertThat(hyps.get(0).getTitle()).isEqualTo("H1");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }
}
