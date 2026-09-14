package com.marketinghub.product.executionprofile.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.financialagent.*;
import com.marketinghub.financialagent.service.*;
import com.marketinghub.planning.*;
import com.marketinghub.product.Product;
import com.marketinghub.product.executionprofile.v1.*;
import com.marketinghub.product.executionprofile.v1.controller.ExecutionProfileController;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocesschain.*;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.planning.*;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.productexecution.*;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: homologar fichas reais em MySQL, com contratos externos e produtos sintéticos.
 */
@TestConfiguration
@Profile("execution-profile-fixture")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({
  ExecutionProfileService.class,
  ExecutionProfileContext.class,
  ExecutionProfileOmissions.class,
  ExecutionProfileActivityPolicy.class,
  ExecutionProfileBudget.class,
  ExecutionProfileController.class,
  com.marketinghub.leadportal.service.LeadPortalExecutionProfileBudget.class,
  com.marketinghub.repository.jdbc.leadportal.ExecutionProfileImagePackageRepository.class,
  ExecutionProfileLocalApplication.FixtureController.class
})
public class ExecutionProfileLocalApplication {
  static final long CHAIN = 94014;
  static final String CHANGELOG =
      "db/changelog/changesets/2026-09-14-product-execution-profiles-v1.yaml";

