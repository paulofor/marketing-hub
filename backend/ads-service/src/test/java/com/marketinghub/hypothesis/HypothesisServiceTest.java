package com.marketinghub.hypothesis;

import com.marketinghub.FixtureUtils;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.hypothesis.service.HypothesisService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.PromptEntity;
import com.marketinghub.prompt.repository.PromptAttributeDescriptionRepository;
import com.marketinghub.prompt.repository.PromptAttributeRepository;
import com.marketinghub.prompt.repository.PromptEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.liquibase.enabled=false"
})
@org.springframework.transaction.annotation.Transactional
class HypothesisServiceTest {
    @Autowired
    HypothesisService service;
    @Autowired
    FixtureUtils fixtures;
    @Autowired
    PromptEntityRepository entityRepository;
    @Autowired
    PromptAttributeRepository attributeRepository;
    @Autowired
    PromptAttributeDescriptionRepository descriptionRepository;

    @Test
    void createValidHypothesis() {
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setTitle("Teste");
        req.setProblem("Problema");
        req.setPersona("Persona");
        Hypothesis h = service.create(req);
        assertThat(h.getId()).isNotNull();
        assertThat(h.getStatus()).isEqualTo(HypothesisStatus.BACKLOG);
        assertThat(h.getGeneratedAt()).isNotNull();
    }

    @Test
    void createHypothesisWithoutAngle() {
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setTitle("Sem ângulo");
        req.setProblem("Problema");
        req.setPersona("Persona");
        Hypothesis h = service.create(req);
        assertThat(h.getPremiseAngle()).isNull();
    }

    @Test
    void validateTitle() {
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setTitle("   ");
        req.setProblem("Problema");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void validateProblem() {
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setTitle("Ok");
        req.setProblem("   ");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }


    @Test
    void validatePersona() {
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setTitle("Ok");
        req.setProblem("Problema");
        req.setPersona("   ");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void listByMarketNicheWithNullStatusReturnsAll() {
        MarketNiche niche = fixtures.createAndSaveNiche();

        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setMarketNicheId(niche.getId());
        req.setTitle("H1");
        req.setProblem("Problema");
        req.setPersona("Persona");

        Hypothesis h1 = service.create(req);
        Hypothesis h2 = service.create(req);
        service.updateStatus(h2.getId(), HypothesisStatus.TESTING);

        Iterable<Hypothesis> all = service.listByMarketNiche(niche.getId(), null);
        java.util.List<Hypothesis> list = java.util.stream.StreamSupport
                .stream(all.spliterator(), false)
                .toList();

        assertThat(list).hasSize(2);
    }

    @Test
    void updateHypothesisOnlyWhenBacklog() {
        MarketNiche niche = fixtures.createAndSaveNiche();
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setMarketNicheId(niche.getId());
        req.setTitle("H1");
        req.setProblem("Problema");
        req.setPersona("Persona");

        Hypothesis h = service.create(req);

        com.marketinghub.hypothesis.dto.UpdateHypothesisRequest u = new com.marketinghub.hypothesis.dto.UpdateHypothesisRequest();
        u.setTitle("H2");
        u.setPersona("Persona atualizada");

        Hypothesis updated = service.update(h.getId(), u);
        assertThat(updated.getTitle()).isEqualTo("H2");

        service.updateStatus(h.getId(), HypothesisStatus.TESTING);
        assertThatThrownBy(() -> service.update(h.getId(), u))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void linkPromptAttributeDescriptionsOnCreate() {
        PromptEntity entity = entityRepository.save(PromptEntity.builder().name("hypothesis").build());
        PromptAttribute attr = attributeRepository.save(PromptAttribute.builder().entity(entity).name("title").build());
        PromptAttributeDescription desc = descriptionRepository.save(PromptAttributeDescription.builder().attribute(attr).description("d").build());
        CreateHypothesisRequest req = new CreateHypothesisRequest();
        req.setTitle("Teste");
        req.setProblem("Problema");
        req.setPersona("Persona");
        req.setPromptAttributeDescriptionIds(java.util.List.of(desc.getId()));
        Hypothesis h = service.create(req);
        assertThat(h.getPromptAttributeDescriptions()).extracting(PromptAttributeDescription::getId).contains(desc.getId());
    }
}
