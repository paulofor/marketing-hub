package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.automation.v1.controller.ProcessRunController;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;

/** Responsabilidade: comprovar a causa da fila por HTTP sem alterar reservas, provas ou custos. */
@DataJpaTest(showSql = false)
@TestPropertySource(
    properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.show-sql=false",
      "spring.datasource.url=jdbc:h2:mem:ProcessRunQueueBlockerPersistenceTest-${random.uuid};DB_CLOSE_DELAY=0"
    })
class ProcessRunQueueBlockerPersistenceTest {
  @Autowired private ProcessRunRepository runs;
  @Autowired private ProcessRunEventRepository events;
  @Autowired private PlatformTransactionManager transactions;
  @Autowired private EntityManager entityManager;
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final ProcessRunContext context = mock(ProcessRunContext.class);
  private final BusinessProcessActivityExecutionService activities =
      mock(BusinessProcessActivityExecutionService.class);
  private ProcessRunService service;
  private MockMvc mvc;
  private ProcessRun waiting;
  private ProcessRun queued;

  /** Persiste duas referências distintas do mesmo produto em banco exclusivamente local. */
  @BeforeEach
  void setup() {
    waiting = runs.saveAndFlush(run(95001L, 95011L, "experiment:95021", "WAITING_INPUT"));
    waiting.setReason("Atualize o plano financeiro vencido ou alterado.");
    waiting.setCurrentActivityId("financialGuardrails");
    waiting.setCurrentActivityName("Validar limites financeiros persistidos");
    queued = runs.saveAndFlush(run(95001L, 95012L, "experiment:95022", "QUEUED"));
    var definition = new BusinessProcessDefinition();
    definition.setName("Homologação técnica local");
    definition.setVersionNumber(4);
    when(context.process(95011L)).thenReturn(definition);
    var navigation = new ProcessRunNavigation(null, null, null, null, json);
    var presentation = mock(ProcessRunNavigation.class);
    when(presentation.executionUrl(any()))
        .thenAnswer(inv -> navigation.executionUrl(inv.getArgument(0)));
    when(presentation.parents(any())).thenReturn(List.of());
    when(presentation.children(any())).thenReturn(List.of());
    service =
        new ProcessRunService(
            runs,
            events,
            mock(ProductRepository.class),
            context,
            presentation,
            mock(ProcessRunSubprocesses.class),
            mock(ProcessRunGuidance.class),
            activities,
            json,
            transactions);
    mvc =
        MockMvcBuilders.standaloneSetup(new ProcessRunController(service, "qa-only", ""))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
            .build();
    entityManager.flush();
  }

