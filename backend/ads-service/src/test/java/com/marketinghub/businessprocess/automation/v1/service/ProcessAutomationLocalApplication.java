package com.marketinghub.businessprocess.automation.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.automation.v1.*;
import com.marketinghub.businessprocess.automation.v1.controller.ProcessRunController;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.*;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequestResponse;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
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
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: homologar o coordenador real com MySQL e contratos de agentes simulados. */
@TestConfiguration
@Profile("process-automation-fixture")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({
  ProcessRunService.class,
  ProcessRunContext.class,
  ProcessRunNavigation.class,
  ProcessRunController.class,
  ProcessAutomationLocalApplication.FixtureController.class
})
public class ProcessAutomationLocalApplication {
  /** Inicia somente a aplicação de teste, sem importar agendamentos ou credenciais produtivas. */
  public static void main(String[] args) {
    start();
  }

  /** Fixa portas e banco local acima das variáveis de produção herdadas do ambiente. */
  static org.springframework.context.ConfigurableApplicationContext start() {
    String host = System.getenv().getOrDefault("PROCESS_TEST_DB_HOST", "sandbox-docker");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host))
      throw new IllegalArgumentException("Host de teste inválido.");
    var app = new SpringApplication(ProcessAutomationLocalApplication.class);
    return app.run(
        "--spring.config.location=optional:classpath:process-automation/no-production.properties",
        "--spring.profiles.active=process-automation-fixture",
        "--spring.datasource.url=jdbc:mysql://"
            + host
            + ":18312/process_automation_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
        "--spring.datasource.username=root",
        "--spring.datasource.password=process-local-only",
        "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "--spring.jpa.open-in-view=true",
        "--spring.liquibase.enabled=false",
        "--spring.sql.init.mode=never",
        "--server.port=18092",
        "--server.address=127.0.0.1",
        "--process-execution.worker-token=process-fixture-only",
        "--spring.mvc.problemdetails.enabled=true",
        "--logging.level.root=WARN",
        "--spring.main.banner-mode=off");
  }

  /** Aplica a migração real sobre tabelas mínimas segregadas da aplicação produtiva. */
  @Bean
  liquibase.integration.spring.SpringLiquibase liquibase(DataSource source) {
    var jdbc = new JdbcTemplate(source);
    for (String table :
        List.of(
            "product",
            "business_process_definition",
            "business_process_chain_definition",
            "learning_sales_cycle_v1"))
      jdbc.execute(
          "CREATE TABLE IF NOT EXISTS " + table + " (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS fixture_product (id BIGINT PRIMARY KEY, enabled BIT NOT NULL DEFAULT 1, version_number INT NOT NULL DEFAULT 1, cycle_status VARCHAR(32) NOT NULL DEFAULT 'OPEN', destination_code VARCHAR(32) NOT NULL DEFAULT 'LANDING') ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS fixture_task (id BIGINT AUTO_INCREMENT PRIMARY KEY, product_id BIGINT NOT NULL, process_id BIGINT NOT NULL, activity_id VARCHAR(100) NOT NULL, status VARCHAR(32) NOT NULL, achieved BIT NOT NULL DEFAULT 0, reason VARCHAR(300) NOT NULL, KEY ix_fixture_task(product_id,process_id,activity_id,id)) ENGINE=InnoDB");
    for (long id = 92001; id <= 92030; id++) {
      jdbc.update("INSERT IGNORE INTO product VALUES (?)", id);
      jdbc.update("INSERT IGNORE INTO fixture_product(id) VALUES (?)", id);
      jdbc.update("INSERT IGNORE INTO business_process_definition VALUES (?)", id);
      jdbc.update("INSERT IGNORE INTO learning_sales_cycle_v1 VALUES (?)", id);
    }
    jdbc.update("INSERT IGNORE INTO business_process_chain_definition VALUES (92014)");
    var migration = new liquibase.integration.spring.SpringLiquibase();
    migration.setDataSource(source);
    migration.setChangeLog(
        "classpath:db/changelog/changesets/2026-09-12-product-process-automation-v1.yaml");
    return migration;
  }

  /** Valida as entidades novas contra o schema físico aplicado pelo Liquibase. */
  @Bean
  @DependsOn("liquibase")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
    factory.setManagedTypes(
        PersistenceManagedTypes.of(ProcessRun.class.getName(), ProcessRunEvent.class.getName()));
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

  /** Habilita transações reais com o mesmo DataSource usado pelos locks e tarefas simuladas. */
  @Bean
  PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
    return new JpaTransactionManager(factory);
  }

  /** Reproduz explicitamente o contexto JPA da requisição usado no backend publicado. */
  @Bean
  org.springframework.web.servlet.config.annotation.WebMvcConfigurer persistenceRequestContext(
      EntityManagerFactory factory) {
    var interceptor = new org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor();
    interceptor.setEntityManagerFactory(factory);
    return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
      /**
       * Mantém o contexto entre as transações da mesma requisição para testar leitura após lock.
       */
      @Override
      public void addInterceptors(
          org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addWebRequestInterceptor(interceptor);
      }
    };
  }

  /** Cria repositório JPA sobre o contexto transacional compartilhado. */
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

  /** Persiste o controle durável real. */
  @Bean
  ProcessRunRepository runs(EntityManagerFactory factory) {
    return repository(factory, ProcessRunRepository.class);
  }

  /** Persiste o diário e a deduplicação reais. */
  @Bean
  ProcessRunEventRepository events(EntityManagerFactory factory) {
    return repository(factory, ProcessRunEventRepository.class);
  }

  /** Simula apenas a escrita da prova do filho no pai, sem inferir conclusão durante leitura. */
  @Bean
  ProcessRunSubprocesses subprocesses(JdbcTemplate jdbc) {
    var subprocesses = mock(ProcessRunSubprocesses.class);
    doAnswer(
            inv -> {
              ProcessRun parent = inv.getArgument(0);
              ProductProcessActivityExecutionGroupResponse activity = inv.getArgument(1);
              jdbc.update(
                  "INSERT INTO fixture_task(product_id,process_id,activity_id,status,achieved,reason) VALUES (?,?,?,?,?,?)",
                  parent.getProductId(),
                  parent.getProcessDefinitionId(),
                  activity.activityId(),
                  "COMPLETED",
                  true,
                  "Objetivo comprovado pelo subprocesso");
              return null;
            })
        .when(subprocesses)
        .complete(any(), any(), any(), any());
    return subprocesses;
  }

  /** Compartilha a conexão transacional com os contratos de teste. */
  @Bean
  JdbcTemplate jdbc(DataSource source) {
    return new JdbcTemplate(source);
  }

  /** Isola a projeção de ciclos exigida pela classe canônica simulada. */
  @Bean
  com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleActivityProjection
      cycleProjection() {
    return mock(
        com.marketinghub.businessprocesschain.learningcycle.v1.service
            .LearningCycleActivityProjection.class);
  }

  /** Simula somente o catálogo, usando SELECT FOR UPDATE real para coordenar o produto. */
  @Bean
  ProductRepository products(JdbcTemplate jdbc) {
    var products = mock(ProductRepository.class);
    when(products.findById(anyLong())).thenAnswer(inv -> product(jdbc, inv.getArgument(0), false));
    when(products.findLockedById(anyLong()))
        .thenAnswer(inv -> product(jdbc, inv.getArgument(0), true));
    return products;
  }

  /** Retorna o produto sintético preservando o lock de linha dentro da transação JPA. */
  static Optional<Product> product(JdbcTemplate jdbc, Long id, boolean locked) {
    var values =
        jdbc.queryForList(
            "SELECT * FROM fixture_product WHERE id=?" + (locked ? " FOR UPDATE" : ""), id);
    if (values.isEmpty()) return Optional.empty();
    var row = values.getFirst();
    var product = new Product();
    product.setId(id);
    product.setInternalName("Produto local " + id);
    product.setAutomaticExecutionEnabled(Boolean.TRUE.equals(row.get("enabled")));
    product.setValidationDefinitionJson("{\"version\":" + row.get("version_number") + "}");
    return Optional.of(product);
  }

  /** Simula versões de processo mantendo ordem do grafo diferente da lista de atividades. */
  @Bean
  BusinessProcessDefinitionRepository processes() {
    var processes = mock(BusinessProcessDefinitionRepository.class);
    when(processes.findById(anyLong()))
        .thenAnswer(inv -> Optional.of(definition(inv.getArgument(0))));
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "local-process-92005", "PUBLISHED"))
        .thenReturn(Optional.of(definition(92005L)));
    return processes;
  }

  /** Declara fluxos locais felizes, incompletos, de aprovação, subprocesso e recuperação. */
  static BusinessProcessDefinition definition(Long id) {
    var process = new BusinessProcessDefinition();
    process.setId(id);
    process.setProcessCode("local-process-" + id);
    process.setName("Processo de teste " + id);
    process.setVersionNumber(1);
    process.setStatus("PUBLISHED");
    process.setExecutionScope("PRODUCT");
    String rework =
        id == 92003
            ? ", {\"id\":\"fix\",\"type\":\"TASK\",\"activationMode\":\"ON_FUNCTIONAL_REJECTION\",\"remediatesActivities\":[\"a\"]}"
            : "";
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"b\",\"type\":\"TASK\"},{\"id\":\"a\",\"type\":\"TASK\""
            + (id == 92004 ? ",\"subprocessCode\":\"local-process-92005\"" : "")
            + "},{\"id\":\"gate\",\"type\":\"TASK\"}"
            + rework
            + (id == 92011 ? ", {\"id\":\"missing\",\"type\":\"TASK\"}" : "")
            + "],\"flows\":[{\"from\":\"a\",\"to\":\"b\"},{\"from\":\"b\",\"to\":\"gate\"}]}");
    return process;
  }

  /** Simula a composição da cadeia sem inventar vínculos fora do BPM. */
  @Bean
  BusinessProcessChainDefinitionRepository chains() {
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var chain = new BusinessProcessChainDefinition();
    chain.setId(92014L);
    chain.setStatus("PUBLISHED");
    for (long id = 92001; id <= 92030; id++) {
      var item = new BusinessProcessChainItem();
      item.setProcessDefinition(definition(id));
      item.setSequenceNumber((int) (id - 92000));
      chain.getItems().add(item);
    }
    when(chains.findById(92014L)).thenReturn(Optional.of(chain));
    return chains;
  }

  /** Simula identidade e encerramento do ciclo para separar acompanhamento de novos comandos. */
  @Bean
  LearningSalesCycleRepository cycles(JdbcTemplate jdbc) {
    var cycles = mock(LearningSalesCycleRepository.class);
    when(cycles.findById(anyLong()))
        .thenAnswer(
            inv -> {
              Long id = inv.getArgument(0);
              var cycle = new LearningSalesCycle();
              cycle.setId(id);
              cycle.setProductId(id);
              cycle.setChainDefinitionId(92014L);
              cycle.setExperimentId(id);
              cycle.setStatus(
                  jdbc.queryForObject(
                      "SELECT cycle_status FROM fixture_product WHERE id=?", String.class, id));
              cycle.setProductVersion("local-v1");
              return Optional.of(cycle);
            });
    return cycles;
  }

  /** Simula a fronteira canônica de agentes, preservando tarefas e respostas no banco local. */
  @Bean
  BusinessProcessActivityExecutionService activities(JdbcTemplate jdbc) {
    var activities = mock(BusinessProcessActivityExecutionService.class);
    when(activities.productProcessExecutions(
            anyLong(), anyLong(), nullable(Long.class), nullable(Long.class), eq(false)))
        .thenAnswer(inv -> snapshot(jdbc, inv.getArgument(1), inv.getArgument(0)));
    when(activities.requestProductActivityExecution(
            anyLong(), anyLong(), anyString(), isNull(), nullable(Long.class)))
        .thenAnswer(
            inv -> {
              Long process = inv.getArgument(0), product = inv.getArgument(1);
              String activity = inv.getArgument(2);
              var previous = latest(jdbc, product, process, activity);
              if (previous != null
                  && Set.of("PENDING", "IN_PROGRESS").contains(previous.get("status")))
                throw new IllegalStateException("Duplicação de tarefa ativa.");
              boolean gate =
                  "gate".equals(activity)
                      || (process == 92004
                          && "a".equals(activity)
                          && privateDestination(jdbc, product));
              jdbc.update(
                  "INSERT INTO fixture_task(product_id,process_id,activity_id,status,achieved,reason) VALUES (?,?,?,?,?,?)",
                  product,
                  process,
                  activity,
                  gate ? "COMPLETED" : "PENDING",
                  gate,
                  gate ? "Gate aprovado" : "Aguardando agente local");
              if (process == 92030)
                throw new IllegalStateException("Falha após escrita para testar rollback");
              return new ProductProcessActivityExecutionRequestResponse(
                  process,
                  product,
                  activity,
                  "experiment:" + product,
                  List.of(),
                  gate ? "COMPLETED" : "PENDING",
                  gate,
                  "Contrato local executado");
            });
    return activities;
  }

  /** Consulta a última tentativa sem apagar o histórico anterior. */
  static Map<String, Object> latest(
      JdbcTemplate jdbc, Long product, Long process, String activity) {
    var tasks =
        jdbc.queryForList(
            "SELECT * FROM fixture_task WHERE product_id=? AND process_id=? AND activity_id=? ORDER BY id DESC LIMIT 1",
            product,
            process,
            activity);
    return tasks.isEmpty() ? null : tasks.getFirst();
  }

  /** Projeta prontidão, escolha de destino e objetivo, sem decidir a ordem pelo executor. */
  static ProductProcessActivityExecutionHistoryResponse snapshot(
      JdbcTemplate jdbc, Long product, Long process) {
    List<ProductProcessActivityExecutionGroupResponse> groups = new ArrayList<>();
    boolean needsFix =
        process == 92003
            && latest(jdbc, product, process, "a") != null
            && "BLOCKED".equals(latest(jdbc, product, process, "a").get("status"));
    for (String id : List.of("b", "a", "gate", "fix")) {
      if (id.equals("fix") && process != 92003) continue;
      var task = latest(jdbc, product, process, id);
      boolean achieved = task != null && Boolean.TRUE.equals(task.get("achieved"));
      String state = task == null ? "NOT_STARTED" : task.get("status").toString();
      boolean selected = !id.equals("fix") || needsFix || task != null;
      String interaction =
          process == 92002 && id.equals("b")
              ? "APPROVAL"
              : process == 92004 && id.equals("a") && !privateDestination(jdbc, product)
                  ? "SUBPROCESS"
                  : process == 92010 && id.equals("gate") ? "WORKSPACE" : "COMMAND";
      boolean available =
          !achieved
              && Set.of("NOT_STARTED", "BLOCKED").contains(state)
              && (!id.equals("fix") || needsFix);
      var control =
          new ProductProcessActivityExecutionControlResponse(
              interaction.equals("APPROVAL")
                  ? "HUMAN"
                  : id.equals("gate")
                          || (process == 92004
                              && id.equals("a")
                              && privateDestination(jdbc, product))
                      ? "BACKEND"
                      : "AGENT",
              interaction,
              "Executar atividade",
              "Contrato local",
              available,
              "Preencha os critérios da atividade",
              interaction.equals("APPROVAL"),
              null,
              null,
              null,
              null,
              null,
              interaction.equals("SUBPROCESS") ? 92005L : null,
              List.of(),
              null,
              null,
              process == 92004 && id.equals("a") && privateDestination(jdbc, product)
                  ? "https://local.example/private"
                  : null);
      groups.add(
          new ProductProcessActivityExecutionGroupResponse(
              (long) groups.size() + 1,
              id,
              "Atividade " + id,
              "Objetivo " + id,
              id.equals("gate") ? "Backend" : "Agente local",
              id.equals("a") ? 1 : id.equals("b") ? 2 : 3,
              selected,
              achieved ? "COMPLETED" : state,
              task == null ? "Sem tarefa" : task.get("reason").toString(),
              achieved,
              "LOCAL_CONTRACT",
              task == null ? null : ((Number) task.get("id")).longValue(),
              1,
              task == null ? 0 : 1,
              List.of(),
              available,
              "Contrato local",
              control));
    }
    int total =
        (int)
            groups.stream()
                .filter(ProductProcessActivityExecutionGroupResponse::selectedVersionActivity)
                .count();
    int completed =
        (int)
            groups.stream()
                .filter(a -> a.selectedVersionActivity() && a.objectiveAchieved())
                .count();
    return new ProductProcessActivityExecutionHistoryResponse(
        product,
        "Produto sintético",
        "Produto local " + product,
        null,
        null,
        process,
        definition(process).getProcessCode(),
        definition(process).getName(),
        1,
        "PUBLISHED",
        "experiment:" + product,
        completed == total ? "COMPLETED" : "IN_PROGRESS",
        completed == total,
        total,
        completed,
        total - completed,
        0,
        null,
        null,
        null,
        null,
        groups.size(),
        0,
        0,
        BigDecimal.ZERO,
        "NOT_REPORTED",
        groups);
  }

  /** Simula a decisão persistida de reutilizar o destino, sem mudar o grafo ou aprovar um filho. */
  private static boolean privateDestination(JdbcTemplate jdbc, Long product) {
    return "PRIVATE_PDE"
        .equals(
            jdbc.queryForObject(
                "SELECT destination_code FROM fixture_product WHERE id=?", String.class, product));
  }

  /** Responsabilidade: fornecer callbacks e consultas de teste, ausentes na aplicação produtiva. */
  @RestController
  static class FixtureController {
    private final JdbcTemplate jdbc;

    /** Compartilha somente o banco descartável da fixture. */
    FixtureController(JdbcTemplate jdbc) {
      this.jdbc = jdbc;
    }

    /** Projeta o histórico que a tela real utiliza para os cards. */
    @GetMapping("/api/business-processes/{process}/products/{product}/activity-executions")
    Object history(@PathVariable Long product, @PathVariable Long process) {
      return snapshot(jdbc, product, process);
    }

    /** Lista pendências do agente simulado sem consumir dados reais. */
    @GetMapping("/fixture/tasks")
    Object tasks() {
      return jdbc.queryForList("SELECT * FROM fixture_task ORDER BY id");
    }

    /** Persiste resultado independente recebido do agente de teste. */
    @PostMapping("/fixture/{product}/{process}/{activity}")
    Object callback(
        @PathVariable Long product,
        @PathVariable Long process,
        @PathVariable String activity,
        @RequestBody Map<String, Object> result) {
      var task = latest(jdbc, product, process, activity);
      if (task == null) throw new IllegalArgumentException("Tarefa não encontrada.");
      jdbc.update(
          "UPDATE fixture_task SET status=?,achieved=?,reason=? WHERE id=?",
          result.getOrDefault("status", "COMPLETED"),
          result.getOrDefault("achieved", true),
          result.getOrDefault("reason", "Objetivo comprovado"),
          task.get("id"));
      return Map.of("ok", true);
    }

    /** Simula decisão humana separada do comando de iniciar o processo. */
    @PostMapping("/fixture/{product}/{process}/approval")
    Object approve(@PathVariable Long product, @PathVariable Long process) {
      jdbc.update(
          "INSERT INTO fixture_task(product_id,process_id,activity_id,status,achieved,reason) VALUES (?,?,'b','COMPLETED',1,'Decisão humana explícita')",
          product,
          process);
      return Map.of("ok", true);
    }

    /** Permite testar STOP, encerramento, mudança de entrada e destino sem editar a execução. */
    @PostMapping("/fixture/products/{product}")
    Object productState(@PathVariable Long product, @RequestBody Map<String, Object> state) {
      if (state.containsKey("play"))
        jdbc.update("UPDATE fixture_product SET enabled=? WHERE id=?", state.get("play"), product);
      if (state.containsKey("version"))
        jdbc.update(
            "UPDATE fixture_product SET version_number=? WHERE id=?",
            state.get("version"),
            product);
      if (state.containsKey("cycleStatus"))
        jdbc.update(
            "UPDATE fixture_product SET cycle_status=? WHERE id=?",
            state.get("cycleStatus"),
            product);
      if (state.containsKey("destination"))
        jdbc.update(
            "UPDATE fixture_product SET destination_code=? WHERE id=?",
            state.get("destination"),
            product);
      return Map.of("ok", true);
    }
  }
}
