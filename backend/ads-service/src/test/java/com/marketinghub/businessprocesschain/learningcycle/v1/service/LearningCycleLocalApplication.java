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
import com.marketinghub.experiment.service.publicationhistory.HistoricalCampaignReceipt;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
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
  com.marketinghub.businessprocesschain.learningcycle.v1.decision.service
      .LearningCycleDecisionService.class,
  com.marketinghub.businessprocesschain.learningcycle.v1.decision.service
      .LearningCycleDecisionApproval.class,
  com.marketinghub.businessprocesschain.learningcycle.v1.decision.controller
      .LearningCycleDecisionController.class,
  LearningCycleOrganization.class,
  LearningCycleActivityProjection.class,
  SalesFlowResolver.class,
  SalesFlowActivityReadiness.class,
  com.marketinghub.product.service.valuechainposition.ProductValueChainPositionService.class,
  com.marketinghub.product.service.valuechainposition.ProductSubprocessPositionResolver.class,
  com.marketinghub.product.service.valuechainposition.ProductStageMeasurementResolver.class,
  com.marketinghub.product.service.valuechainposition.PdeProcessCodeResolver.class,
  com.marketinghub.product.web.ProductValueChainPositionController.class,
  LearningCycleExecutionContext.class,
  LearningCycleWorkResolver.class,
  com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService.class,
  com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController
      .class,
  com.marketinghub.businessprocesschain.service.BusinessProcessChainService.class,
  com.marketinghub.businessprocesschain.controller.BusinessProcessChainController.class,
  BusinessProcessDefinitionService.class,
  BusinessProcessDefinitionController.class,
  com.marketinghub.businessprocesscomposition.service.BusinessProcessCompositionService.class,
  com.marketinghub.businessprocesscomposition.controller.BusinessProcessCompositionController.class,
  LearningCycleJson.class,
  LearningCycleEvidence.class,
  LearningCyclePublicationHistory.class,
  LearningCycleVideoEvidence.class,
  LearningCycleVideoBudget.class,
  com.marketinghub.businessprocess.automation.v1.service.ProcessRunService.class,
  com.marketinghub.businessprocess.automation.v1.service.ProcessRunContext.class,
  com.marketinghub.businessprocess.automation.v1.service.ProcessRunNavigation.class,
  com.marketinghub.businessprocess.automation.v1.service.ProcessRunSubprocesses.class,
  com.marketinghub.businessprocess.automation.v1.service.ProcessRunGuidance.class,
  com.marketinghub.businessprocess.automation.v1.controller.ProcessRunController.class,
  LearningCycleVideoFixtures.class,
  LearningCycleBpmLedger.class,
  LearningCycleController.class
})
public class LearningCycleLocalApplication {
  static final Map<Long, Experiment> EXPERIMENTS = new ConcurrentHashMap<>();
  static final Map<Long, ExperimentRun> RUNS = new ConcurrentHashMap<>();
  static final Map<Long, HistoricalCampaignReceipt> CAMPAIGNS = new ConcurrentHashMap<>();
  static final Map<Long, Map<String, Object>> MEASUREMENTS = new ConcurrentHashMap<>();

  /** Simula a biblioteca de referências sem consultar fontes ou agentes produtivos. */
  @Bean
  com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService
      researchIntelligence() {
    return mock(com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService.class);
  }

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
        Map.entry("process-execution.worker-token", "cycles-process-fixture-only"),
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

  /** Persiste a fila de propostas em MySQL real na homologação. */
  @Bean
  LearningCycleDecisionProposalRepository decisionProposals(EntityManagerFactory factory) {
    return repository(factory, LearningCycleDecisionProposalRepository.class);
  }

