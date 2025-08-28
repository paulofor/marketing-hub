package com.marketinghub.worker.niche;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.PromptEntity;
import com.marketinghub.prompt.repository.PromptAttributeDescriptionRepository;
import com.marketinghub.prompt.repository.PromptAttributeRepository;
import com.marketinghub.prompt.repository.PromptEntityRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    HypothesisRepository hypothesisRepository;
    @Autowired
    PromptEntityRepository entityRepository;
    @Autowired
    PromptAttributeRepository attributeRepository;
    @Autowired
    PromptAttributeDescriptionRepository descriptionRepository;

    @Value("${openai.model}")
    String model;

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

    @BeforeEach
    void resetDb() {
        hypothesisRepository.deleteAll();
        nicheRepository.deleteAll();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void generateHypothesesForNiches() {
        PromptEntity entity = entityRepository.save(PromptEntity.builder().name("hypothesis").build());
        PromptAttribute attr = attributeRepository.save(PromptAttribute.builder().entity(entity).name("title").build());
        PromptAttributeDescription desc = descriptionRepository.save(PromptAttributeDescription.builder().attribute(attr).description("d").build());
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

        String content = """
                [
                  {"title":"H1","promise":"p1","problem":"pr1","persona":"pe1","successRule":"sr1","offerType":"LEAD","kpiTargetCpl":1},
                  {"title":"H2","promise":"p2","problem":"pr2","persona":"pe2","successRule":"sr2","offerType":"LEAD","kpiTargetCpl":1}
                ]
                """;
        try {
            String body = new ObjectMapper().writeValueAsString(
                    Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
            mockWebServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(body));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int initialCount = mockWebServer.getRequestCount();
        Map<Long, List<Hypothesis>> result = service.generate();
        assertThat(result).containsKey(niche.getId());
        List<Hypothesis> hyps = result.get(niche.getId());
        assertThat(hyps).hasSize(2);
        Hypothesis first = hyps.get(0);
        assertThat(first.getTitle()).isEqualTo("H1");
        assertThat(first.getMarketNiche().getId()).isEqualTo(niche.getId());
        assertThat(first.getModel()).isEqualTo(model);
        assertThat(first.getPrompt()).isNotBlank();
        // Access the field via reflection since older ads-service builds may lack the getter
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(first, "generatedAt"))
                .isNotNull();
        assertThat(first.getPromptAttributeDescriptions()).extracting(PromptAttributeDescription::getId).contains(desc.getId());
        assertThat(hypothesisRepository.count()).isEqualTo(2);
        assertThat(mockWebServer.getRequestCount() - initialCount).isEqualTo(1);
        assertThat(nicheRepository.findById(niche.getId()).orElseThrow().getHypothesesToGenerate()).isZero();
    }

    @Test
    void skipHypothesesWithoutTitle() {
        MarketNiche niche = MarketNiche.builder()
                .name("Fitness")
                .hypothesesToGenerate(2)
                .build();
        nicheRepository.save(niche);

        String content = """
                [
                  {"title":"H1","promise":"p1","problem":"pr1","persona":"pe1","successRule":"sr1","offerType":"LEAD","kpiTargetCpl":1},
                  {"promise":"p2","problem":"pr2","persona":"pe2","successRule":"sr2","offerType":"LEAD","kpiTargetCpl":1}
                ]
                """;
        try {
            String body = new ObjectMapper().writeValueAsString(
                    Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
            mockWebServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(body));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int initialCount = mockWebServer.getRequestCount();
        Map<Long, List<Hypothesis>> result = service.generate();
        assertThat(result).containsKey(niche.getId());
        List<Hypothesis> hyps = result.get(niche.getId());
        assertThat(hyps).hasSize(1);
        assertThat(hypothesisRepository.count()).isEqualTo(1);
        assertThat(mockWebServer.getRequestCount() - initialCount).isEqualTo(1);
        assertThat(nicheRepository.findById(niche.getId()).orElseThrow().getHypothesesToGenerate()).isZero();
    }

    @Test
    void nullOfferTypeOnInvalidValue() {
        MarketNiche niche = MarketNiche.builder()
                .name("Tech")
                .hypothesesToGenerate(1)
                .build();
        nicheRepository.save(niche);

        String content = """
                [
                  {"title":"H1","promise":"p1","problem":"pr1","persona":"pe1","successRule":"sr1",
                  "offerType":"Teste grátis 14 dias de plataforma SaaS","kpiTargetCpl":1}
                ]
                """;
        try {
            String body = new ObjectMapper().writeValueAsString(
                    Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
            mockWebServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(body));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map<Long, List<Hypothesis>> result = service.generate();
        assertThat(result).containsKey(niche.getId());
        List<Hypothesis> hyps = result.get(niche.getId());
        assertThat(hyps).hasSize(1);
        assertThat(hyps.get(0).getOfferType()).isNull();
        assertThat(nicheRepository.findById(niche.getId()).orElseThrow().getHypothesesToGenerate()).isZero();
    }
}