  /** Inicia apenas serviços locais e ignora configurações e credenciais produtivas. */
  public static void main(String[] args) {
    String host = System.getenv().getOrDefault("PROFILE_TEST_DB_HOST", "sandbox-docker");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host))
      throw new IllegalArgumentException("Host não local.");
    new SpringApplication(ExecutionProfileLocalApplication.class)
        .run(
            "--spring.config.location=optional:classpath:execution-profile/no-production.properties",
            "--spring.profiles.active=execution-profile-fixture",
            "--spring.datasource.url=jdbc:mysql://"
                + host
                + ":18414/execution_profiles_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            "--spring.datasource.username=root",
            "--spring.datasource.password=profiles-local-only",
            "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
            "--spring.jpa.open-in-view=false",
            "--spring.liquibase.enabled=false",
            "--spring.sql.init.mode=never",
            "--server.port=18094",
            "--server.address=127.0.0.1",
            "--spring.mvc.problemdetails.enabled=true",
            "--logging.level.root=WARN");
  }

  /** Cria fontes mínimas sintéticas e aplica o mesmo changelog incremental da aplicação. */
  @Bean
  liquibase.integration.spring.SpringLiquibase liquibase(DataSource source) {
    var jdbc = new JdbcTemplate(source);
    for (String name :
        List.of(
            "product",
            "commercial_plan",
            "business_process_chain_definition",
            "learning_sales_cycle_v1",
            "financial_agent_execution"))
      jdbc.execute("CREATE TABLE IF NOT EXISTS " + name + " (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS fixture_finance (id BIGINT AUTO_INCREMENT PRIMARY KEY, plan_id BIGINT NOT NULL, status VARCHAR(30), context_json LONGTEXT) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS fixture_instances (id BIGINT AUTO_INCREMENT PRIMARY KEY, process_id BIGINT, activity_id VARCHAR(100), source_reference VARCHAR(191), evidence LONGTEXT) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS fixture_settings (id INT PRIMARY KEY, version_number INT) ENGINE=InnoDB");
    jdbc.update("INSERT IGNORE INTO fixture_settings VALUES (1, 1)");
    for (long id = 94001; id <= 94080; id++)
      for (String table : List.of("product", "commercial_plan", "learning_sales_cycle_v1"))
        jdbc.update("INSERT IGNORE INTO " + table + " VALUES (?)", id);
    jdbc.update("INSERT IGNORE INTO business_process_chain_definition VALUES (?)", CHAIN);
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS lead_portal_flow (id BIGINT PRIMARY KEY, slug VARCHAR(190)) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS flow_submissions (id VARCHAR(36) PRIMARY KEY, flow_slug VARCHAR(190), stored_file_name VARCHAR(255)) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS experiment (id BIGINT PRIMARY KEY, product_id BIGINT, lead_portal_flow_id BIGINT, images_per_package INT) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS flow_submission_image_package (id BIGINT PRIMARY KEY, submission_id VARCHAR(36), planned_outputs INT, prompt LONGTEXT) ENGINE=InnoDB");
    for (long id = 94001; id <= 94080; id++) {
      jdbc.update("INSERT IGNORE INTO lead_portal_flow VALUES (?,?)", id, "fixture-flow-" + id);
      jdbc.update(
          "INSERT IGNORE INTO flow_submissions VALUES (?,?,?)",
          "fixture-submission-" + id,
          "fixture-flow-" + id,
          id == 94007 ? "fixture-base.png" : null);
      jdbc.update("INSERT IGNORE INTO experiment VALUES (?,?,?,2)", id, id, id);
      jdbc.update(
          "INSERT IGNORE INTO flow_submission_image_package VALUES (?,?,2,'Entrada sintética')",
          id,
          "fixture-submission-" + id);
    }
    var migration = new liquibase.integration.spring.SpringLiquibase();
    migration.setDataSource(source);
    migration.setChangeLog("classpath:" + CHANGELOG);
    return migration;
  }

  /** Valida mapeamentos das quatro entidades novas contra o schema MySQL 5.7 aplicado. */
  @Bean
  @DependsOn("liquibase")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
    factory.setManagedTypes(
        PersistenceManagedTypes.of(
            ExecutionProfile.class.getName(),
            ExecutionProfileBinding.class.getName(),
            ExecutionProfileReview.class.getName(),
            ExecutionProfileConsumption.class.getName()));
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

  /** Coordena os locks JDBC de produto e as gravações JPA na mesma transação real. */
  @Bean
  PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
    return new JpaTransactionManager(factory);
  }

  /** Cria um repositório real no contexto transacional compartilhado. */
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

  /** Habilita persistência real das fichas. */
  @Bean
  ExecutionProfileRepository profiles(EntityManagerFactory f) {
    return repository(f, ExecutionProfileRepository.class);
  }

  /** Habilita persistência real dos vínculos. */
  @Bean
  ExecutionProfileBindingRepository bindings(EntityManagerFactory f) {
    return repository(f, ExecutionProfileBindingRepository.class);
  }

  /** Habilita persistência real dos pareceres vinculados. */
  @Bean
  ExecutionProfileReviewRepository reviews(EntityManagerFactory f) {
    return repository(f, ExecutionProfileReviewRepository.class);
  }

  /** Habilita persistência real do consumo. */
  @Bean
  ExecutionProfileConsumptionRepository consumption(EntityManagerFactory f) {
    return repository(f, ExecutionProfileConsumptionRepository.class);
  }

  /** Fornece identidades sintéticas e locks de linha reais para testar concorrência. */
  @Bean
  ProductRepository products(JdbcTemplate jdbc) {
    var repo = mock(ProductRepository.class);
    when(repo.existsById(anyLong())).thenAnswer(i -> product((Long) i.getArgument(0)).isPresent());
    when(repo.findById(anyLong())).thenAnswer(i -> product(i.getArgument(0)));
    when(repo.findLockedById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              jdbc.queryForList("SELECT id FROM product WHERE id=? FOR UPDATE", id);
              return product(id);
            });
    return repo;
  }

  /** Cria produto de teste sem assumir que o mineral escolhe a capacidade. */
  static Optional<Product> product(long id) {
    if (id < 94001 || id > 94080) return Optional.empty();
    var p = new Product();
    p.setId(id);
    p.setName("Produto sintético " + id);
    p.setInternalName("Fixture " + id);
    p.setProductType(id % 2 == 0 ? "AI_PRODUCT" : "LOW_TICKET_DIGITAL_PRODUCT");
    p.setProductFormat("Pacote visual");
    return Optional.of(p);
  }

  /** Mantém um plano próprio para cada produto de teste. */
  @Bean
  CommercialPlanRepository plans() {
    var r = mock(CommercialPlanRepository.class);
    when(r.findIdsByProductId(anyLong())).thenAnswer(i -> List.of((Long) i.getArgument(0)));
    when(r.findByProductId(anyLong())).thenAnswer(i -> List.of(plan(i.getArgument(0))));
    return r;
  }

  /** Monta um plano mínimo sem qualquer credencial ou experimento real. */
  static CommercialPlan plan(long id) {
    var p = new CommercialPlan();
    p.setId(id);
    p.setName("Plano local " + id);
    return p;
  }

  /** Simula uma alteração material de versão persistida para verificar revogação dos gates. */
  @Bean
  CommercialPlanVersionRepository versions(JdbcTemplate jdbc) {
    var r = mock(CommercialPlanVersionRepository.class);
    when(r.findTopByPlanIdOrderByVersionNumberDesc(anyLong()))
        .thenAnswer(
            i -> {
              var v = new CommercialPlanVersion();
              v.setVersionNumber(
                  jdbc.queryForObject(
                      "SELECT version_number FROM fixture_settings WHERE id=1", Integer.class));
              return Optional.of(v);
            });
    return r;
  }

  /** Disponibiliza a cadeia comum com seis definições exatas. */
  @Bean
  BusinessProcessChainDefinitionRepository chains() {
    var r = mock(BusinessProcessChainDefinitionRepository.class);
    var chain = new BusinessProcessChainDefinition();
    chain.setId(CHAIN);
    chain.setName("Cadeia local comum");
    chain.setVersionNumber(1);
    chain.setStatus("PUBLISHED");
    List<BusinessProcessChainItem> items = new ArrayList<>();
    for (int n = 0; n < 6; n++) {
      var item = new BusinessProcessChainItem();
      item.setProcessDefinition(process(94101L + n));
      item.setSequenceNumber(n + 1);
      items.add(item);
    }
    chain.setItems(items);
    when(r.findById(CHAIN)).thenReturn(Optional.of(chain));
    when(r.findAllByStatusOrderByNameAscVersionNumberDesc("PUBLISHED")).thenReturn(List.of(chain));
    return r;
  }

  /** Mantém catálogo de processos simulado; suas identidades são persistidas na ficha real. */
  @Bean
  BusinessProcessDefinitionRepository processes() {
    var r = mock(BusinessProcessDefinitionRepository.class);
    when(r.findById(anyLong())).thenAnswer(i -> Optional.of(process(i.getArgument(0))));
    when(r.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(anyString(), eq("PUBLISHED")))
        .thenAnswer(
            i ->
                Optional.of(
                    process(94101L + ExecutionProfileRules.PHASES.indexOf(i.getArgument(0)))));
    return r;
  }

  /** Monta o BPM mínimo usado na especialização de atividades. */
  static BusinessProcessDefinition process(long id) {
    var p = new BusinessProcessDefinition();
    p.setId(id);
    p.setProcessCode(ExecutionProfileRules.PHASES.get((int) (id - 94101)));
    p.setName("Processo " + (id - 94100));
    p.setVersionNumber(1);
    p.setStatus("PUBLISHED");
    p.setDiagramJson("{\"nodes\":[],\"flows\":[]}");
    return p;
  }

  /** Fornece a definição de audiovisual para verificar dispensa real sem falsear objetivo. */
  @Bean
  BusinessProcessActivityDefinitionRepository definitions() {
    var r = mock(BusinessProcessActivityDefinitionRepository.class);
    when(r.findByProcessDefinitionIdAndActivityId(94103L, "audiovisual"))
        .thenReturn(Optional.of(audio()));
    return r;
  }

  /** Cria a definição opcional estritamente pertencente a Apolo. */
  static BusinessProcessActivityDefinition audio() {
    var d = new BusinessProcessActivityDefinition();
    d.setId(94201L);
    d.setProcessDefinition(process(94103));
    d.setActivityId("audiovisual");
    d.setOwnerName("Apolo");
    d.setDefinitionJson("{\"responsibleAgentKeys\":[\"videomaker\"]}");
    return d;
  }

  /**
   * Persiste a evidência da instância existente em tabela isolada, sem carregar todo o domínio BPM.
   */
  @Bean
  BusinessProcessActivityInstanceRepository instances(JdbcTemplate jdbc) {
    var r = mock(BusinessProcessActivityInstanceRepository.class);
    when(r.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              var a = (BusinessProcessActivityInstance) i.getArgument(0);
              jdbc.update(
                  "INSERT INTO fixture_instances(process_id,activity_id,source_reference,evidence) VALUES (?,?,?,?)",
                  a.getActivityDefinition().getProcessDefinition().getId(),
                  a.getActivityDefinition().getActivityId(),
                  a.getSourceReference(),
                  a.getObjectiveEvidenceJson());
              return a;
            });
    return r;
  }

  /** Simula apenas ausência de tarefas anteriores em referências novas de homologação. */
  @Bean
  AgentTaskRepository tasks() {
    return mock(AgentTaskRepository.class);
  }

  /** Simula apenas ausência de processos anteriores em referências novas de homologação. */
  @Bean
  ProcessRunRepository runs() {
    return mock(ProcessRunRepository.class);
  }

  /** Confere propriedade de experimentos sintéticos com IDs distintos por produto. */
  @Bean
  ExperimentRepository experiments() {
    var r = mock(ExperimentRepository.class);
    when(r.findById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              var e = new Experiment();
              e.setId(id);
              e.setProduct(product(id).orElseThrow());
              return Optional.of(e);
            });
    return r;
  }

  /** Confere identidade completa de ciclos sintéticos. */
  @Bean
  LearningSalesCycleRepository cycles() {
    var r = mock(LearningSalesCycleRepository.class);
    when(r.findById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              var c = new LearningSalesCycle();
              c.setId(id);
              c.setProductId(id);
              c.setExperimentId(id);
              c.setChainDefinitionId(CHAIN);
              c.setProductVersion("fixture-v1");
              c.setStatus("OPEN");
              return Optional.of(c);
            });
    return r;
  }

  /** Simula Plutus sem modelo pago, preservando requisição e ID financeiro no MySQL. */
  @Bean
  FinancialAgentService plutus(JdbcTemplate jdbc) {
    var s = mock(FinancialAgentService.class);
    when(s.startRevenueProjection(anyLong(), any()))
        .thenAnswer(
            i -> {
              long planId = i.getArgument(0);
              var request = (StartRevenueProjectionRequest) i.getArgument(1);
              var holder = new org.springframework.jdbc.support.GeneratedKeyHolder();
              jdbc.update(
                  connection -> {
                    var q =
                        connection.prepareStatement(
                            "INSERT INTO fixture_finance(plan_id,status,context_json) VALUES (?,'PENDING',?)",
                            java.sql.Statement.RETURN_GENERATED_KEYS);
                    q.setLong(1, planId);
                    q.setString(2, request.decisionContext());
                    return q;
                  },
                  holder);
              long id = holder.getKey().longValue();
              jdbc.update("INSERT INTO financial_agent_execution VALUES (?)", id);
              return new FinancialAgentExecutionResponse(
                  id,
                  planId,
                  FinancialAgentExecutionStatus.PENDING,
                  "READ_ONLY_REVENUE_PROJECTION",
                  1,
                  null,
                  request.decisionContext(),
                  "{}",
                  null,
                  null,
                  "fixture",
                  null,
                  null,
                  null,
                  Instant.now());
            });
    return s;
  }

  /** Reconstitui o parecer simulado após reinício sem usar memórias soltas do processo Java. */
  @Bean
  FinancialAgentExecutionRepository financial(JdbcTemplate jdbc, ObjectMapper json) {
    var r = mock(FinancialAgentExecutionRepository.class);
    when(r.findById(anyLong()))
        .thenAnswer(
            i -> {
              var rows =
                  jdbc.queryForList(
                      "SELECT * FROM fixture_finance WHERE id=?", (Long) i.getArgument(0));
              if (rows.isEmpty()) return Optional.empty();
              var row = rows.getFirst();
              var e = new FinancialAgentExecution();
              e.setId(((Number) row.get("id")).longValue());
              e.setCommercialPlan(plan(((Number) row.get("plan_id")).longValue()));
              e.setCommercialPlanVersion(1);
              e.setStatus(FinancialAgentExecutionStatus.valueOf((String) row.get("status")));
              e.setProjectionRequest((String) row.get("context_json"));
              var report = json.createObjectNode();
              var scenarios = report.putArray("scenarios");
              for (var scenario : json.readTree(e.getProjectionRequest()).path("economics")) {
                var s = scenarios.addObject();
                String code = scenario.path("code").asText();
                s.put("name", code.equals("FAVORABLE") ? "OPTIMISTIC" : code);
                s.put("averagePriceBrl", scenario.path("revenueBrl").decimalValue());
                s.put("contributionMarginPercent", 70);
              }
              e.setReconciliationJson(report.toString());
              return Optional.of(e);
            });
    return r;
  }

  /**
   * Responsabilidade: expor somente controles sintéticos da matriz, nunca carregados em produção.
   */
  @RestController
  @RequestMapping("/fixture")
  public static class FixtureController {
    private final JdbcTemplate jdbc;
    private final ExecutionProfileBudget budget;
    private final ExecutionProfileContext context;
    private final ExecutionProfileActivityPolicy policy;

    @org.springframework.beans.factory.annotation.Autowired
    private com.marketinghub.leadportal.service.LeadPortalExecutionProfileBudget packageBudget;

    /** Recebe apenas serviços sob teste e a fonte local. */
    public FixtureController(
        JdbcTemplate jdbc,
        ExecutionProfileBudget budget,
        ExecutionProfileContext context,
        ExecutionProfileActivityPolicy policy) {
      this.jdbc = jdbc;
      this.budget = budget;
      this.context = context;
      this.policy = policy;
    }

    /** Informa disponibilidade para o runner local. */
    @GetMapping("/health")
    public Map<String, Boolean> health() {
      return Map.of("ready", true);
    }

    /** Reproduz a reserva do pacote usando a consulta real de origem do Lead Portal. */
    @PostMapping("/package/{id}/start")
    public void startPackage(@PathVariable Long id) {
      packageBudget.reserve(id);
    }

    /** Reproduz a conferência de completude e o retorno do custo desconhecido do worker. */
    @PostMapping("/package/{id}/complete")
    public void completePackage(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
      packageBudget.requireComplete(id, body.get("outputs"));
      packageBudget.settle(id, false);
    }

    /** Verifica a mesma prova de dispensa consumida pela fila BPM. */
    @GetMapping("/omission/{id}")
    public Map<String, Boolean> omission(@PathVariable Long id) {
      String evidence =
          jdbc.queryForObject(
              "SELECT evidence FROM fixture_instances WHERE source_reference=? ORDER BY id DESC LIMIT 1",
              String.class,
              "experiment:" + id);
      var instance = new BusinessProcessActivityInstance();
      instance.setActivityDefinition(audio());
      instance.setSourceReference("experiment:" + id);
      instance.setStatus("NOT_APPLICABLE");
      instance.setObjectiveEvidenceJson(evidence);
      return Map.of(
          "omitted",
          BusinessProcessOptionalActivity.isOmitted(instance),
          "objectiveAchieved",
          instance.isObjectiveAchieved());
    }

    /** Conclui o test double de Plutus sem criar parecer comercial real. */
    @PostMapping("/finance/{id}/complete")
    public void complete(@PathVariable Long id) {
      jdbc.update("UPDATE fixture_finance SET status='COMPLETED' WHERE id=?", id);
    }

    /** Simula alteração de plano para invalidar aprovações antigas de consumo. */
    @PostMapping("/plan-version/{version}")
    public void version(@PathVariable int version) {
      jdbc.update("UPDATE fixture_settings SET version_number=? WHERE id=1", version);
    }

    /** Executa a reserva real no backend antes de qualquer provedor simulado. */
    @PostMapping("/reserve/{productId}")
    public Map<String, Object> reserve(
        @PathVariable Long productId, @RequestBody Map<String, Object> body) {
      Long id =
          budget.reserve(
              productId,
              "experiment:" + productId,
              (String) body.getOrDefault("usageKey", "fixture-package"),
              (String) body.get("operationKey"),
              "input",
              (String) body.getOrDefault("model", "gpt-image-2.5-sunburst"),
              ((Number) body.getOrDefault("units", 1)).intValue(),
              (Boolean) body.getOrDefault("testData", true));
      return Map.of("reservationId", id);
    }

    /** Simula resposta/custo de provedor para testar falha cobrada e conciliação tardia. */
    @PostMapping("/settle/{productId}/{id}")
    public void settle(
        @PathVariable Long productId,
        @PathVariable Long id,
        @RequestBody Map<String, Object> body) {
      budget.settle(
          productId,
          id,
          body.get("actualBrl") == null ? null : new BigDecimal(body.get("actualBrl").toString()),
          "fixture-provider:invoice",
          (Boolean) body.getOrDefault("failed", false));
    }

    /** Expõe o contrato que o agente receberá através do serviço canônico de tarefas. */
    @GetMapping("/context/{productId}")
    public Map<String, Object> context(@PathVariable Long productId) {
      return context.taskContext("experiment:" + productId).orElse(Map.of());
    }

    /** Reproduz a projeção real das atividades antes de um agente ou pessoa executá-las. */
    @GetMapping("/activities/{productId}")
    public List<ProductProcessActivityExecutionGroupResponse> activities(
        @PathVariable Long productId) {
      var control =
          new ProductProcessActivityExecutionControlResponse(
              "AGENT",
              "COMMAND",
              "Executar",
              "Atividade",
              true,
              "Pronto",
              false,
              null,
              null,
              null,
              null,
              null,
              null,
              List.of());
      return policy.decorate(
          productId,
          "experiment:" + productId,
          process(94103),
          List.of(
              new ProductProcessActivityExecutionGroupResponse(
                  94202L,
                  "journey",
                  "Jornada genérica",
                  "Objetivo",
                  "Dédalo",
                  1,
                  true,
                  "NOT_STARTED",
                  "Pronto",
                  false,
                  "NOT_RECORDED",
                  null,
                  null,
                  0,
                  List.of(),
                  true,
                  "Pronto",
                  control)));
    }
  }
}