  /** Simula somente o cadastro PLAY de Atena sem consultar agente produtivo. */
  @Bean
  com.marketinghub.repository.jpa.agent.AgentRepository decisionAgents() {
    var repository = mock(com.marketinghub.repository.jpa.agent.AgentRepository.class);
    var agent =
        com.marketinghub.agent.Agent.builder()
            .id(91004L)
            .nickname("Atena")
            .agentKey("experiment-strategist")
            .automaticExecutionEnabled(true)
            .build();
    when(repository.findByAgentKey("experiment-strategist")).thenReturn(Optional.of(agent));
    return repository;
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

  /** Limita a persistência ao ciclo, sua execução de processo e ao ledger BPM homologados. */
  @Bean
  @DependsOn("liquibase")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
    factory.setManagedTypes(
        PersistenceManagedTypes.of(
            LearningSalesCycle.class.getName(),
            com.marketinghub.businessprocess.automation.v1.ProcessRun.class.getName(),
            com.marketinghub.businessprocess.automation.v1.ProcessRunEvent.class.getName(),
            com.marketinghub.businessprocesschain.learningcycle.v1.decision
                .LearningCycleDecisionProposal.class
                .getName(),
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

  /** Persiste o estado real de execução dos processos vinculados aos ciclos de teste. */
  @Bean
  com.marketinghub.repository.jpa.processautomation.ProcessRunRepository processRuns(
      EntityManagerFactory factory) {
    return repository(
        factory, com.marketinghub.repository.jpa.processautomation.ProcessRunRepository.class);
  }

  /** Preserva as decisões reais do conciliador, sem simular sucesso de atividade. */
  @Bean
  com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository processRunEvents(
      EntityManagerFactory factory) {
    return repository(
        factory, com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository.class);
  }

  /** Simula somente o ledger de tarefas externas, que não executam durante a homologação. */
  @Bean
  com.marketinghub.repository.jpa.agenttask.AgentTaskRepository agentTasks() {
    return mock(com.marketinghub.repository.jpa.agenttask.AgentTaskRepository.class);
  }

  /** Simula apenas vínculos de tarefas externas; as atividades e os ciclos usam MySQL real. */
  @Bean
  com.marketinghub.repository.jpa.agenttask.AgentTaskActivityCoverageRepository activityCoverage() {
    return mock(
        com.marketinghub.repository.jpa.agenttask.AgentTaskActivityCoverageRepository.class);
  }

  /** Mantém os planos externos vazios para comprovar a entrada histórica independente. */
  @Bean
  com.marketinghub.repository.jpa.planning.CommercialPlanRepository plans() {
    return mock(com.marketinghub.repository.jpa.planning.CommercialPlanRepository.class);
  }

  /** Isola execuções de landing sem acionar geradores durante navegação. */
  @Bean
  com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository landings() {
    return mock(
        com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository.class);
  }

  /** Impede execução de agentes reais; a matriz verifica que navegar não solicita tarefas. */
  @Bean
  com.marketinghub.agenttask.AgentTaskService taskService() {
    return mock(com.marketinghub.agenttask.AgentTaskService.class);
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

  /** Simula as fontes oficiais já conciliadas sem acessar Meta, checkout ou tráfego reais. */
  @Bean
  LearningCycleMeasurementCollector measurements(ObjectMapper mapper) {
    var collector = mock(LearningCycleMeasurementCollector.class);
    when(collector.collect(
            any(LearningSalesCycle.class), any(Experiment.class), any(Instant.class)))
        .thenAnswer(
            call -> {
              LearningSalesCycle cycle = call.getArgument(0);
              Experiment experiment = call.getArgument(1);
              Instant observedAt = call.getArgument(2);
              Instant periodEnd =
                  observedAt.isBefore(cycle.getWindowEnd()) ? observedAt : cycle.getWindowEnd();
              Map<String, Object> source = MEASUREMENTS.getOrDefault(experiment.getId(), Map.of());
              String snapshot = String.valueOf(source.getOrDefault("snapshot", "v1"));
              var evidence = mapper.createObjectNode();
              evidence.put("contractVersion", LearningCycleMeasurementCollector.CONTRACT);
              evidence.put("automatic", true);
              evidence.put("experimentId", experiment.getId());
              evidence.put("observedAt", observedAt.toString());
              if (!booleanValue(source, "ready", true)) {
                String blocker =
                    String.valueOf(
                        source.getOrDefault(
                            "blocker", "A fonte simulada está indisponível para conciliação."));
                evidence.put("dataValid", false);
                evidence.put("blocker", blocker);
                return new LearningCycleMeasurementCollector.Result(
                    false,
                    evidence,
                    "Conciliação automática bloqueada: " + blocker,
                    "internal://fixture/automatic-measurement/"
                        + experiment.getId()
                        + "/blocked/"
                        + snapshot);
              }
              evidence.put("sourceFingerprint", "fixture-" + experiment.getId() + "-" + snapshot);
              evidence.put("currency", "BRL");
              evidence.put("source", "Fontes oficiais simuladas e segregadas da fixture local");
              evidence.put("periodStart", cycle.getWindowStart().toString());
              evidence.put("periodEnd", periodEnd.toString());
              evidence.put("sessions", longValue(source, "sessions", 10));
              evidence.put("starts", longValue(source, "starts", 8));
              evidence.put("firstResults", longValue(source, "firstResults", 7));
              evidence.put("checkouts", longValue(source, "checkouts", 2));
              evidence.put("netSales", longValue(source, "netSales", 0));
              evidence.put("refunds", longValue(source, "refunds", 0));
              evidence.put("spendBrl", decimalValue(source, "spendBrl", "40.00"));
              evidence.put("revenueBrl", decimalValue(source, "revenueBrl", "0.00"));
              evidence.put("contributionBrl", decimalValue(source, "contributionBrl", "-40.00"));
              evidence.put("dataValid", true);
              evidence.put("testDataExcluded", true);
              evidence.put("deliveryVerified", booleanValue(source, "deliveryVerified", false));
              evidence.put("useVerified", booleanValue(source, "useVerified", false));
              evidence.put(
                  "satisfactionVerified", booleanValue(source, "satisfactionVerified", false));
              var sources = evidence.putObject("sources");
              sources.putObject("pdeAnalytics").put("trafficQualityIncluded", "HUMAN");
              sources.putObject("acquisition").put("mode", "META_INSIGHTS_FIXTURE");
              sources.putObject("financialOutcomes").put("referencesComplete", true);
              sources.putObject("costLedger").put("auditable", true);
              sources.putObject("valueDelivery").put("referencesComplete", true);
              return new LearningCycleMeasurementCollector.Result(
                  true,
                  evidence,
                  "Conciliação automática das fontes segregadas da fixture.",
                  "internal://fixture/automatic-measurement/"
                      + experiment.getId()
                      + "/"
                      + snapshot);
            });
    return collector;
  }

  /** Lê uma contagem configurável mantendo valor padrão determinístico. */
  private static long longValue(Map<String, Object> source, String field, long fallback) {
    Object value = source.get(field);
    return value instanceof Number number ? number.longValue() : fallback;
  }

  /** Lê moeda de teste sem depender do tipo numérico escolhido pelo parser JSON. */
  private static java.math.BigDecimal decimalValue(
      Map<String, Object> source, String field, String fallback) {
    return new java.math.BigDecimal(String.valueOf(source.getOrDefault(field, fallback)));
  }

  /** Lê flags configuráveis mantendo o padrão seguro do cenário sem vendas. */
  private static boolean booleanValue(Map<String, Object> source, String field, boolean fallback) {
    Object value = source.get(field);
    return value instanceof Boolean bool ? bool : fallback;
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
    when(repository.findValueChainSummaryById(anyLong()))
        .thenAnswer(
            call -> {
              var value = product(call.getArgument(0));
              return value == null
                  ? Optional.empty()
                  : Optional.of(
                      new com.marketinghub.repository.jpa.product.ProductValueChainSummaryProduct(
                          value.getId(),
                          value.getName(),
                          value.getInternalName(),
                          value.getCommercialStatus()));
            });
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
    product.setCommercialStatus("ACTIVE");
    return product;
  }

  /** Isola períodos comerciais legados; o estado corrente é lido de ciclos reais no MySQL. */
  @Bean
  com.marketinghub.repository.jpa.product.ProductProcessPeriodRepository periods() {
    return mock(com.marketinghub.repository.jpa.product.ProductProcessPeriodRepository.class);
  }

  /** Isola custos de fornecedores sem autorizar integrações ou registrar despesas reais. */
  @Bean
  com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository studioLedger() {
    return mock(
        com.marketinghub.repository.jpa.financialagent.StudioCostLedgerEntryRepository.class);
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
    CAMPAIGNS.clear();
    MEASUREMENTS.clear();
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
    when(repository.findTopByExperimentIdAndModeAndPublishedAtIsNotNullOrderByRunNumberDesc(
            anyLong(), eq(ExperimentRunMode.PRODUCTION)))
        .thenAnswer(
            call ->
                Optional.ofNullable(RUNS.get(call.getArgument(0)))
                    .filter(run -> run.getPublishedAt() != null));
    return repository;
  }

  /** Simula recibos Meta legados sem criar run ou chamar o provedor externo. */
  @Bean
  com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository campaigns() {
    var repository =
        mock(com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository.class);
    when(repository.findHistoricalPublicationReceipts(anyLong()))
        .thenAnswer(
            call -> Optional.ofNullable(CAMPAIGNS.get(call.getArgument(0))).stream().toList());
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

  /** Instala os dois outros subprocessos chamados pelo pai, sem executores ou efeitos externos. */
  @Bean
  ApplicationRunner subprocessFixtures(BusinessProcessDefinitionRepository processes) {
    return args -> {
      for (String code :
          List.of("operacao-otimizacao-experimento", "venda-entrega-satisfacao-cliente")) {
        if (processes.findByProcessCodeAndVersionNumber(code, 1).isPresent()) continue;
        var process = new BusinessProcessDefinition();
        process.setProcessCode(code);
        process.setVersionNumber(1);
        process.setName(
            code.startsWith("operacao")
                ? "Operação do experimento local"
                : "Entrega e satisfação local");
        process.setPurpose("Validar a navegação de subprocessos na sandbox");
        process.setOwnerName("Backend local");
        process.setTriggerDescription("Atividade de chamada do pai");
        process.setOutcomeDescription("Consulta local sem execução comercial");
        process.setStatus("PUBLISHED");
        process.setProcessType("SUBPROCESS");
        process.setExecutionScope("PRODUCT");
        process.setParentProcessCode("pde-sales-delivery-learning");
        process.setDiagramJson("{\"nodes\":[],\"flows\":[]}");
        process.setCreatedAt(Instant.now());
        processes.saveAndFlush(process);
      }
    };
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
    private final LearningCycleDecisionProposalRepository decisionProposals;
    private final com.marketinghub.repository.jpa.processautomation.ProcessRunRepository
        processRuns;
    private final com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository
        processRunEvents;

    /** Recebe as fontes persistidas da fixture. */
    FixtureController(
        BusinessProcessDefinitionRepository processes,
        BusinessProcessActivityDefinitionRepository activities,
        BusinessProcessActivityInstanceRepository instances,
        ObjectMapper mapper,
        LearningSalesCycleRepository cycles,
        LearningSalesCycleEventRepository events,
        LearningCycleDecisionProposalRepository decisionProposals,
        com.marketinghub.repository.jpa.processautomation.ProcessRunRepository processRuns,
        com.marketinghub.repository.jpa.processautomation.ProcessRunEventRepository
            processRunEvents) {
      this.processRuns = processRuns;
      this.processRunEvents = processRunEvents;
      this.decisionProposals = decisionProposals;
      this.cycles = cycles;
      this.events = events;
      this.processes = processes;
      this.activities = activities;
      this.instances = instances;
      this.mapper = mapper;
    }

    /** Simula o comando PLAY do executor somente para consumo HTTP local. */
    @GetMapping("/api/internal/agents/executor-health/experiment-strategist/automatic-execution")
    Map<String, Object> automaticExecution() {
      return Map.of("automaticExecutionEnabled", true);
    }

    /** Lista produtos explicitamente segregados de homologação. */
    @GetMapping("/api/products")
    List<Product> products() {
      return List.of(product(91001L), product(91002L));
    }

    /** Limpa execuções e ciclos em uma transação, preservando a ordem das referências locais. */
    @PostMapping("/fixture/reset")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    Map<String, Object> reset() {
      processRunEvents.deleteAllInBatch();
      processRuns.deleteAllInBatch();
      var existing =
          cycles.findAll().stream()
              .sorted(Comparator.comparing(LearningSalesCycle::getId))
              .map(cycle -> cycles.findLockedById(cycle.getId()))
              .flatMap(Optional::stream)
              .toList();
      existing.forEach(cycle -> cycle.setCurrentInstanceId(null));
      cycles.saveAllAndFlush(existing);
      decisionProposals.deleteAllInBatch();
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

    /** Reproduz o contrato de recibo do legado pausado, preservando a ausência de run/preflight. */
    @PostMapping("/fixture/experiments/{id}/legacy-publication")
    Map<String, Object> legacyPublication(@PathVariable Long id) {
      var experiment = EXPERIMENTS.get(id);
      experiment.setStatus(ExperimentStatus.USER_STOPPED);
      var receipt =
          new HistoricalCampaignReceipt(
              "fixture-campaign-" + id, Instant.now().minusSeconds(86400));
      CAMPAIGNS.put(id, receipt);
      return Map.of(
          "legacyReceipt", receipt.campaignId(), "runCount", RUNS.containsKey(id) ? 1 : 0);
    }

    /** Expõe somente os estados segregados para comprovar ausência de mutação retroativa. */
    @GetMapping("/fixture/experiments/{id}/state")
    Map<String, Object> state(@PathVariable Long id) {
      return Map.of(
          "status", EXPERIMENTS.get(id).getStatus(),
          "runCount", RUNS.containsKey(id) ? 1 : 0,
          "campaignCount", CAMPAIGNS.containsKey(id) ? 1 : 0);
    }

    /** Simula o encerramento oficial do experimento sem gerar tráfego comercial. */
    @PostMapping("/fixture/experiments/{id}/stop")
    Map<String, Object> stop(@PathVariable Long id) {
      EXPERIMENTS.get(id).setStatus(ExperimentStatus.USER_STOPPED);
      return Map.of("stopped", true);
    }

    /** Configura apenas a fotografia segregada usada pela próxima conciliação local. */
    @PostMapping("/fixture/experiments/{id}/measurement")
    Map<String, Object> measurement(@PathVariable Long id, @RequestBody Map<String, Object> input) {
      if (!EXPERIMENTS.containsKey(id))
        throw new IllegalArgumentException("Experimento inexistente");
      MEASUREMENTS.put(id, Map.copyOf(input));
      return Map.of("configured", true, "experimentId", id);
    }

    /** Reproduz um estado legado divergente sem apagar a prova de exposição anterior. */
    @PostMapping("/fixture/experiments/{id}/plan-again")
    Map<String, Object> planAgain(@PathVariable Long id) {
      EXPERIMENTS.get(id).setStatus(ExperimentStatus.PLANNED);
      return Map.of("planned", true);
    }

    /**
     * Persiste o gate simulado na fonte canônica do ciclo, permitindo fontes inválidas no teste.
     */
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
      String canonicalSource =
          cycles.findByProductIdOrderByIdDesc(productId).stream()
              .findFirst()
              .map(
                  cycle ->
                      LearningCycleExecutionContext.constructionSource(product(productId), cycle))
              .orElse("experiment:91003");
      String source = String.valueOf(input.getOrDefault("sourceReference", canonicalSource));
      gate.setSourceReference(source);
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
                      "sourceReference",
                      input.getOrDefault("proofSourceReference", source),
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
