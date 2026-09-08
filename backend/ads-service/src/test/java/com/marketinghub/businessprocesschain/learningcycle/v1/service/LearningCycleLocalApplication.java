package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.controller.LearningCycleController;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.run.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.*;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: subir o fluxo local com persistência real e dependências externas simuladas.
 */
@TestConfiguration
@Profile("learning-cycles-fixture")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({
  LearningCycleService.class,
  LearningCycleOrganization.class,
  com.marketinghub.businessprocesschain.service.BusinessProcessChainService.class,
  com.marketinghub.businessprocesschain.controller.BusinessProcessChainController.class,
  BusinessProcessDefinitionService.class,
  BusinessProcessDefinitionController.class,
  com.marketinghub.businessprocesscomposition.service.BusinessProcessCompositionService.class,
  com.marketinghub.businessprocesscomposition.controller.BusinessProcessCompositionController.class,
  LearningCycleJson.class,
  LearningCycleEvidence.class,
  LearningCycleVideoEvidence.class,
  LearningCycleVideoFixtures.class,
  LearningCycleBpmLedger.class,
  LearningCycleController.class
})
public class LearningCycleLocalApplication {
  static final Map<Long, Experiment> EXPERIMENTS = new ConcurrentHashMap<>();
  static final Map<Long, ExperimentRun> RUNS = new ConcurrentHashMap<>();

  /** Inicia somente a fixture local, sem importar executores, agendamentos ou credenciais reais. */
  public static void main(String[] args) {
    start();
  }

  /** Fixa os parâmetros locais acima de configurações e variáveis produtivas. */
  static org.springframework.context.ConfigurableApplicationContext start() {
    SpringApplication app = new SpringApplication(LearningCycleLocalApplication.class);
    String[] args =
        properties().entrySet().stream()
            .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
            .toArray(String[]::new);
    return app.run(args);
  }

