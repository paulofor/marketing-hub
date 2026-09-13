package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

/** Responsabilidade: comprovar a separação física entre consulta privada e reserva de escrita. */
class PrivateCommunicationJourneyPersistenceTest {
  private final List<String> statements = new ArrayList<>();
  private LocalContainerEntityManagerFactoryBean factory;
  private BusinessProcessActivityInstanceRepository instances;
  private TransactionTemplate reading;
  private TransactionTemplate writing;
  private PrivateCommunicationJourney journey;
  private BusinessProcessDefinition process;
  private BusinessProcessActivityDefinition activity;
  private Product product;
  private LearningSalesCycle cycle;

  /** Usa três entidades reais e banco descartável, sem importar configurações de produção. */
  @BeforeEach
  void preparePersistence() {
    boolean mysql = Boolean.getBoolean("private.journey.mysql57");
    String host = System.getenv().getOrDefault("PROCESS_TEST_DB_HOST", "sandbox-docker");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host))
      throw new IllegalArgumentException("Host de homologação inválido.");
    var source =
        new DriverManagerDataSource(
            mysql
                ? "jdbc:mysql://"
                    + host
                    + ":18312/private_journey_local?useSSL=false&serverTimezone=UTC"
                : "jdbc:h2:mem:private_journey_persistence;DB_CLOSE_DELAY=-1",
            mysql ? "root" : "sa",
            mysql ? "process-local-only" : "");
    factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
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
    var manager = SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());
    instances =
        new JpaRepositoryFactory(manager)
            .getRepository(BusinessProcessActivityInstanceRepository.class);
    var transactions = new JpaTransactionManager(factory.getObject());
    reading = new TransactionTemplate(transactions);
    reading.setReadOnly(true);
    writing = new TransactionTemplate(transactions);
    var cycles = mock(IrisLearningCycleContext.class);
    var cycleRepository = mock(LearningSalesCycleRepository.class);
    cycle = new LearningSalesCycle();
    cycle.setProductId(92041L);
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    when(cycleRepository.findByExperimentId(92041L)).thenReturn(Optional.of(cycle));
    when(cycles.resolve("experiment:92041")).thenReturn(Optional.empty());
    journey =
        new PrivateCommunicationJourney(
            cycles,
            cycleRepository,
            mock(ProductProcessActivityPredecessorService.class),
            instances,
            mock(AgentTaskRepository.class),
            mock(PrivateCommunicationCreativeProof.class),
            new ObjectMapper());
    product = new Product();
    product.setId(92041L);
    writing.executeWithoutResult(
        status -> {
          process = new BusinessProcessDefinition();
          process.setProcessCode("pde-communication-sales-journey");
          process.setName("Comunicação privada local");
          process.setPurpose("Validar leitura e escrita.");
          process.setOwnerName("Backend");
          process.setTriggerDescription("Homologação local");
          process.setOutcomeDescription("Transações segregadas");
          process.setVersionNumber(1);
          process.setStatus("PUBLISHED");
          process.setDiagramJson("{}");
          process.setCreatedAt(Instant.now());
          manager.persist(process);
          activity = new BusinessProcessActivityDefinition();
          activity.setProcessDefinition(process);
          activity.setActivityId("destination");
          activity.setName("Confirmar destino");
          activity.setDefinitionJson("{}");
          activity.setCreatedAt(Instant.now());
          manager.persist(activity);
        });
    statements.clear();
  }

  /** Libera conexões e remove somente o schema das três entidades de teste. */
  @AfterEach
  void closePersistence() {
    if (factory != null) factory.destroy();
  }

  /** A ausência de contexto vira pendência funcional, sem invalidar a transação de consulta. */
  @Test
  void readsReadinessWithoutRequestingAWriteLock() {
    reading.executeWithoutResult(
        status -> {
          var result = journey.readiness(process, activity, product, "experiment:92041");
          assertThat(result.reason()).isEqualTo("O contexto privado não foi encontrado.");
          assertThat(instances.count()).isZero();
        });
    assertThat(statements).noneMatch(sql -> sql.toLowerCase().contains("for update"));
  }

  /** Uma prova arquivada continua consultável em transação somente leitura no MySQL 5.7. */
  @Test
  void readsCompletedObjectiveWithoutLockingItsOccurrence() {
    persistCompletedOccurrence();
    cycle.setStatus("ADJUSTED");
    statements.clear();
    Boolean stale =
        reading.execute(status -> journey.stale(process, activity, product, "experiment:92041"));
    assertThat(stale).isFalse();
    assertThat(statements).noneMatch(sql -> sql.toLowerCase().contains("for update"));
  }

  /** A reserva usada pelas gravações conserva o bloqueio pessimista da ocorrência existente. */
  @Test
  void preservesWriteReservationForExistingOccurrence() {
    persistCompletedOccurrence();
    statements.clear();
    writing.executeWithoutResult(
        status ->
            assertThat(
                    instances
                        .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                            activity.getId(), "experiment:92041"))
                .isPresent());
    assertThat(statements).anyMatch(sql -> sql.toLowerCase().contains("for update"));
  }

  /** Grava uma prova sintética com identidade completa, separada de produtos e métricas reais. */
  private void persistCompletedOccurrence() {
    writing.executeWithoutResult(
        status -> {
          var instance = new BusinessProcessActivityInstance();
          instance.setActivityDefinition(activity);
          instance.setSourceReference("experiment:92041");
          instance.setOccurrenceNumber(1);
          instance.setStatus("COMPLETED");
          instance.setObjectiveAchieved(true);
          instance.setObjectiveEvidenceJson(
              "{\"evidenceType\":\"PDE_COMMUNICATION_PRIVATE_DESTINATION_V1\",\"productId\":92041,\"sourceReference\":\"experiment:92041\",\"prototypeVersion\":\"local-v1\",\"destinationUrl\":\"https://example.test/private\"}");
          instance.setEnteredAt(Instant.now());
          instance.setCreatedAt(Instant.now());
          instance.setUpdatedAt(Instant.now());
          instance.setCostCoverage("COMPLETE");
          instance.setEvidenceQuality("DIRECT");
          instances.saveAndFlush(instance);
        });
  }
}
