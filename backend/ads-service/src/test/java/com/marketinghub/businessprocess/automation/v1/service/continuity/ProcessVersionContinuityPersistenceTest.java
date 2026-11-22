package com.marketinghub.businessprocess.automation.v1.service.continuity;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.*;
import com.marketinghub.repository.jpa.processautomation.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

/** Responsabilidade: comprovar persistência, isolamento e rollback do diário de continuidade. */
@DataJpaTest(
    showSql = false,
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@ContextConfiguration(classes = ProcessVersionContinuityPersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProcessVersionContinuityPersistenceTest {
  /** Responsabilidade: carregar somente o armazenamento do motor de processos em H2 local. */
  @Configuration
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = ProcessRun.class)
  @EnableJpaRepositories(basePackageClasses = ProcessRunRepository.class)
  static class Config {}

  @Autowired ProcessRunRepository runs;
  @Autowired ProcessRunEventRepository events;
  @Autowired PlatformTransactionManager manager;

  /** Reabre recibo persistido, rejeita outro contexto e reverte gravação incompleta. */
  @Test
  void persistsScopedReceiptAndRollsBackFailure() {
    var tx = new TransactionTemplate(manager);
    var id =
        tx.execute(
            ignored -> {
              var source = new ProcessRun();
              source.setProductId(4L);
              source.setProcessDefinitionId(117L);
              source.setChainDefinitionId(27L);
              source.setLearningCycleId(22L);
              source.setSourceReference("experiment:115");
              source.setScopeKey("synthetic-continuity-rollback");
              source.setStatus("PAUSED");
              source.setReason("Custos preservados");
              source.setKnownCostUsd(new BigDecimal("113.33"));
              source.setCreatedAt(Instant.now());
              source.setUpdatedAt(Instant.now());
              source.setLastReconciledAt(Instant.now());
              return runs.saveAndFlush(source).getId();
            });
    assertThatThrownBy(
            () ->
                tx.execute(
                    ignored -> {
                      events.saveAndFlush(event(id));
                      throw new IllegalStateException("Falha sintética antes do commit");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(events.continuations(4L, 22L, "experiment:115")).isEmpty();
    tx.execute(ignored -> events.saveAndFlush(event(id)));
    var lookup = new ProcessVersionContinuityLookup(events, new ObjectMapper());
    assertThat(lookup.permits(4L, 22L, "experiment:115", 27L, 127L)).isTrue();
    assertThat(lookup.permits(71L, 22L, "experiment:115", 27L, 127L)).isFalse();
    assertThat(lookup.permits(4L, 99L, "experiment:115", 27L, 127L)).isFalse();
    assertThat(lookup.permits(4L, 22L, "experiment:116", 27L, 127L)).isFalse();
    assertThat(lookup.permits(4L, 22L, "experiment:115", 28L, 127L)).isFalse();
    var original = runs.findById(id).orElseThrow();
    assertThat(original.getStatus()).isEqualTo("PAUSED");
    assertThat(original.getKnownCostUsd()).isEqualByComparingTo("113.33");
  }

  /** Monta recibo sintético sem credenciais, gastos, tarefas ou efeitos externos. */
  private ProcessRunEvent event(Long id) {
    var event = new ProcessRunEvent();
    event.setRunId(id);
    event.setEventType("VERSION_CONTINUATION");
    event.setStatus("PAUSED");
    event.setMessage("Conferência sintética");
    event.setCreatedAt(Instant.now());
    event.setDetailsJson(
        "{\"binding\":{\"sourceProcessId\":117,\"targetProcessId\":127,\"successorRunId\":800,\"chainId\":27},\"reused\":[]}");
    return event;
  }
}
