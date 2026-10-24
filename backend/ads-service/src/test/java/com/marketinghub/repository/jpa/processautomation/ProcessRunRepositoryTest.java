package com.marketinghub.repository.jpa.processautomation;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/** Responsabilidade: comprovar a fila e a leitura isolada do estado durável dos processos. */
@DataJpaTest(showSql = false)
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.show-sql=false",
      "spring.datasource.url=jdbc:h2:mem:com.marketinghub.repository.jpa.processautomation.ProcessRunRepositoryTest-${random.uuid};DB_CLOSE_DELAY=0"
    })
class ProcessRunRepositoryTest {
  @Autowired private ProcessRunRepository repository;

  @Autowired
  private com.marketinghub.repository.jpa.businessprocesschain
          .BusinessProcessChainDefinitionRepository
      chains;

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
    assertThat(repository.findOpenRootsForOperations(4L, 13L, "experiment:106"))
        .extracting(ProcessRun::getId)
        .containsExactly(active.getId(), paused.getId());
    assertThat(repository.findOpenRootsForOperations(11L, null, null))
        .extracting(ProcessRun::getId)
        .containsExactly(initial.getId());
  }

  /** O resumo não pode herdar pausa de outro produto, processo, cadeia, ciclo ou experimento. */
  @Test
  void readsOnlyExactProductProcessCycleAndExperiment() {
    var expected = scopedRun(75L, 77L, 14L, 55L, "experiment:95");
    repository.save(expected);
    repository.save(scopedRun(76L, 77L, 14L, 55L, "experiment:95"));
    repository.save(scopedRun(75L, 78L, 14L, 55L, "experiment:95"));
    repository.save(scopedRun(75L, 77L, 15L, 55L, "experiment:95"));
    repository.save(scopedRun(75L, 77L, 14L, 56L, "experiment:95"));
    repository.save(scopedRun(75L, 77L, 14L, 55L, "experiment:96"));
    repository.flush();
    assertThat(
            repository
                .findFirstByProductIdAndProcessDefinitionIdAndChainDefinitionIdAndLearningCycleIdAndSourceReferenceOrderByIdDesc(
                    75L, 77L, 14L, 55L, "experiment:95"))
        .get()
        .extracting(ProcessRun::getId)
        .isEqualTo(expected.getId());
    assertThat(
            repository
                .findFirstByProductIdAndProcessDefinitionIdAndChainDefinitionIdAndLearningCycleIdAndSourceReferenceOrderByIdDesc(
                    75L, 77L, 14L, 57L, "experiment:95"))
        .isEmpty();
  }

  /** Prepara uma pausa sintética com identidade completa para a consulta do resumo. */
  private ProcessRun scopedRun(Long product, Long process, Long chain, Long cycle, String source) {
    var result = run(product, "PAUSED");
    result.setProcessDefinitionId(process);
    result.setChainDefinitionId(chain);
    result.setLearningCycleId(cycle);
    result.setSourceReference(source);
    return result;
  }

  /** Uma falha terminal preserva o histórico, mas não bloqueia nem volta à fila de conciliação. */
  @Test
  void terminalErrorDoesNotReserveProduct() {
    var failed = repository.save(run(4L, "ERROR"));
    var queued = repository.save(run(4L, "QUEUED"));
    repository.save(run(5L, "PAUSED"));

    assertThat(repository.activeRoots(4L))
        .extracting(ProcessRun::getId)
        .containsExactly(queued.getId());
    assertThat(repository.pending(PageRequest.of(0, 10)))
        .containsExactly(queued.getId())
        .doesNotContain(failed.getId());
    assertThat(repository.findById(failed.getId()))
        .get()
        .extracting(ProcessRun::getStatus)
        .isEqualTo("ERROR");
  }

  /** Pai e filho alternam a vez sem concorrer no mesmo produto nem bloquear outro produto. */
  @ParameterizedTest
  @ValueSource(longs = {7L, 9007L})
  void offersOnePendingRunPerProductWithFairRotation(long productId) {
    var parent = repository.saveAndFlush(run(productId, "WAITING_SUBPROCESS"));
    var child = run(productId, "WAITING_ACTIVITY");
    child.setParentRunId(parent.getId());
    child.setLastReconciledAt(parent.getLastReconciledAt().plusSeconds(1));
    repository.saveAndFlush(child);
    var other = repository.saveAndFlush(run(productId + 1, "QUEUED"));

    assertThat(repository.pending(PageRequest.of(0, 20)))
        .containsExactly(parent.getId(), other.getId());

    parent.setLastReconciledAt(child.getLastReconciledAt().plusSeconds(1));
    repository.saveAndFlush(parent);
    assertThat(repository.pending(PageRequest.of(0, 20)))
        .containsExactly(other.getId(), child.getId());
    assertThat(parent.getStatus()).isEqualTo("WAITING_SUBPROCESS");
    assertThat(child.getStatus()).isEqualTo("WAITING_ACTIVITY");
  }

  /** Cria uma execução raiz sintética com todos os campos duráveis obrigatórios. */
  private ProcessRun run(long productId, String status) {
    var now = Instant.parse("2026-09-16T18:00:00Z");
    var run = new ProcessRun();
    run.setProductId(productId);
    run.setProcessDefinitionId(77L);
    run.setChainDefinitionId(14L);
    run.setScopeKey(java.util.UUID.randomUUID().toString());
    run.setSourceReference("experiment:" + productId);
    run.setStatus(status);
    run.setReason("Cenário local");
    run.setTotalActivities(8);
    run.setCompletedActivities(0);
    run.setRemainingActivities(8);
    run.setOmittedActivities(0);
    run.setRetryEpoch(0);
    run.setFailureCount(0);
    run.setCreatedAt(now);
    run.setUpdatedAt(now);
    run.setLastReconciledAt(now);
    return run;
  }
}
