package com.marketinghub.worker.successproduct;

import com.marketinghub.successproduct.SuccessProduct;
import com.marketinghub.worker.WorkerSuccessProductRepository;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.ai.generation.service.AiWorkerGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class SuccessProductNicheHypothesisServiceTest {

    @Autowired
    SuccessProductNicheHypothesisService service;

    @Autowired
    WorkerSuccessProductRepository productRepository;

    @MockBean
    AiWorkerGenerationService aiWorkerGenerationService;

    @Autowired
    MarketNicheRepository marketNicheRepository;

    @Autowired
    HypothesisRepository hypothesisRepository;

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
        assertThat(hypothesis.getTitle()).isEqualTo("Hipotese A");
        assertThat(hypothesis.getMarketNiche().getId()).isEqualTo(niche.getId());
        assertThat(hypothesis.getPersona()).isEqualTo("Persona A");
        assertThat(hypothesis.getProblem()).isEqualTo("Problema A");
        assertThat(hypothesis.getPromise()).isEqualTo("Promessa A");
        assertThat(hypothesis.getUniqueMechanism()).isEqualTo("Mecanismo A");
    }
}

