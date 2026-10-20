package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.sql.SQLTransactionRollbackException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Comprova que disputa transitória não inventa pendência humana nem reescreve decisão concorrente.
 */
@DataJpaTest(showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.show-sql=false",
      "spring.datasource.url=jdbc:h2:mem:ProcessRunLockContentionPersistenceTest-${random.uuid};DB_CLOSE_DELAY=0"
    })
class ProcessRunLockContentionPersistenceTest {
  @Autowired ProcessRunRepository runs;
  @Autowired ProcessRunEventRepository events;
  @Autowired PlatformTransactionManager manager;
  final ProductRepository products = mock(ProductRepository.class);
  final ProcessRunContext context = mock(ProcessRunContext.class);
  final ProcessRunNavigation navigation = mock(ProcessRunNavigation.class);
  final ProcessRunSubprocesses subprocesses = mock(ProcessRunSubprocesses.class);
  final ProcessRunGuidance guidance = mock(ProcessRunGuidance.class);
  final BusinessProcessActivityExecutionService activities =
      mock(BusinessProcessActivityExecutionService.class);
  ProcessRunService service;
  TransactionTemplate transaction;

  /** Usa transações efetivas independentes; os ports de leitura e execução não geram trabalho. */
  @BeforeEach
  void setup() {
    transaction = new TransactionTemplate(manager);
    service =
        new ProcessRunService(
            runs,
            events,
            products,
            context,
            navigation,
            subprocesses,
            guidance,
            activities,
            new ObjectMapper().findAndRegisterModules(),
            manager);
    when(products.findById(anyLong()))
        .thenAnswer(i -> Optional.of(Product.builder().id(i.getArgument(0)).build()));
  }

  /** Duas identidades conservam estado, custo, falhas e revisão após o SQL 1205 confirmado. */
  @ParameterizedTest
  @ValueSource(longs = {7L, 9007L})
  void keepsDurableProgressEligibleForWorkerRetry(long productId) {
    var run = persist(productId);
    when(products.findLockedById(productId)).thenThrow(contention());

    var result = service.reconcile(run.getId());

    assertThat(result.status()).isEqualTo("WAITING_ACTIVITY");
    var saved = runs.findById(run.getId()).orElseThrow();
    assertThat(saved.getFailureCount()).isZero();
    assertThat(saved.getRevision()).isEqualTo(run.getRevision());
    assertThat(saved.getKnownCostUsd()).isEqualByComparingTo("10.622772");
    assertThat(saved.getCurrentActivityId()).isEqualTo("targeting");
    assertThat(
            events.history(
                run.getId(), null, org.springframework.data.domain.PageRequest.of(0, 20)))
        .isEmpty();
    verify(products, times(1)).findLockedById(productId);
    verifyNoInteractions(activities, context);
  }

  /** Uma pausa gravada por outra transação prevalece sem reativação nem nova reserva. */
  @Test
  void preservesPauseCommittedDuringContention() {
    var run = persist(7007L);
    var independent = new TransactionTemplate(manager);
    independent.setPropagationBehavior(
        org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    when(products.findLockedById(7007L))
        .thenAnswer(
            ignored -> {
              independent.executeWithoutResult(
                  status -> {
                    var current = runs.findById(run.getId()).orElseThrow();
                    current.setStatus("PAUSED");
                    current.setReason("Pausa registrada pelo operador");
                    runs.saveAndFlush(current);
                  });
              throw contention();
            });

    assertThat(service.reconcile(run.getId()).status()).isEqualTo("PAUSED");
    assertThat(runs.findById(run.getId()).orElseThrow().getStatus()).isEqualTo("PAUSED");
    verify(products, times(1)).findLockedById(7007L);
    verifyNoInteractions(activities);
  }

  /** Erro técnico não transitório mantém o tratamento de falha existente e seu evento. */
  @Test
  void retainsFailureHandlingForProgrammingErrors() {
    var run = persist(8007L);
    when(products.findLockedById(8007L))
        .thenThrow(new IllegalStateException("Falha técnica de fixture"))
        .thenReturn(Optional.of(Product.builder().id(8007L).build()));

    assertThat(service.reconcile(run.getId()).status()).isEqualTo("WAITING_INPUT");
    assertThat(runs.findById(run.getId()).orElseThrow().getFailureCount()).isEqualTo(1);
    assertThat(
            events.history(
                run.getId(), null, org.springframework.data.domain.PageRequest.of(0, 20)))
        .extracting(event -> event.getEventType())
        .containsExactly("RECONCILIATION_FAILED");
    verifyNoInteractions(activities);
  }

  /** Persiste um resultado de atividade já solicitado, com custo e contexto conservados. */
  private ProcessRun persist(long productId) {
    return transaction.execute(
        ignored -> {
          var run = new ProcessRun();
          run.setProductId(productId);
          run.setProcessDefinitionId(81L);
          run.setChainDefinitionId(26L);
          run.setLearningCycleId(productId + 100);
          run.setScopeKey(UUID.randomUUID().toString());
          run.setSourceReference("experiment:" + (productId + 200));
          run.setStatus("WAITING_ACTIVITY");
          run.setReason("Resultado persistido aguarda conciliação");
          run.setCurrentActivityId("targeting");
          run.setKnownCostUsd(new BigDecimal("10.622772"));
          run.setCostCoverage("PARTIAL");
          run.setCreatedAt(Instant.parse("2026-10-20T12:00:00Z"));
          run.setUpdatedAt(run.getCreatedAt());
          run.setLastReconciledAt(run.getCreatedAt());
          return runs.saveAndFlush(run);
        });
  }

  /** Reproduz a tradução Spring do timeout de lock observado no MySQL 5.7 operacional. */
  private CannotAcquireLockException contention() {
    return new CannotAcquireLockException(
        "Lock wait timeout exceeded",
        new SQLTransactionRollbackException("Lock wait timeout exceeded", "40001", 1205));
  }
}
