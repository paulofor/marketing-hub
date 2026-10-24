package com.marketinghub.repository.jpa.product.operations;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Responsabilidade: comprovar o isolamento da consulta operacional por família, produto e
 * referência.
 */
@DataJpaTest(showSql = false)
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.show-sql=false",
      "spring.datasource.url=jdbc:h2:mem:com.marketinghub.repository.jpa.product.operations.ProductOperationsRunRepositoryTest-${random.uuid};DB_CLOSE_DELAY=0"
    })
class ProductOperationsRunRepositoryTest {
  @Autowired private ProductOperationsRunRepository operations;
  @Autowired private ProcessRunRepository repository;
  @Autowired private BusinessProcessChainDefinitionRepository chains;

  /** A visão operacional respeita produto, ciclo, referência, família e fila antes de pausas. */
  @Test
  void inPlayQueryPreservesScopeAndQueue() {
    var chain = new com.marketinghub.businessprocesschain.BusinessProcessChainDefinition();
    chain.setChainCode("pde-value-creation-delivery");
    chain.setName("Cadeia sintética");
    chain.setPurpose("Teste local");
    chain.setOutcomeDescription("Consulta sem efeitos");
    chain.setPrimaryMetric("Isolamento");
    chain.setVersionNumber(26);
    chain.setStatus("RETIRED");
    chain.setCreatedAt(Instant.now());
    chains.saveAndFlush(chain);
    var paused = scopedRun(4L, 77L, chain.getId(), 13L, "experiment:106");
    repository.save(paused);
    var active = scopedRun(4L, 78L, chain.getId(), 13L, "experiment:106");
    active.setStatus("WAITING_SUBPROCESS");
    repository.save(active);
    var closed = scopedRun(4L, 79L, chain.getId(), 13L, "experiment:106");
    closed.setStatus("COMPLETED");
    repository.save(closed);
    repository.save(scopedRun(5L, 77L, chain.getId(), 13L, "experiment:106"));
    repository.save(scopedRun(4L, 77L, chain.getId(), 14L, "experiment:106"));
    repository.save(scopedRun(4L, 77L, chain.getId(), 13L, "experiment:107"));
    var initial = repository.save(scopedRun(11L, 98L, chain.getId(), null, "experiment:97"));
    repository.flush();
    assertThat(operations.findOpenRootsForOperations(4L, 13L, "experiment:106"))
        .extracting(ProcessRun::getId)
        .containsExactly(active.getId(), paused.getId());
    assertThat(operations.findOpenRootsForOperations(11L, null, null))
        .extracting(ProcessRun::getId)
        .containsExactly(initial.getId());
  }

  /**
   * Cria controle sintético com os campos obrigatórios, mantendo os dados fora da operação real.
   */
  private ProcessRun scopedRun(
      Long productId, Long processId, Long chainId, Long cycleId, String reference) {
    var run = new ProcessRun();
    run.setProductId(productId);
    run.setProcessDefinitionId(processId);
    run.setChainDefinitionId(chainId);
    run.setLearningCycleId(cycleId);
    run.setSourceReference(reference);
    run.setScopeKey(UUID.randomUUID().toString());
    run.setStatus("PAUSED");
    run.setReason("Cenário local");
    run.setCreatedAt(Instant.now());
    run.setUpdatedAt(Instant.now());
    run.setLastReconciledAt(Instant.now());
    return run;
  }
}
