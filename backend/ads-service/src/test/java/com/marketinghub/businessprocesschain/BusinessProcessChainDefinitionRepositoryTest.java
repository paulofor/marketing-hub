package com.marketinghub.businessprocesschain;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/** Responsabilidade: comprovar a filtragem persistente das cadeias exibidas na operação atual. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class BusinessProcessChainDefinitionRepositoryTest {
  @Autowired private BusinessProcessChainDefinitionRepository repository;

  /** Lista somente a versão publicada e preserva a aposentada no banco para auditoria. */
  @Test
  void listsOnlyPublishedChainsInOperationalQuery() {
    repository.save(chain(1, "RETIRED"));
    BusinessProcessChainDefinition published = repository.saveAndFlush(chain(2, "PUBLISHED"));

    var operationalChains = repository.findAllByStatusOrderByNameAscVersionNumberDesc("PUBLISHED");

    assertThat(operationalChains)
        .extracting(BusinessProcessChainDefinition::getId)
        .containsExactly(published.getId());
    assertThat(repository.count()).isEqualTo(2);
  }

  /** Seleciona a versão publicada mais recente para sustentar a posição atual dos produtos. */
  @Test
  void findsLatestPublishedChainByCanonicalCode() {
    repository.save(chain(3, "PUBLISHED"));
    BusinessProcessChainDefinition current = repository.saveAndFlush(chain(5, "PUBLISHED"));
    repository.save(chain(6, "DRAFT"));

    var result =
        repository.findAllByChainCodeAndStatusOrderByVersionNumberDesc(
            "pde-value-creation-delivery", "PUBLISHED");

    assertThat(result).isNotEmpty();
    assertThat(result.getFirst().getId()).isEqualTo(current.getId());
    assertThat(result.getFirst().getVersionNumber()).isEqualTo(5);
  }

  /** Monta uma versão persistível da cadeia PDE para o cenário informado. */
  private BusinessProcessChainDefinition chain(int version, String status) {
    BusinessProcessChainDefinition chain = new BusinessProcessChainDefinition();
    chain.setChainCode("pde-value-creation-delivery");
    chain.setName("Criação e entrega de valor PDE");
    chain.setPurpose("Transformar oportunidade em valor entregue.");
    chain.setOutcomeDescription("Venda entregue com satisfação.");
    chain.setPrimaryMetric("Tempo até venda entregue com satisfação");
    chain.setVersionNumber(version);
    chain.setStatus(status);
    chain.setCreatedAt(Instant.parse("2026-08-22T03:00:00Z"));
    chain.setPublishedAt(Instant.parse("2026-08-22T03:00:00Z"));
    return chain;
  }
}
