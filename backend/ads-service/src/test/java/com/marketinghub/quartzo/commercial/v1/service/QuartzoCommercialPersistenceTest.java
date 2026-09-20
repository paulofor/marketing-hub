package com.marketinghub.quartzo.commercial.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskReviewSnapshot;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

/**
 * Responsabilidade: comprovar leitura, identidade e reservas das evidências Quartzo persistidas.
 */
class QuartzoCommercialPersistenceTest {
  private static final String SOURCE = "experiment:92088";
  private final ObjectMapper json = new ObjectMapper();
  private final List<String> statements = new ArrayList<>();
  private final Map<String, BusinessProcessActivityDefinition> activities = new LinkedHashMap<>();
  private LocalContainerEntityManagerFactoryBean factory;
  private EntityManager manager;
  private TransactionTemplate reading;
  private TransactionTemplate writing;
  private BusinessProcessActivityInstanceRepository instances;
  private QuartzoCommercialService service;
  private QuartzoCommercialAgentReadiness readiness;
  private QuartzoCommercialContext.Scope scope;
  private Product product;
  private BusinessProcessDefinition process;
  private BusinessProcessDefinition parent;
  private BusinessProcessActivityDefinition parentActivity;
  private ObjectNode snapshot;

  /** Inicializa entidades e transações reais exclusivamente em banco local descartável. */
  @BeforeEach
  void preparePersistence() throws Exception {
    String mysqlUrl = System.getenv("QUARTZO_MYSQL_URL");
    boolean mysql = mysqlUrl != null && !mysqlUrl.isBlank();
    if (mysql
        && !mysqlUrl.matches(
            "jdbc:mysql://(127\\.0\\.0\\.1|sandbox-docker):18307/quartzo_persistence_test(\\?.*)?"))
      throw new IllegalArgumentException("Use somente o schema local segregado de Quartzo.");
    factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(
        new DriverManagerDataSource(
            mysql ? mysqlUrl : "jdbc:h2:mem:quartzo_persistence;DB_CLOSE_DELAY=-1",
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
    var repositories = new JpaRepositoryFactory(manager);
    instances = repositories.getRepository(BusinessProcessActivityInstanceRepository.class);
    var definitions = repositories.getRepository(BusinessProcessActivityDefinitionRepository.class);
    var transactions = new JpaTransactionManager(factory.getObject());
    reading = new TransactionTemplate(transactions);
    reading.setReadOnly(true);
    writing = new TransactionTemplate(transactions);
    writing.executeWithoutResult(
        status -> {
          process = process(QuartzoCommercialContext.CODE, 1);
          parent = process("pde-commercial-homologation-activation", 8);
          parentActivity = activity(parent, "commercialPreparation");
          var steps = new ArrayList<>(QuartzoCommercialChecks.PREPARATION);
          steps.addAll(QuartzoCommercialService.REVIEWS);
          steps.add("ready");
          steps.forEach(step -> activities.put(step, activity(process, step)));
        });
    product = Product.builder().id(92007L).build();
    var experiment = new Experiment();
    experiment.setId(92088L);
    experiment.setProduct(product);
    scope = new QuartzoCommercialContext.Scope(experiment, product, "local-v1", null, null, null);
    snapshot =
        (ObjectNode)
            json.readTree(
                """
        {"productId":92007,"experimentId":92088,"productVersion":"local-v1","fingerprint":"local-proof",
         "destinationUrl":"https://example.test/kit","checkoutUrl":"https://example.test/pay"}
        """);
    snapshot.put("productId", product.getId());
    snapshot.put("experimentId", experiment.getId());
    var context = mock(QuartzoCommercialContext.class);
    when(context.applies(product)).thenReturn(true);
    when(context.scope(eq(SOURCE), any(), anyBoolean())).thenReturn(scope);
    when(context.snapshot(SOURCE)).thenAnswer(invocation -> snapshot.deepCopy());
    when(context.read(anyString()))
        .thenAnswer(invocation -> json.readTree(invocation.getArgument(0, String.class)));
    var processes = mock(BusinessProcessDefinitionRepository.class);
    when(processes.findByProcessCodeAndVersionNumber(QuartzoCommercialContext.CODE, 1))
        .thenReturn(Optional.of(process));
    var predecessors = mock(ProductProcessActivityPredecessorService.class);
    when(predecessors.readiness(any(), any(), eq(SOURCE)))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Prova local"));
    var tasks = mock(AgentTaskRepository.class);
    var result = json.createObjectNode().put("decision", "APPROVED");
    result.putArray("requiredChanges");
    result.putArray("evidence").add("Prova sintética local");
    var gates = result.putArray("gateChecks");
    QuartzoCommercialService.REVIEW_GATES.forEach(
        gate ->
            gates
                .addObject()
                .put("gate", gate)
                .put("status", "PASS")
                .put("evidence", "Prova local"));
    var evidence = json.createObjectNode().set("quartzoScope", snapshot.deepCopy());
    when(tasks.findLatestReviewSnapshots(eq(process.getId()), eq(SOURCE), anyString(), any()))
        .thenReturn(
            List.of(
                new AgentTaskReviewSnapshot(
                    92001L, "COMPLETED", evidence.toString(), result.toString())));
    service =
        new QuartzoCommercialService(
            context,
            mock(QuartzoCommercialChecks.class),
            instances,
            definitions,
            processes,
            tasks,
            mock(ExperimentRepository.class),
            predecessors);
    readiness = new QuartzoCommercialAgentReadiness(context, service, instances);
    statements.clear();
  }

  /** Fecha a persistência e remove apenas as três tabelas do schema sintético local. */
  @AfterEach
  void closePersistence() {
    if (factory != null) factory.destroy();
  }

  /** Reproduz a leitura inicial do pai e do filho sem qualquer ocorrência registrada. */
  @Test
  void readsInitialParentAndChildWithoutLocking() {
    reading.executeWithoutResult(
        status -> {
          assertThat(readiness.requiresFreshExecution(parent, parentActivity, product, SOURCE))
              .isFalse();
          activities
              .values()
              .forEach(
                  activity ->
                      assertThat(
                              readiness.requiresFreshExecution(process, activity, product, SOURCE))
                          .isFalse());
          assertThat(instances.count()).isZero();
        });
    assertReadsWithoutLocks();
  }

  /** Mantém consulta utilizável ao reconhecer prova atual ou invalidar uma fonte alterada. */
  @Test
  void readsExistingAndStaleProofWithoutLocking() {
    persist(activities.get("entry"));
    reading.executeWithoutResult(
        status ->
            assertThat(
                    readiness.requiresFreshExecution(
                        process, activities.get("entry"), product, SOURCE))
                .isFalse());
    snapshot.put("fingerprint", "changed-local-proof");
    reading.executeWithoutResult(
        status -> {
          assertThat(
                  readiness.requiresFreshExecution(
                      process, activities.get("entry"), product, SOURCE))
              .isTrue();
          assertThat(instances.count()).isEqualTo(1);
        });
    assertReadsWithoutLocks();
  }

  /** Revalida os cinco comprovantes e libera as revisões em transação somente leitura. */
  @Test
  void readsPreparationBeforeReviewsWithoutLocking() {
    QuartzoCommercialChecks.PREPARATION.forEach(step -> persist(activities.get(step)));
    reading.executeWithoutResult(
        status -> {
          service.prepared(process, scope, SOURCE, snapshot);
          for (String review : QuartzoCommercialService.REVIEWS)
            assertThat(
                    readiness.readiness(process, activities.get(review), product, SOURCE).ready())
                .isTrue();
          assertThat(service.readiness(process, activities.get("ready"), product, SOURCE).ready())
              .isTrue();
          assertThat(instances.count()).isEqualTo(5);
        });
    assertReadsWithoutLocks();
  }

  /** Reutiliza a conclusão e os pareceres no pai sem reservar registros para escrita. */
  @Test
  void readsCompletedChildAndParentWithoutLocking() {
    persist(activities.get("ready"));
    persist(parentActivity);
    reading.executeWithoutResult(
        status -> {
          assertThat(service.completed(product, SOURCE)).isTrue();
          assertThat(readiness.requiresFreshExecution(parent, parentActivity, product, SOURCE))
              .isFalse();
          assertThat(instances.count()).isEqualTo(2);
        });
    assertReadsWithoutLocks();
  }

  /** A repetição do comando mantém a reserva pessimista e não duplica o comprovante. */
  @Test
  void reservesExistingOccurrenceWhenExecutingCommand() {
    persist(activities.get("entry"));
    writing.executeWithoutResult(
        status -> {
          assertThat(
                  service
                      .execute(process, activities.get("entry"), product, SOURCE)
                      .objectiveAchieved())
              .isTrue();
          assertThat(instances.count()).isEqualTo(1);
        });
    assertThat(statements).anyMatch(sql -> sql.toLowerCase().contains("for update"));
  }

  /** Reconcilia cada comando em outra transação e reaproveita o filho sem perder sua identidade. */
  @Test
  void preservesProgressAcrossCommandAndReconciliationTransactions() {
    var steps = new ArrayList<>(QuartzoCommercialChecks.PREPARATION);
    steps.add("ready");
    for (String step : steps) {
      var activity = activities.get(step);
      writing.executeWithoutResult(
          status ->
              assertThat(service.execute(process, activity, product, SOURCE).objectiveAchieved())
                  .isTrue());
      statements.clear();
      reading.executeWithoutResult(
          status ->
              assertThat(readiness.requiresFreshExecution(process, activity, product, SOURCE))
                  .isFalse());
      assertReadsWithoutLocks();
    }
    reading.executeWithoutResult(status -> assertThat(service.completed(product, SOURCE)).isTrue());
    writing.executeWithoutResult(
        status -> {
          steps.forEach(step -> service.execute(process, activities.get(step), product, SOURCE));
          assertThat(instances.count()).isEqualTo(6);
        });
  }

  /** Registra a identidade mínima do processo isolado, sem consultar dados de produção. */
  private BusinessProcessDefinition process(String code, int version) {
    var result = new BusinessProcessDefinition();
    result.setProcessCode(code);
    result.setName("Preparação local");
    result.setPurpose("Validar transações");
    result.setOwnerName("Backend");
    result.setTriggerDescription("Teste local");
    result.setOutcomeDescription("Prova transacional");
    result.setVersionNumber(version);
    result.setStatus("PUBLISHED");
    result.setDiagramJson("{}");
    result.setCreatedAt(Instant.now());
    manager.persist(result);
    return result;
  }

  /** Registra a etapa na mesma persistência usada pelo repositório real de ocorrências. */
  private BusinessProcessActivityDefinition activity(BusinessProcessDefinition owner, String step) {
    var result = new BusinessProcessActivityDefinition();
    result.setProcessDefinition(owner);
    result.setActivityId(step);
    result.setName(step);
    result.setDefinitionJson("{}");
    result.setCreatedAt(Instant.now());
    manager.persist(result);
    return result;
  }

  /**
   * Persiste prova local antes da leitura e exclui seus comandos da auditoria de SQL consultivo.
   */
  private void persist(BusinessProcessActivityDefinition activity) {
    writing.executeWithoutResult(
        status -> {
          var proof = new BusinessProcessActivityInstance();
          proof.setActivityDefinition(activity);
          proof.setSourceReference(SOURCE);
          proof.setOccurrenceNumber(1);
          proof.setStatus("COMPLETED");
          proof.setObjectiveAchieved(true);
          proof.setObjectiveEvidenceJson(snapshot.toString());
          proof.setEnteredAt(Instant.now());
          proof.setCreatedAt(Instant.now());
          proof.setUpdatedAt(Instant.now());
          proof.setCostCoverage("COMPLETE");
          proof.setEvidenceQuality("DIRECT");
          instances.saveAndFlush(proof);
        });
    statements.clear();
  }

  /** Impede regressão mesmo quando um banco de desenvolvimento aceita lock em consulta. */
  private void assertReadsWithoutLocks() {
    assertThat(statements).isNotEmpty().noneMatch(sql -> sql.toLowerCase().contains("for update"));
  }
}