  /** A consulta revela a espera financeira anterior sem alterar aprovação, fila ou consumo. */
  @Test
  void exposesFinancialReservationAndExactReferenceWithoutWrites() throws Exception {
    var revision = waiting.getRevision();
    for (int i = 0; i < 2; i++) {
      mvc.perform(
              get("/api/business-processes/95012/products/95001/automation/v1")
                  .param("chainId", "95014")
                  .param("sourceReference", "experiment:95022"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("QUEUED"))
          .andExpect(jsonPath("$.queueBlocker.runId").value(waiting.getId().intValue()))
          .andExpect(jsonPath("$.queueBlocker.sourceReference").value("experiment:95021"))
          .andExpect(jsonPath("$.queueBlocker.reason").value(waiting.getReason()))
          .andExpect(jsonPath("$.queueBlocker.processVersion").value(4))
          .andExpect(
              jsonPath("$.queueBlocker.navigationUrl")
                  .value(
                      "/products/95001/value-chain-history/processes/95011/activities?chainId=95014&sourceReference=experiment%3A95021#activity-financialGuardrails"));
    }
    entityManager.flush();
    entityManager.clear();
    var persisted = runs.findById(waiting.getId()).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo("WAITING_INPUT");
    assertThat(persisted.getRevision()).isEqualTo(revision);
    assertThat(persisted.getKnownCostUsd()).isEqualByComparingTo("0.25");
    assertThat(persisted.getCompletedActivities()).isEqualTo(3);
    assertThat(events.count()).isZero();
    verifyNoInteractions(activities);
  }

  /** Estados terminais ou pausados não reservam a fila nem aparecem como dependência atual. */
  @ParameterizedTest
  @ValueSource(strings = {"PAUSED", "COMPLETED", "CLOSED", "ERROR"})
  void removesReservationAfterPersistedRelease(String released) {
    waiting.setStatus(released);
    assertThat(readStatus().queueBlocker()).isNull();
    verifyNoInteractions(activities);
  }

  /** Reservas de outro produto não são expostas, mesmo quando seu identificador é anterior. */
  @Test
  void isolatesProductsAndPreservesQueueStatusUntilReconciliation() {
    waiting.setProductId(95002L);
    var result = readStatus();
    assertThat(result.queueBlocker()).isNull();
    assertThat(result.status()).isEqualTo("QUEUED");
    assertThat(events.count()).isZero();
  }

  /** A dependência comercial já permitida permanece livre e a leitura não atualiza contagens. */
  @Test
  void preservesCommercialContinuationWithoutDirtyingObservedRoot() throws Exception {
    when(context.permitsCommercialContinuation(any(), any())).thenReturn(true);
    var snapshot =
        json.readValue(
            """
        {"selectedActivityCount":8,"completedActivityCount":7,"remainingActivityCount":1,"activities":[]}
        """,
            ProductProcessActivityExecutionHistoryResponse.class);
    when(context.read(any(ProcessRun.class), eq(false))).thenReturn(snapshot);
    assertThat(readStatus().queueBlocker()).isNull();
    entityManager.flush();
    entityManager.clear();
    assertThat(runs.findById(waiting.getId()).orElseThrow().getCompletedActivities()).isEqualTo(3);
    assertThat(events.count()).isZero();
  }

  /** Uma raiz própria compartilha a vez com seus filhos, sem apontar a si mesma como bloqueio. */
  @Test
  void sharesReservationWithinSameRoot() {
    queued.setParentRunId(waiting.getId());
    assertThat(readStatus().queueBlocker()).isNull();
  }

  /** Trabalho real impede ultrapassagem mesmo na exceção comercial, sem gravar a observação. */
  @Test
  void exposesInFlightReservationWithoutUpdatingProgress() throws Exception {
    when(context.permitsCommercialContinuation(any(), any())).thenReturn(true);
    when(context.read(any(ProcessRun.class), eq(false)))
        .thenReturn(
            json.readValue(
                """
        {"selectedActivityCount":8,"completedActivityCount":7,"remainingActivityCount":1,
         "activities":[{"activityId":"other","operationalState":"PENDING","tasks":[]}]}
        """,
                ProductProcessActivityExecutionHistoryResponse.class));
    assertThat(readStatus().queueBlocker()).isNotNull();
    entityManager.flush();
    entityManager.clear();
    assertThat(runs.findById(waiting.getId()).orElseThrow().getCompletedActivities()).isEqualTo(3);
    assertThat(events.count()).isZero();
    verifyNoInteractions(activities);
  }

  /** Consulta o serviço no escopo idempotente gravado, sem retomar nem conciliar processos. */
  private com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunResponse
      readStatus() {
    return service.status(95001L, 95012L, new ProcessRunCommand(95014L, null, "experiment:95022"));
  }

  /** Preenche o contrato obrigatório de uma execução sintética segregada da operação real. */
  private ProcessRun run(long product, long process, String reference, String status) {
    var run = new ProcessRun();
    run.setProductId(product);
    run.setProcessDefinitionId(process);
    run.setChainDefinitionId(95014L);
    run.setSourceReference(reference);
    try {
      run.setScopeKey(
          HexFormat.of()
              .formatHex(
                  java.security.MessageDigest.getInstance("SHA-256")
                      .digest(
                          (product + "|" + process + "|95014|null|" + reference)
                              .getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    } catch (java.security.NoSuchAlgorithmException ex) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .error("Falha ao identificar execução local", ex);
      throw new IllegalStateException(ex);
    }
    run.setStatus(status);
    run.setReason("Aguardando a fila local.");
    run.setTotalActivities(4);
    run.setCompletedActivities(3);
    run.setRemainingActivities(1);
    run.setKnownCostUsd(new BigDecimal("0.25"));
    run.setCreatedAt(Instant.parse("2026-10-03T12:00:00Z"));
    run.setUpdatedAt(run.getCreatedAt());
    run.setLastReconciledAt(run.getCreatedAt());
    return run;
  }
}
