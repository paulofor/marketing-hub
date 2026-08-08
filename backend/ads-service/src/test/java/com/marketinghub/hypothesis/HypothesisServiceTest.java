package com.marketinghub.hypothesis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.FixtureUtils;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.hypothesis.service.HypothesisService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.prompt.PromptAttribute;
import com.marketinghub.prompt.PromptAttributeDescription;
import com.marketinghub.prompt.PromptEntity;
import com.marketinghub.repository.jpa.prompt.PromptAttributeDescriptionRepository;
import com.marketinghub.repository.jpa.prompt.PromptAttributeRepository;
import com.marketinghub.repository.jpa.prompt.PromptEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = com.marketinghub.ads.AdsServiceApplication.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driverClassName=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create",
      "spring.liquibase.enabled=false"
    })
@org.springframework.transaction.annotation.Transactional
class HypothesisServiceTest {
  @Autowired HypothesisService service;
  @Autowired FixtureUtils fixtures;
  @Autowired PromptEntityRepository entityRepository;
  @Autowired PromptAttributeRepository attributeRepository;
  @Autowired PromptAttributeDescriptionRepository descriptionRepository;
  @Autowired com.marketinghub.repository.jpa.product.ProductRepository productRepository;

  /** Vincula ao pedido um produto de teste pertencente ao nicho informado. */
  private void bindProduct(CreateHypothesisRequest req, MarketNiche niche) {
    var product =
        productRepository.save(
            com.marketinghub.product.Product.builder()
                .name("Produto " + java.util.UUID.randomUUID())
                .marketNiche(niche)
                .build());
    req.setProductId(product.getId());
  }

  @Test
  void createValidHypothesis() {
    CreateHypothesisRequest req = new CreateHypothesisRequest();
    req.setTitle("Teste ignorado");
    req.setProblem("Problema");
    req.setPersona("Persona");
    bindProduct(req, null);
    Hypothesis h = service.create(req);
    assertThat(h.getId()).isNotNull();
    assertThat(h.getStatus()).isEqualTo(HypothesisStatus.BACKLOG);
    assertThat(h.getGeneratedAt()).isNotNull();
    assertThat(h.getImageFilterTitle()).isNull();
    assertThat(h.getTitle()).isEqualTo("GER-H001");
  }

  @Test
  void createHypothesisWithoutAngle() {
    CreateHypothesisRequest req = new CreateHypothesisRequest();
    req.setTitle("Sem ângulo");
    req.setProblem("Problema");
    req.setPersona("Persona");
    bindProduct(req, null);
    Hypothesis h = service.create(req);
    assertThat(h.getPremiseAngle()).isNull();
  }

  @Test
  void validateProblem() {
    CreateHypothesisRequest req = new CreateHypothesisRequest();
    req.setTitle("Ok");
    req.setProblem("   ");
    bindProduct(req, null);
    assertThatThrownBy(() -> service.create(req))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }

  @Test
  void validatePersona() {
    CreateHypothesisRequest req = new CreateHypothesisRequest();
    req.setTitle("Ok");
    req.setProblem("Problema");
    req.setPersona("   ");
    bindProduct(req, null);
    assertThatThrownBy(() -> service.create(req))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }

  @Test
  void rejectHypothesisWithoutProduct() {
    CreateHypothesisRequest req = new CreateHypothesisRequest();
    req.setProblem("Problema");
    req.setPersona("Persona");

    assertThatThrownBy(() -> service.create(req))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("productId required");
  }

  @Test
  void rejectProductFromAnotherNiche() {
    MarketNiche hypothesisNiche = fixtures.createAndSaveNiche();
    MarketNiche productNiche = fixtures.createAndSaveNiche();
    CreateHypothesisRequest req = new CreateHypothesisRequest();
    req.setMarketNicheId(hypothesisNiche.getId());
    req.setProblem("Problema");
    req.setPersona("Persona");
    bindProduct(req, productNiche);

    assertThatThrownBy(() -> service.create(req))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("mesmo nicho");
  }

  @Test
  void listByMarketNicheWithNullStatusReturnsAll() {
    MarketNiche niche = fixtures.createAndSaveNiche();

    CreateHypothesisRequest req = new CreateHypothesisRequest();
    req.setMarketNicheId(niche.getId());
    req.setTitle("H1");
    req.setProblem("Problema");
    req.setPersona("Persona");
    bindProduct(req, niche);

    Hypothesis h1 = service.create(req);
    Hypothesis h2 = service.create(req);
    service.updateStatus(h2.getId(), HypothesisStatus.TESTING);

    Iterable<Hypothesis> all = service.listByMarketNiche(niche.getId(), null);
    java.util.List<Hypothesis> list =
        java.util.stream.StreamSupport.stream(all.spliterator(), false).toList();

    assertThat(list).hasSize(2);
    assertThat(list).extracting(Hypothesis::getTitle).contains("NXX-H001", "NXX-H002");
  }

