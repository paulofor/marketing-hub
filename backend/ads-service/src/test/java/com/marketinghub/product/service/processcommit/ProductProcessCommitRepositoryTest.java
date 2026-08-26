package com.marketinghub.product.service.processcommit;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;
import com.marketinghub.product.ProductProcessCommit;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.product.ProductProcessCommitRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/** Responsabilidade: comprovar o mapeamento relacional dos commits por produto e processo. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class ProductProcessCommitRepositoryTest {
  @Autowired private ProductRepository productRepository;
  @Autowired private BusinessProcessDefinitionRepository processRepository;
  @Autowired private ProductProcessCommitRepository commitRepository;

  /** Persiste e consulta o vínculo sem misturar produtos ou versões de processo. */
  @Test
  void persistsAndListsProductProcessCommit() {
    Product product = new Product();
    product.setName("Rigel");
    product = productRepository.saveAndFlush(product);
    BusinessProcessDefinition process = processRepository.saveAndFlush(process());
    ProductProcessCommit commit = commit(product, process);

    ProductProcessCommit saved = commitRepository.saveAndFlush(commit);

    assertThat(commitRepository.findByProductIdOrderByRecordedAtDescIdDesc(product.getId()))
        .extracting(ProductProcessCommit::getId)
        .containsExactly(saved.getId());
    assertThat(commitRepository.findByIdAndProductId(saved.getId(), product.getId())).isPresent();
    assertThat(commitRepository.findByIdAndProductId(saved.getId(), product.getId() + 1)).isEmpty();
  }

  /** Monta a versão de processo exigida pelo vínculo relacional. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("pde-communication-sales-journey");
    process.setName("Comunicação e jornada de venda do PDE");
    process.setPurpose("Construir a jornada comercial.");
    process.setOwnerName("Backend");
    process.setTriggerDescription("Produto aprovado.");
    process.setOutcomeDescription("Jornada pronta.");
    process.setVersionNumber(4);
    process.setStatus("PUBLISHED");
    process.setDiagramJson("{\"nodes\":[],\"flows\":[]}");
    process.setCreatedAt(Instant.parse("2026-08-26T12:00:00Z"));
    return process;
  }

  /** Monta um commit completo para o produto e o processo persistidos. */
  private ProductProcessCommit commit(
      Product product, BusinessProcessDefinition processDefinition) {
    ProductProcessCommit commit = new ProductProcessCommit();
    commit.setProduct(product);
    commit.setProcessDefinition(processDefinition);
    commit.setRepositoryName("paulofor/marketing-hub");
    commit.setCommitSha("a".repeat(40));
    commit.setCommitSummary("Registra commits por produto e processo");
    commit.setCommitUrl(
        "https://github.com/paulofor/marketing-hub/commit/" + "a".repeat(40));
    commit.setRecordedBy("time@marketinghub.io");
    commit.setRecordedAt(Instant.parse("2026-08-26T12:30:00Z"));
    return commit;
  }
}
