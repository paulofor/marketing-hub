package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;

/** Responsabilidade: comprovar consultas e reservas do preflight com JPA e transações reais. */
class ExperimentTechnicalPreflightPersistenceTest {
  private static final String SOURCE = "experiment:97088";
  private final ObjectMapper json = new ObjectMapper();
  private final List<String> statements = new ArrayList<>();
  private final Map<String, BusinessProcessActivityDefinition> activities = new LinkedHashMap<>();
  private LocalContainerEntityManagerFactoryBean factory;
  private EntityManager manager;
  private TransactionTemplate reading;
  private TransactionTemplate writing;
  private BusinessProcessActivityInstanceRepository instances;
  private ExperimentTechnicalPreflightEvidenceService evidence;
  private ExperimentTechnicalPreflightActivityExecutor executor;
  private BusinessProcessDefinition process;
  private Product product;
  private String fingerprint = "local-proof-v1";
  private Long runId = 97016L;

  /** Reutiliza o banco descartável do runner Quartzo; nunca aceita uma conexão de produção. */
  @BeforeEach
  void preparePersistence() {
    String mysqlUrl = System.getenv("QUARTZO_MYSQL_URL");
    boolean mysql = mysqlUrl != null && !mysqlUrl.isBlank();
    if (mysql
        && !mysqlUrl.matches(
            "jdbc:mysql://(127\\.0\\.0\\.1|sandbox-docker):18307/quartzo_persistence_test(\\?.*)?"))
      throw new IllegalArgumentException("Use somente o schema local segregado de Quartzo.");
    factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(
        new DriverManagerDataSource(
            mysql ? mysqlUrl : "jdbc:h2:mem:preflight_persistence;DB_CLOSE_DELAY=-1",
            mysql ? "root" : "sa",
            mysql ? "cycles-root-local-only" : ""));
    factory.setManagedTypes(
        PersistenceManagedTypes.of(
            BusinessProcessDefinition.class.getName(),
            BusinessProcessActivityDefinition.class.getName(),
            BusinessProcessActivityInstance.class.getName()));
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setJpaPropertyMap(
        Map.of(
            "hibernate.hbm2ddl.auto",
            "create-drop",
            "hibernate.dialect",
            mysql
                ? "org.hibernate.community.dialect.MySQL57Dialect"
                : "org.hibernate.dialect.H2Dialect",
            "hibernate.session_factory.statement_inspector",
            (StatementInspector)
                sql -> {
                  statements.add(sql);
                  return sql;
                }));
    factory.afterPropertiesSet();
    manager = SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());
    instances =
        new JpaRepositoryFactory(manager)
            .getRepository(BusinessProcessActivityInstanceRepository.class);
    var transactions = new JpaTransactionManager(factory.getObject());
    reading = new TransactionTemplate(transactions);
    reading.setReadOnly(true);
    writing = new TransactionTemplate(transactions);
    writing.executeWithoutResult(
        status -> {
          process = new BusinessProcessDefinition();
          process.setProcessCode(ExperimentTechnicalPreflightEvidenceService.PROCESS_CODE);
          process.setName("Homologação técnica local");
          process.setPurpose("Validar persistência consultiva");
          process.setOwnerName("Backend");
          process.setTriggerDescription("Teste local");
          process.setOutcomeDescription("Prova transacional");
          process.setVersionNumber(1);
          process.setStatus("PUBLISHED");
          process.setDiagramJson("{}");
          process.setCreatedAt(Instant.now());
          manager.persist(process);
          for (String code :
              List.of("surfaces", "transaction", "measurement", "financialGuardrails")) {
            var activity = new BusinessProcessActivityDefinition();
            activity.setProcessDefinition(process);
            activity.setActivityId(code);
            activity.setName(code);
            activity.setDefinitionJson("{}");
            activity.setCreatedAt(Instant.now());
            manager.persist(activity);
            activities.put(code, activity);
          }
        });
    product = Product.builder().id(97007L).build();
    var predecessors = mock(ProductProcessActivityPredecessorService.class);
    when(predecessors.readiness(eq(process), any(), eq(SOURCE)))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Prova local"));
    evidence = mock(ExperimentTechnicalPreflightEvidenceService.class);
    when(evidence.referencedExperimentId(product, SOURCE)).thenReturn(97088L);
    when(evidence.evaluate(anyString(), eq(product), eq(SOURCE))).thenAnswer(invocation -> proof());
    executor =
        new ExperimentTechnicalPreflightActivityExecutor(predecessors, evidence, instances, json);
    statements.clear();
  }

  /** Remove somente as tabelas sintéticas deste teste e encerra a fábrica de persistência. */
  @AfterEach
  void closePersistence() {
    if (factory != null) factory.destroy();
  }

  /** A consulta inicial não reserva registros nem materializa atividades ausentes. */
  @Test
  void readsMissingOccurrencesWithoutLockingOrWriting() {
    reading.executeWithoutResult(
        status -> {
          activities.values().forEach(activity -> assertThat(fresh(activity)).isFalse());
          assertThat(instances.count()).isZero();
        });
    assertConsultativeSql();
  }

  /** Reconhece as quatro evidências atuais e conserva as ocorrências concluídas. */
  @Test
  void readsCurrentCompletedOccurrencesWithoutLocking() {
    activities.values().forEach(this::execute);
    statements.clear();
    reading.executeWithoutResult(
        status -> {
          activities.values().forEach(activity -> assertThat(fresh(activity)).isFalse());
          assertThat(instances.count()).isEqualTo(4);
        });
    assertConsultativeSql();
  }

  /** Uma fonte alterada ou um novo run exige renovação sem reescrever a prova anterior. */
  @Test
  void readsChangedFingerprintAndRunWithoutLosingHistory() {
    var activity = activities.get("surfaces");
    execute(activity);
    fingerprint = "local-proof-v2";
    statements.clear();
    reading.executeWithoutResult(status -> assertThat(fresh(activity)).isTrue());
    assertConsultativeSql();
    fingerprint = "local-proof-v1";
    runId = 97017L;
    statements.clear();
    reading.executeWithoutResult(
        status -> {
          assertThat(fresh(activity)).isTrue();
          var original = instances.findAll().getFirst();
          assertThat(original.getStatus()).isEqualTo("COMPLETED");
          assertThat(original.getObjectiveEvidenceJson()).contains("local-proof-v1", "97016");
          assertThat(instances.count()).isEqualTo(1);
        });
    assertConsultativeSql();
  }

  /** A espera conserva workspace e histórico sem lock ou atualização durante a leitura. */
  @Test
  void readsUnavailableEvidenceAsFreshExecutionRequired() {
    var activity = activities.get("financialGuardrails");
    execute(activity);
    when(evidence.evaluate(activity.getActivityId(), product, SOURCE))
        .thenThrow(new IllegalStateException("Plano financeiro local vencido."));
    statements.clear();
    reading.executeWithoutResult(
        status -> {
          assertThat(fresh(activity)).isTrue();
          var readiness = executor.readiness(process, activity, product, SOURCE);
          assertThat(readiness.ready()).isFalse();
          assertThat(readiness.workspaceCode()).isEqualTo("EXPERIMENT_PREFLIGHT");
          assertThat(readiness.workspaceReferenceId()).isEqualTo(97088L);
          assertThat(instances.count()).isEqualTo(1);
        });
    assertConsultativeSql();
  }

  /** Preserva a prova encerrada sem consultar fontes atuais ou aprovar a atividade ausente. */
  @Test
  void readsClosedHistoricalProofWithoutRevalidatingOrWriting() {
    var activity = activities.get("surfaces");
    execute(activity);
    String original =
        reading.execute(status -> instances.findAll().getFirst().getObjectiveEvidenceJson());
    when(evidence.executionBlockReason(product, SOURCE)).thenReturn("Experimento encerrado.");
    when(evidence.evaluate(anyString(), eq(product), eq(SOURCE)))
        .thenThrow(new IllegalStateException("A superfície atual mudou."));
    clearInvocations(evidence);
    statements.clear();
    reading.executeWithoutResult(
        status -> {
          assertThat(fresh(activity)).isFalse();
          assertThat(instances.findAll())
              .singleElement()
              .satisfies(
                  instance -> {
                    assertThat(instance.getObjectiveEvidenceJson()).isEqualTo(original);
                    assertThat(instance.isObjectiveAchieved()).isTrue();
                  });
          assertThat(
                  executor
                      .readiness(process, activities.get("financialGuardrails"), product, SOURCE)
                      .ready())
              .isFalse();
          assertThat(instances.count()).isEqualTo(1);
        });
    verify(evidence, never()).evaluate(anyString(), any(), anyString());
    assertConsultativeSql();
  }

  /** Concilia após cada comando, conserva o lock de escrita e evita custo ou registro duplicado. */
  @Test
  void renewsProofAndReconcilesAcrossTransactionsIdempotently() {
    var activity = activities.get("surfaces");
    execute(activity);
    statements.clear();
    execute(activity);
    assertThat(statements).anyMatch(sql -> sql.toLowerCase().contains("for update"));
    fingerprint = "local-proof-v2";
    execute(activity);
    statements.clear();
    reading.executeWithoutResult(
        status -> {
          assertThat(fresh(activity)).isFalse();
          var history = instances.findAll();
          assertThat(history).hasSize(2);
          assertThat(history).allSatisfy(item -> assertThat(item.getKnownCostUsd()).isZero());
          assertThat(history)
              .extracting(BusinessProcessActivityInstance::getOccurrenceNumber)
              .containsExactlyInAnyOrder(1, 2);
        });
    assertConsultativeSql();
  }

  /** Consulta o executor real na transação do relatório com o repositório JPA real. */
  private boolean fresh(BusinessProcessActivityDefinition activity) {
    return executor.requiresFreshExecution(process, activity, product, SOURCE);
  }

  /** Registra a prova pelo comando canônico dentro de uma transação de escrita. */
  private void execute(BusinessProcessActivityDefinition activity) {
    writing.executeWithoutResult(
        status ->
            assertThat(executor.execute(process, activity, product, SOURCE).objectiveAchieved())
                .isTrue());
  }

  /** Produz uma fonte sintética independente de IDs, modelos e pagamentos produtivos. */
  private ExperimentTechnicalPreflightEvidenceService.Evidence proof() {
    var payload = json.createObjectNode().put("runId", runId).put("inputFingerprint", fingerprint);
    return new ExperimentTechnicalPreflightEvidenceService.Evidence(
        97088L, runId, fingerprint, payload);
  }

  /** Rejeita locks e escritas mesmo quando o banco de desenvolvimento os tolera em leitura. */
  private void assertConsultativeSql() {
    assertThat(statements)
        .isNotEmpty()
        .allSatisfy(
            sql -> {
              String normalized = sql.stripLeading().toLowerCase();
              assertThat(normalized).startsWith("select ").doesNotContain("for update");
            });
  }
}
