package com.marketinghub.marketresearch;

import com.marketinghub.marketresearch.domain.MarketResearchStatus;
import com.marketinghub.marketresearch.domain.MarketResearchTask;
import com.marketinghub.marketresearch.dto.MarketResearchRequest;
import com.marketinghub.marketresearch.repository.MarketResearchTaskRepository;
import com.marketinghub.marketresearch.service.MarketResearchService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MarketResearchServiceTest {

    private static MockWebServer mockServer;

    @Autowired
    private MarketResearchService service;

    @Autowired
    private MarketResearchTaskRepository repository;

    @BeforeAll
    static void beforeAll() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterAll
    static void afterAll() throws IOException {
        mockServer.shutdown();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("openai.base-url", () -> mockServer.url("/").toString());
    }

    @Test
    void shouldStoreTaskAndReturnSummary() {
        mockServer.enqueue(new MockResponse().setBody("<html><body>Conteudo fonte 1</body></html>").addHeader("Content-Type", "text/html"));
        mockServer.enqueue(new MockResponse().setBody("{\"id\":\"res_1\",\"output\":[{\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"Resumo IA\"}]}]}").addHeader("Content-Type", "application/json"));

        String sourceUrl = mockServer.url("/fonte-1").toString();
        MarketResearchRequest request = new MarketResearchRequest("Demanda por curso de IA", List.of(sourceUrl), "Mapear oportunidades");

        MarketResearchTask task = service.execute(request);

        assertThat(task.getId()).isNotNull();
        assertThat(task.getStatus()).isEqualTo(MarketResearchStatus.COMPLETED);
        assertThat(task.getSummary()).isEqualTo("Resumo IA");
        assertThat(task.getSources()).containsExactly(sourceUrl);
        assertThat(repository.findById(task.getId())).isPresent();
    }
}