  @Test
  void updateHypothesisAllowsEditingAfterStatusChange() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    CreateHypothesisRequest req = new CreateHypothesisRequest();
    req.setMarketNicheId(niche.getId());
    req.setTitle("H1");
    req.setProblem("Problema");
    req.setPersona("Persona");
    bindProduct(req, niche);

    Hypothesis h = service.create(req);

    com.marketinghub.hypothesis.dto.UpdateHypothesisRequest u =
        new com.marketinghub.hypothesis.dto.UpdateHypothesisRequest();
    u.setTitle("H2");
    u.setPersona("Persona atualizada");
    u.setImageFilterTitle("Filtro inicial");

    Hypothesis updated = service.update(h.getId(), u);
    assertThat(updated.getTitle()).isEqualTo("H2");
    assertThat(updated.getImageFilterTitle()).isEqualTo("Filtro inicial");

    service.updateStatus(h.getId(), HypothesisStatus.TESTING);
    u.setImageFilterTitle("Filtro após teste");
    Hypothesis updatedAfterStatusChange = service.update(h.getId(), u);
    assertThat(updatedAfterStatusChange.getImageFilterTitle()).isEqualTo("Filtro após teste");
  }

  @Test
  void linkPromptAttributeDescriptionsOnCreate() {
    PromptEntity entity = entityRepository.save(PromptEntity.builder().name("hypothesis").build());
    PromptAttribute attr =
        attributeRepository.save(PromptAttribute.builder().entity(entity).name("title").build());
    PromptAttributeDescription desc =
        descriptionRepository.save(
            PromptAttributeDescription.builder().attribute(attr).description("d").build());
    CreateHypothesisRequest req = new CreateHypothesisRequest();
    req.setTitle("Teste ignorado");
    req.setProblem("Problema");
    req.setPersona("Persona");
    bindProduct(req, null);
    req.setPromptAttributeDescriptionIds(java.util.List.of(desc.getId()));
    Hypothesis h = service.create(req);
    assertThat(h.getPromptAttributeDescriptions())
        .extracting(PromptAttributeDescription::getId)
        .contains(desc.getId());
  }

  @Test
  void createVersionPreservesSourceAndCommercialContext() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    CreateHypothesisRequest req = new CreateHypothesisRequest();
    req.setMarketNicheId(niche.getId());
    req.setProblem("Faltas na agenda");
    req.setPersona("Nail designer");
    req.setEntrega("Mensagens de confirmação");
    req.setOfferType("TRIPWIRE");
    req.setPrice(new java.math.BigDecimal("27.00"));
    bindProduct(req, niche);
    Hypothesis source = service.create(req);

    var versionRequest =
        new com.marketinghub.hypothesis.dto.CreateHypothesisVersionRequest(
            "Perfil sem identidade visual",
            "Nail designer autônoma",
            "Um perfil à altura do seu talento",
            "Kit visual personalizado",
            "Prévia visual antes da compra",
            "Posts personalizados, imagens e legendas",
            "Briefing concluído por visitante",
            "TRIPWIRE",
            new java.math.BigDecimal("67.00"));

    Hypothesis version = service.createVersion(source.getId(), versionRequest);

    assertThat(version.getId()).isNotEqualTo(source.getId());
    assertThat(version.getSourceHypothesis().getId()).isEqualTo(source.getId());
    assertThat(version.getRootHypothesis().getId()).isEqualTo(source.getId());
    assertThat(version.getVersionNumber()).isEqualTo(2);
    assertThat(version.getProduct().getId()).isEqualTo(source.getProduct().getId());
    assertThat(version.getMarketNiche().getId()).isEqualTo(niche.getId());
    assertThat(version.getEntrega()).contains("Posts personalizados");
    assertThat(version.getPrice()).isEqualByComparingTo("67.00");
    assertThat(version.getStatus()).isEqualTo(HypothesisStatus.BACKLOG);
    assertThat(source.getEntrega()).isEqualTo("Mensagens de confirmação");
    assertThat(source.getPrice()).isEqualByComparingTo("27.00");

    Hypothesis third = service.createVersion(version.getId(), versionRequest);
    assertThat(third.getSourceHypothesis().getId()).isEqualTo(version.getId());
    assertThat(third.getRootHypothesis().getId()).isEqualTo(source.getId());
    assertThat(third.getVersionNumber()).isEqualTo(3);
  }

  @Test
  void rejectVersionWithoutPositivePrice() {
    CreateHypothesisRequest req = new CreateHypothesisRequest();
    req.setProblem("Problema");
    req.setPersona("Persona");
    bindProduct(req, null);
    Hypothesis source = service.create(req);
    var invalid =
        new com.marketinghub.hypothesis.dto.CreateHypothesisVersionRequest(
            "Problema", "Persona", null, null, null, "Entrega", null, "TRIPWIRE", null);

    assertThatThrownBy(() -> service.createVersion(source.getId(), invalid))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("price must be positive");
  }
}
