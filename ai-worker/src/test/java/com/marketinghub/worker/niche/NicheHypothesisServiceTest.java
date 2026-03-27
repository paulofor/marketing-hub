package com.marketinghub.worker.niche;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.config.PoolDiagnosticsLogger;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.PromptDomain;
import com.marketinghub.prompt.PromptDomainObjectType;
import com.marketinghub.prompt.PromptEntity;
import com.marketinghub.prompt.PromptDomains;
import com.marketinghub.prompt.Prompt;
import com.marketinghub.prompt.repository.PromptAttributeDescriptionRepository;
import com.marketinghub.prompt.repository.PromptAttributeRepository;
import com.marketinghub.prompt.repository.PromptDomainRepository;
import com.marketinghub.prompt.repository.PromptEntityRepository;
import com.marketinghub.prompt.repository.PromptRepository;
import com.marketinghub.worker.config.TestServiceMocksConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aiworker;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "lead-portal.storage.bucket=test-bucket",
        "lead-portal.storage.endpoint=http://localhost:9000",
        "lead-portal.storage.public-base-url=http://localhost:9000/test-bucket",
        "lead-portal.storage.access-key-id=test-access-key",
        "lead-portal.storage.secret-access-key=test-secret-key",
        "lead-portal.storage.region=us-east-1",
        "openai.api-key=test-key"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestServiceMocksConfig.class)
class NicheHypothesisServiceTest {
    static MockWebServer mockWebServer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    AiWorkerGenerationService aiWorkerGenerationService;

    @MockBean
    PoolDiagnosticsLogger poolDiagnosticsLogger;

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

    @Autowired
    PromptDomainRepository promptDomainRepository;

    @Autowired
    PromptRepository promptRepository;

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
        descriptionRepository.deleteAll();
        attributeRepository.deleteAll();
        entityRepository.deleteAll();
        promptDomainRepository.deleteAll();
        promptDomainRepository.flush();
        promptRepository.deleteAll();
        seedPromptDomains();
        createActivePrompt();
    }

    private void createActivePrompt() {
        promptRepository.save(Prompt.builder()
                .name("Test Prompt")
                .domain(PromptDomains.NICHE_HYPOTHESIS)
                .template("Generate hypotheses.")
                .active(true)
                .build());
    }

    private void seedPromptDomains() {
        PromptDomain descriptions = new PromptDomain();
        descriptions.setCode(PromptDomains.NICHE_DETAILED_DESCRIPTION);
        descriptions.setName("Descrições detalhadas");
        descriptions.setObjectTypes(List.of(PromptDomainObjectType.NICHE));
        promptDomainRepository.save(descriptions);

        PromptDomain hypotheses = new PromptDomain();
        hypotheses.setCode(PromptDomains.NICHE_HYPOTHESIS);
        hypotheses.setName("Hipóteses");
        hypotheses.setObjectTypes(List.of(
                PromptDomainObjectType.NICHE,
                PromptDomainObjectType.DETAILED_DESCRIPTION,
                PromptDomainObjectType.DIFFERENTIATED_TECHNOLOGY,
                PromptDomainObjectType.HYPOTHESIS
        ));
        promptDomainRepository.save(hypotheses);
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
        enqueueBatchResponse(niche.getId(), content);

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
        assertThat(mockWebServer.getRequestCount() - initialCount).isEqualTo(4);
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
        enqueueBatchResponse(niche.getId(), content);

        int initialCount = mockWebServer.getRequestCount();
        Map<Long, List<Hypothesis>> result = service.generate();
        assertThat(result).containsKey(niche.getId());
        List<Hypothesis> hyps = result.get(niche.getId());
        assertThat(hyps).hasSize(1);
        assertThat(hypothesisRepository.count()).isEqualTo(1);
        assertThat(mockWebServer.getRequestCount() - initialCount).isEqualTo(4);
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
        enqueueBatchResponse(niche.getId(), content);

        Map<Long, List<Hypothesis>> result = service.generate();
        assertThat(result).containsKey(niche.getId());
        List<Hypothesis> hyps = result.get(niche.getId());
        assertThat(hyps).hasSize(1);
        assertThat(hyps.get(0).getOfferType()).isNull();
        assertThat(nicheRepository.findById(niche.getId()).orElseThrow().getHypothesesToGenerate()).isZero();
    }

    @Test
    void resetQuantityWhenNoHypothesisCreated() {
        MarketNiche niche = MarketNiche.builder()
                .name("Health")
                .hypothesesToGenerate(1)
                .build();
        nicheRepository.save(niche);

        String content = """
                [
                  {"promise":"p1","problem":"pr1","persona":"pe1","successRule":"sr1","offerType":"LEAD","kpiTargetCpl":1}
                ]
                """;
        enqueueBatchResponse(niche.getId(), content);

        Map<Long, List<Hypothesis>> result = service.generate();
        assertThat(result).containsKey(niche.getId());
        assertThat(result.get(niche.getId())).isEmpty();
        assertThat(hypothesisRepository.count()).isZero();
        assertThat(nicheRepository.findById(niche.getId()).orElseThrow().getHypothesesToGenerate()).isZero();
    }

    @Test
    void removeFromQueueWhenPromptTemplateHasFormattingError() {
        Prompt activePrompt = promptRepository
                .findFirstByDomainAndActiveTrueOrderByUpdatedAtDesc(PromptDomains.NICHE_HYPOTHESIS)
                .orElseThrow();
        activePrompt.setTemplate("""
                Gere ${quantity} hipóteses.
                <#if detailedDescription.title?has_content>Título: ${detailedDescription.title}</#if>
                """);
        promptRepository.save(activePrompt);

        MarketNiche niche = MarketNiche.builder()
                .name("Sem descrição detalhada")
                .hypothesesToGenerate(1)
                .build();
        nicheRepository.save(niche);

        int initialCount = mockWebServer.getRequestCount();
        Map<Long, List<Hypothesis>> result = service.generate();

        assertThat(result).isEmpty();
        assertThat(hypothesisRepository.count()).isZero();
        assertThat(mockWebServer.getRequestCount() - initialCount).isZero();
        assertThat(nicheRepository.findById(niche.getId()).orElseThrow().getHypothesesToGenerate()).isZero();
    }

    private void enqueueBatchResponse(Long nicheId, String content) {
        mockWebServer.enqueue(jsonResponse("{\"id\":\"file-1\"}"));
        mockWebServer.enqueue(jsonResponse("{\"id\":\"batch-1\",\"status\":\"validating\"}"));
        mockWebServer.enqueue(jsonResponse("{\"id\":\"batch-1\",\"status\":\"completed\",\"output_file_id\":\"file-output-1\"}"));
        try {
            Map<String, Object> outputContent = Map.of(
                    "type", "output_text",
                    "text", content
            );
            Map<String, Object> output = Map.of(
                    "type", "message",
                    "role", "assistant",
                    "content", List.of(outputContent)
            );
            Map<String, Object> responseBody = Map.of(
                    "output", List.of(output),
                    "output_text", content
            );
            Map<String, Object> lineObject = Map.of(
                    "id", "request-1",
                    "custom_id", "niche-" + nicheId,
                    "response", Map.of(
                            "status_code", 200,
                            "request_id", "req_123",
                            "body", responseBody));
            String line = objectMapper.writeValueAsString(lineObject);
            mockWebServer.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setBody(line));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
