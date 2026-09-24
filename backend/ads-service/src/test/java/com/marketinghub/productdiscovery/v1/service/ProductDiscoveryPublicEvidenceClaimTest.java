package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/** Comprova na persistência que versões antigas não recebem a nova política nem em retomadas. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class ProductDiscoveryPublicEvidenceClaimTest {
  @Autowired private TestEntityManager entityManager;
  @Autowired private ProductDiscoveryCycleRepository repository;

  /** Segrega a fila por política antes de conceder trabalho e mantém o consumo legado possível. */
  @Test
  void oldWorkerCannotClaimPublicCyclesIncludingExpiredLeases() {
    var legacy = cycle("CONSENTED_INTERVIEWS_V1");
    var publicCycle = cycle("PUBLIC_SOURCES_V1");
    var expired = cycle("PUBLIC_SOURCES_V1");
    expired.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    expired.setLeaseExpiresAt(Instant.now().minusSeconds(3600));
    entityManager.flush();
    var now = Instant.now();
    assertThat(
            repository.findClaimableForUpdate(
                "research",
                ProductDiscoveryCycleStatus.READY_FOR_RESEARCH,
                ProductDiscoveryCycleStatus.RESEARCHING,
                now,
                now.minusSeconds(3600),
                false,
                PageRequest.of(0, 10)))
        .extracting(ProductDiscoveryCycle::getId)
        .containsExactly(legacy.getId());
    assertThat(
            repository.findClaimableForUpdate(
                "research",
                ProductDiscoveryCycleStatus.READY_FOR_RESEARCH,
                ProductDiscoveryCycleStatus.RESEARCHING,
                now,
                now.minusSeconds(3600),
                true,
                PageRequest.of(0, 10)))
        .extracting(ProductDiscoveryCycle::getId)
        .containsExactlyInAnyOrder(legacy.getId(), publicCycle.getId(), expired.getId());
  }

  /** Persiste um ciclo sintético sem candidata, pessoa ou consumo externos. */
  private ProductDiscoveryCycle cycle(String policy) {
    var cycle = new ProductDiscoveryCycle();
    cycle.setTheme("Pesquisa sintética de compatibilidade");
    cycle.setCountry("BR");
    cycle.setLanguage("pt-BR");
    cycle.setStatus(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
    cycle.setStageCode("research");
    cycle.setEvidencePolicy(policy);
    return entityManager.persistAndFlush(cycle);
  }
}
