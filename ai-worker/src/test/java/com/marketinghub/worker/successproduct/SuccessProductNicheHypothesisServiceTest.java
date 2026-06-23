package com.marketinghub.worker.successproduct;

import com.marketinghub.successproduct.SuccessProduct;
import com.marketinghub.config.PoolDiagnosticsLogger;
import com.marketinghub.worker.WorkerSuccessProductRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import com.marketinghub.worker.config.TestServiceMocksConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica a geração de nicho e hipótese a partir de produto de sucesso usando cliente dummy no perfil de teste. */
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
        "openai.api-key=sk-test-unit"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(TestServiceMocksConfig.class)
class SuccessProductNicheHypothesisServiceTest {

    @Autowired
    SuccessProductNicheHypothesisService service;

    @Autowired
    WorkerSuccessProductRepository productRepository;

    @MockBean
    AiWorkerGenerationService aiWorkerGenerationService;

    @MockBean
    PoolDiagnosticsLogger poolDiagnosticsLogger;

    @Autowired
    MarketNicheRepository marketNicheRepository;

    @Autowired
    HypothesisRepository hypothesisRepository;

    /** Limpa os registros persistidos para isolar cada cenário de teste. */
    @BeforeEach
    void cleanDb() {
        hypothesisRepository.deleteAll();
        marketNicheRepository.deleteAll();
        productRepository.deleteAll();
    }

    /** Garante que um produto marcado gera nicho, hipótese e desmarca o reprocessamento. */
    @Test
    void generateCreatesNicheAndHypothesisFromProduct() {
        SuccessProduct product = SuccessProduct.builder()
                .description("Produto de sucesso")
                .name("Produto Original")
                .generateNicheHypothesis(true)
                .build();
        productRepository.save(product);

        service.generate();

        assertThat(marketNicheRepository.count()).isEqualTo(1);
        assertThat(hypothesisRepository.count()).isEqualTo(1);

        assertThat(productRepository.findById(product.getId()).orElseThrow().isGenerateNicheHypothesis())
                .isFalse();

        var niche = marketNicheRepository.findAll().get(0);
        var hypothesis = hypothesisRepository.findAll().get(0);
        assertThat(niche.getName()).isEqualTo("Saude");
        assertThat(niche.getDescription()).isEqualTo("Nicho de saude");
        assertThat(hypothesis.getTitle()).isEqualTo("SXX-H001");
        assertThat(hypothesis.getMarketNiche().getId()).isEqualTo(niche.getId());
        assertThat(hypothesis.getPersona()).isEqualTo("Persona A");
        assertThat(hypothesis.getProblem()).isEqualTo("Problema A");
        assertThat(hypothesis.getPromise()).isEqualTo("Promessa A");
        assertThat(hypothesis.getUniqueMechanism()).isEqualTo("Mecanismo A");
    }
}
