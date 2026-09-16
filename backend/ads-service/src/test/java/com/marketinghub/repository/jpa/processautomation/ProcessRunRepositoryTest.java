package com.marketinghub.repository.jpa.processautomation;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/** Responsabilidade: comprovar quais execuções podem conciliar ou reservar a vez do produto. */
@DataJpaTest(showSql = false)
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.show-sql=false",
      "spring.datasource.url=jdbc:h2:mem:com.marketinghub.repository.jpa.processautomation.ProcessRunRepositoryTest-${random.uuid};DB_CLOSE_DELAY=0"
    })
class ProcessRunRepositoryTest {
  @Autowired private ProcessRunRepository repository;

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