  /** Declara banco de teste, migração exata e servidor local com erros contratuais legíveis. */
  static Map<String, Object> properties() {
    String host = System.getenv().getOrDefault("LEARNING_CYCLES_DB_HOST", "127.0.0.1");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host))
      throw new IllegalArgumentException("Host externo proibido na fixture.");
    return Map.ofEntries(
        Map.entry(
            "spring.config.location",
            "optional:classpath:learningcycle/no-production-config.properties"),
        Map.entry("spring.profiles.active", "learning-cycles-fixture"),
        Map.entry(
            "spring.datasource.url",
            "jdbc:mysql://"
                + host
                + ":18307/learning_cycles_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"),
        Map.entry("spring.datasource.username", "cycles_local"),
        Map.entry("spring.datasource.password", "cycles-local-only"),
        Map.entry("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver"),
        Map.entry("spring.sql.init.mode", "never"),
        Map.entry("spring.sql.init.schema-locations", "classpath:learningcycle/baseline.sql"),
        Map.entry("spring.liquibase.change-log", "classpath:learningcycle/changelog.yaml"),
        Map.entry("spring.jpa.open-in-view", "false"),
        Map.entry("spring.mvc.problemdetails.enabled", "true"),
        Map.entry("server.port", "18091"),
        Map.entry("server.address", "127.0.0.1"),
        Map.entry("spring.main.banner-mode", "off"),
        Map.entry("logging.level.root", "WARN"));
  }

  /**
   * Instala a base mínima antes do Liquibase para preservar a ordem real das chaves estrangeiras.
   */
  @Bean
  liquibase.integration.spring.SpringLiquibase liquibase(DataSource source) {
    new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
            new org.springframework.core.io.ClassPathResource("learningcycle/baseline.sql"))
        .execute(source);
    var runner = new liquibase.integration.spring.SpringLiquibase();
    runner.setDataSource(source);
    runner.setChangeLog("classpath:learningcycle/changelog.yaml");
    return runner;
  }

  /** Limita o modelo persistente às entidades realmente usadas pelo ciclo e pelo ledger BPM. */
  @Bean
  @DependsOn("liquibase")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
    factory.setManagedTypes(
        PersistenceManagedTypes.of(
            LearningSalesCycle.class.getName(),
            LearningSalesCycleEvent.class.getName(),
            BusinessProcessDefinition.class.getName(),
            BusinessProcessChainDefinition.class.getName(),
            BusinessProcessChainItem.class.getName(),
            BusinessProcessActivityDefinition.class.getName(),
            BusinessProcessActivityInstance.class.getName()));
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setJpaPropertyMap(
        Map.of(
            "hibernate.hbm2ddl.auto",
            "validate",
            "hibernate.dialect",
            "org.hibernate.community.dialect.MySQL57Dialect",
            "hibernate.jdbc.time_zone",
            "UTC"));
    return factory;
  }

  /** Mantém transações reais para comprovar atomicidade e concorrência no MySQL 5.7. */
  @Bean
  PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
    return new JpaTransactionManager(factory);
  }

  /** Instancia um repositório JPA real sobre o contexto transacional compartilhado. */
  private <T> T repository(EntityManagerFactory factory, Class<T> type) {
    var target =
        new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory))
            .getRepository(type);
    var proxy = new org.springframework.aop.framework.ProxyFactory(target);
    proxy.addAdvice(
        new org.springframework.transaction.interceptor.TransactionInterceptor(
            new JpaTransactionManager(factory),
            new org.springframework.transaction.interceptor
                .MatchAlwaysTransactionAttributeSource()));
    return type.cast(proxy.getProxy());
  }

  /** Expõe o repositório real de ciclos. */
  @Bean
  LearningSalesCycleRepository cycles(EntityManagerFactory factory) {
    return repository(factory, LearningSalesCycleRepository.class);
  }

  /** Simula somente o ledger de tarefas externas, que não executam durante a homologação. */
  @Bean
  com.marketinghub.repository.jpa.agenttask.AgentTaskRepository agentTasks() {
    return mock(com.marketinghub.repository.jpa.agenttask.AgentTaskRepository.class);
  }

  /** Isola o catálogo de integrações sem carregar executores ou credenciais reais. */
  @Bean
  com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository
      executionResources() {
    return mock(
        com.marketinghub.repository.jpa.businessprocessresource
            .BusinessProcessExecutionResourceRepository.class);
  }

  /** Expõe o repositório real de eventos. */
  @Bean
  LearningSalesCycleEventRepository events(EntityManagerFactory factory) {
    return repository(factory, LearningSalesCycleEventRepository.class);
  }

  /** Expõe o catálogo relacional real do BPM. */
  @Bean
  BusinessProcessDefinitionRepository processes(EntityManagerFactory factory) {
    return repository(factory, BusinessProcessDefinitionRepository.class);
  }

  /** Expõe as definições reais de atividades. */
  @Bean
  BusinessProcessActivityDefinitionRepository activities(EntityManagerFactory factory) {
    return repository(factory, BusinessProcessActivityDefinitionRepository.class);
  }

  /** Expõe as ocorrências BPM reais. */
  @Bean
  BusinessProcessActivityInstanceRepository instances(EntityManagerFactory factory) {
    return repository(factory, BusinessProcessActivityInstanceRepository.class);
  }

  /** Simula apenas identidade e bloqueio de produto, preservando a persistência do ciclo. */
  @Bean
  ProductRepository products() {
    var repository = mock(ProductRepository.class);
    when(repository.findById(anyLong()))
        .thenAnswer(call -> Optional.ofNullable(product(call.getArgument(0))));
    when(repository.findLockedById(anyLong()))
        .thenAnswer(call -> Optional.ofNullable(product(call.getArgument(0))));
    return repository;
  }

  /** Gera somente produtos de teste identificáveis e sem parâmetros comerciais reais. */
  static Product product(Long id) {
    if (!Set.of(91001L, 91002L).contains(id)) return null;
    var product = new Product();
    product.setId(id);
    product.setInternalName(id == 91001L ? "Vega · fixture local" : "Mira · fixture local");
    product.setName(product.getInternalName());
    product.setSlug("fixture-" + id);
    product.setAutomaticExecutionEnabled(true);
    return product;
  }

  /** Simula o cadastro oficial e estados externos que o ciclo apenas consulta. */
  @Bean
  ExperimentRepository experiments() {
    resetExperiments();
    var repository = mock(ExperimentRepository.class);
    when(repository.findById(anyLong()))
        .thenAnswer(call -> Optional.ofNullable(EXPERIMENTS.get(call.getArgument(0))));
    when(repository.findByProductIdOrderByUpdatedAtDescIdDesc(anyLong()))
        .thenAnswer(
            call ->
                EXPERIMENTS.values().stream()
                    .filter(
                        experiment -> experiment.getProduct().getId().equals(call.getArgument(0)))
                    .sorted(Comparator.comparing(Experiment::getId))
                    .toList());
    return repository;
  }

  /** Reinicia exclusivamente os test doubles de experimentos entre cenários. */
  static void resetExperiments() {
    EXPERIMENTS.clear();
    RUNS.clear();
    for (long id = 91001; id <= 91006; id++) {
      var experiment = new Experiment();
      experiment.setId(id);
      experiment.setProduct(product(id == 91006 ? 91002L : 91001L));
      experiment.setName("Experimento segregado #" + id);
      experiment.setStatus(ExperimentStatus.PLANNED);
      experiment.setPlatform(ExperimentPlatform.FACEBOOK);
      experiment.setMediaSpendLimit(new java.math.BigDecimal("100.00"));
      experiment.setExperimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
      EXPERIMENTS.put(id, experiment);
    }
  }

  /** Simula callbacks de publicação; nenhuma API da Meta participa da homologação. */
  @Bean
  ExperimentRunRepository runs() {
    var repository = mock(ExperimentRunRepository.class);
    when(repository.findTopByExperimentIdAndModeOrderByRunNumberDesc(
            anyLong(), eq(ExperimentRunMode.PRODUCTION)))
        .thenAnswer(call -> Optional.ofNullable(RUNS.get(call.getArgument(0))));
    return repository;
  }

  /** Consulta a cadeia real migrada, incluindo as seis etapas e o subprocesso de vendas. */
  @Bean
  BusinessProcessChainDefinitionRepository chains(EntityManagerFactory factory) {
    return repository(factory, BusinessProcessChainDefinitionRepository.class);
  }

  /** Consulta os vínculos reais e versionados entre cadeia e processos. */
  @Bean
  com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainItemRepository
      chainItems(EntityManagerFactory factory) {
    return repository(
        factory,
        com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainItemRepository
            .class);
  }

  /** Instala uma atividade real de gate para receber callbacks simulados dos especialistas. */
  @Bean
  ApplicationRunner fixtures(
      BusinessProcessDefinitionRepository processes,
      BusinessProcessActivityDefinitionRepository activities) {
    return args -> {
      if (processes.findByProcessCodeAndVersionNumber("pde-construction-approval", 8).isPresent())
        return;
      var process = new BusinessProcessDefinition();
      process.setProcessCode("pde-construction-approval");
      process.setVersionNumber(8);
      process.setName("Construção e aprovação local");
      process.setPurpose("Comprovar o retorno causal");
      process.setOwnerName("Dédalo e revisores independentes");
      process.setTriggerDescription("Teste local");
      process.setOutcomeDescription("Gate aprovado");
      process.setStatus("PUBLISHED");
      process.setDiagramJson("{\"nodes\":[],\"flows\":[]}");
      process.setCreatedAt(Instant.now());
      process = processes.saveAndFlush(process);
      for (String id : List.of("agentValidationGate", "rework")) {
        var activity = new BusinessProcessActivityDefinition();
        activity.setProcessDefinition(process);
        activity.setActivityId(id);
        activity.setName(id.equals("rework") ? "Corrigir valor do produto" : "Gate multiagente");
        activity.setOwnerName("Dédalo");
        activity.setDefinitionJson("{}");
        activity.setCreatedAt(Instant.now());
        activities.saveAndFlush(activity);
      }
    };
  }

  /** Responsabilidade: simular contratos externos exclusivamente na aplicação de testes. */
  @RestController
  @Profile("learning-cycles-fixture")
  static class FixtureController {
    private final BusinessProcessDefinitionRepository processes;
    private final BusinessProcessActivityDefinitionRepository activities;
    private final BusinessProcessActivityInstanceRepository instances;
    private final ObjectMapper mapper;
    private final LearningSalesCycleRepository cycles;
    private final LearningSalesCycleEventRepository events;

    /** Recebe as fontes persistidas da fixture. */
    FixtureController(
        BusinessProcessDefinitionRepository processes,
        BusinessProcessActivityDefinitionRepository activities,
        BusinessProcessActivityInstanceRepository instances,
        ObjectMapper mapper,
        LearningSalesCycleRepository cycles,
        LearningSalesCycleEventRepository events) {
      this.cycles = cycles;
      this.events = events;
      this.processes = processes;
      this.activities = activities;
      this.instances = instances;
      this.mapper = mapper;
    }

    /** Lista produtos explicitamente segregados de homologação. */
    @GetMapping("/api/products")
    List<Product> products() {
      return List.of(product(91001L), product(91002L));
    }

    /** Reinicia somente estados simulados para outra rodada local. */
    @PostMapping("/fixture/reset")
    Map<String, Object> reset() {
      var existing = cycles.findAll();
      existing.forEach(cycle -> cycle.setCurrentInstanceId(null));
      cycles.saveAllAndFlush(existing);
      events.deleteAllInBatch();
      existing.stream()
          .sorted(Comparator.comparing(LearningSalesCycle::getId).reversed())
          .forEach(cycle -> cycles.deleteById(cycle.getId()));
      instances.deleteAllInBatch();
      resetExperiments();
      return Map.of("reset", true);
    }

    /** Simula uma nova autorização de limite no cadastro oficial do experimento. */
    @PostMapping("/fixture/experiments/{id}/budget")
    Map<String, Object> budget(@PathVariable Long id, @RequestBody Map<String, Object> input) {
      EXPERIMENTS
          .get(id)
          .setMediaSpendLimit(new java.math.BigDecimal(input.get("budgetLimitBrl").toString()));
      return Map.of("updated", true);
    }

    /** Simula o registro de publicação pelo backend responsável. */
    @PostMapping("/fixture/experiments/{id}/publish")
    Map<String, Object> publish(@PathVariable Long id) {
      var experiment = EXPERIMENTS.get(id);
      experiment.setStatus(ExperimentStatus.RUNNING);
      var run = new ExperimentRun();
      run.setExperiment(experiment);
      run.setId(id);
      run.setMode(ExperimentRunMode.PRODUCTION);
      run.setPublishedAt(Instant.now());
      run.setPreflightCompletedAt(Instant.now());
      RUNS.put(id, run);
      return Map.of("published", true);
    }

    /** Simula o encerramento oficial do experimento sem gerar tráfego comercial. */
    @PostMapping("/fixture/experiments/{id}/stop")
    Map<String, Object> stop(@PathVariable Long id) {
      EXPERIMENTS.get(id).setStatus(ExperimentStatus.USER_STOPPED);
      return Map.of("stopped", true);
    }

    /** Reproduz um estado legado divergente sem apagar a prova de exposição anterior. */
    @PostMapping("/fixture/experiments/{id}/plan-again")
    Map<String, Object> planAgain(@PathVariable Long id) {
      EXPERIMENTS.get(id).setStatus(ExperimentStatus.PLANNED);
      return Map.of("planned", true);
    }

    /** Persiste a evidência canônica emitida pelo gate com especialistas simulados. */
    @PostMapping("/fixture/approval")
    Map<String, Object> approval(@RequestBody Map<String, Object> input) {
      Long productId = ((Number) input.get("productId")).longValue();
      var process =
          processes.findByProcessCodeAndVersionNumber("pde-construction-approval", 8).orElseThrow();
      var activity =
          activities
              .findByProcessDefinitionIdAndActivityId(process.getId(), "agentValidationGate")
              .orElseThrow();
      var gate = new BusinessProcessActivityInstance();
      gate.setActivityDefinition(activity);
      gate.setSourceReference("product:" + productId + "@agent-validation-v1");
      gate.setOccurrenceNumber((int) (System.nanoTime() % Integer.MAX_VALUE));
      gate.setStatus(Boolean.FALSE.equals(input.get("approved")) ? "BLOCKED" : "COMPLETED");
      gate.setObjectiveAchieved(!Boolean.FALSE.equals(input.get("approved")));
      gate.setEnteredAt(Instant.now());
      gate.setExitedAt(Instant.now());
      gate.setCreatedAt(Instant.now());
      gate.setUpdatedAt(Instant.now());
      gate.setCostCoverage("COMPLETE");
      gate.setEvidenceQuality("DIRECT");
      gate.setObjectiveEvidenceJson(
          mapper
              .valueToTree(
                  Map.of(
                      "evidenceType",
                      "PDE_AGENT_VALIDATION_GATE_V1",
                      "productId",
                      productId,
                      "prototypeVersion",
                      input.get("productVersion"),
                      "taskEvidence",
                      List.of(
                          "technical",
                          "psiqueAdherent",
                          "psiqueRecovery",
                          "psiqueSafety",
                          "temis")))
              .toString());
      return Map.of("approvalInstanceId", instances.saveAndFlush(gate).getId());
    }
  }
}
