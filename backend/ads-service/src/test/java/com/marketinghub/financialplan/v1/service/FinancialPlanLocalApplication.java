package com.marketinghub.financialplan.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marketinghub.financialagent.*;
import com.marketinghub.financialagent.service.*;
import com.marketinghub.financialplan.v1.FinancialPlanRevision;
import com.marketinghub.financialplan.v1.controller.FinancialPlanController;
import com.marketinghub.planning.*;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import com.marketinghub.repository.jpa.financialplan.FinancialPlanRevisionRepository;
import com.marketinghub.repository.jpa.planning.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.producttype.ProductTypeDefinitionRepository;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.sql.Statement;
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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: homologar API e persistência reais com referências sintéticas e Plutus
 * simulado.
 */
@TestConfiguration
@Profile("financial-plan-fixture")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({
  FinancialPlanService.class,
  FinancialPlanController.class,
  FinancialPlanLocalApplication.FixtureController.class
})
public class FinancialPlanLocalApplication {
  public static final String CHANGELOG =
      "db/changelog/changesets/2026-09-15-product-financial-plan-v1.yaml";

  /** Inicia somente topologia local e ignora configurações e credenciais reais. */
  public static void main(String[] args) {
    new SpringApplication(FinancialPlanLocalApplication.class)
        .run(
            "--spring.config.location=optional:classpath:financial-plan/no-production.properties",
            "--spring.profiles.active=financial-plan-fixture",
            "--spring.datasource.url=" + jdbcUrl(),
            "--spring.datasource.username=root",
            "--spring.datasource.password=financial-plans-local-only",
            "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
            "--spring.jpa.open-in-view=false",
            "--spring.liquibase.enabled=false",
            "--spring.sql.init.mode=never",
            "--server.port=18095",
            "--server.address=127.0.0.1",
            "--spring.mvc.problemdetails.enabled=true",
            "--logging.level.root=WARN");
  }

  /** Restringe o banco da fixture à engine isolada ou loopback. */
  public static String jdbcUrl() {
    String host = System.getenv().getOrDefault("FINANCIAL_PLAN_DB_HOST", "sandbox-docker");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host))
      throw new IllegalArgumentException("Banco não local");
    return "jdbc:mysql://"
        + host
        + ":18515/financial_plans_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
  }

  /** Cria referências mínimas e aplica o changelog produtivo com rollback próprio. */
  @Bean
  liquibase.integration.spring.SpringLiquibase liquibase(DataSource dataSource) {
    var jdbc = new JdbcTemplate(dataSource);
    for (String table :
        List.of(
            "product", "product_type_definition", "commercial_plan", "financial_agent_execution"))
      jdbc.execute(
          "CREATE TABLE IF NOT EXISTS " + table + " (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
    for (long id = 95101; id <= 95120; id++) {
      jdbc.update("INSERT IGNORE INTO product VALUES (?)", id);
      jdbc.update("INSERT IGNORE INTO commercial_plan VALUES (?)", id);
    }
    jdbc.update("INSERT IGNORE INTO product_type_definition VALUES (951),(952)");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS fixture_plan_version (id BIGINT PRIMARY KEY, version_number INT NOT NULL) ENGINE=InnoDB");
    for (long id = 95101; id <= 95120; id++)
      jdbc.update("INSERT IGNORE INTO fixture_plan_version VALUES (?,1)", id);
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS fixture_review (id BIGINT AUTO_INCREMENT PRIMARY KEY, plan_id BIGINT, version_number INT, status VARCHAR(30), context_json LONGTEXT, cost DECIMAL(12,6)) ENGINE=InnoDB");
    var lb = new liquibase.integration.spring.SpringLiquibase();
    lb.setDataSource(dataSource);
    lb.setChangeLog("classpath:" + CHANGELOG);
    return lb;
  }

  /** Valida a entidade real contra o schema MySQL 5.7 sem criação automática pelo Hibernate. */
  @Bean
  @DependsOn("liquibase")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
    var f = new LocalContainerEntityManagerFactoryBean();
    f.setDataSource(source);
    f.setManagedTypes(PersistenceManagedTypes.of(FinancialPlanRevision.class.getName()));
    f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    f.setJpaPropertyMap(
        Map.of(
            "hibernate.hbm2ddl.auto",
            "validate",
            "hibernate.dialect",
            "org.hibernate.community.dialect.MySQL57Dialect",
            "hibernate.jdbc.time_zone",
            "UTC"));
    return f;
  }

  /** Coordena locks JDBC de referências e gravações JPA na mesma transação. */
  @Bean
  PlatformTransactionManager transactionManager(EntityManagerFactory f) {
    return new JpaTransactionManager(f);
  }

  /** Fornece o repository real com queries e lock canônicos. */
  @Bean
  FinancialPlanRevisionRepository revisions(EntityManagerFactory f) {
    return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(f))
        .getRepository(FinancialPlanRevisionRepository.class);
  }

  /** Monta somente identidades sintéticas, mantendo o tipo diferente para testar isolamento. */
  private Product product(long id) {
    var p = new Product();
    p.setId(id);
    p.setInternalName("Produto sintético " + id);
    p.setName("Produto local " + id);
    var type = type(id == 95103 ? 952 : 951);
    p.setProductTypeDefinition(type);
    return p;
  }

  /** Declara tipos fictícios sem copiar produtos produtivos. */
  private ProductTypeDefinition type(long id) {
    var t = new ProductTypeDefinition();
    t.setId(id);
    t.setName("Tipo local " + id);
    t.setInternalName("Mineral sintético " + id);
    return t;
  }

  /** Simula catálogo, mas mantém lock de produto real para testar concorrência. */
  @Bean
  ProductRepository products(DataSource source) {
    var repo = mock(ProductRepository.class);
    var jdbc = new JdbcTemplate(source);
    when(repo.findAll())
        .thenAnswer(
            i ->
                java.util.stream.LongStream.rangeClosed(95101, 95120)
                    .mapToObj(this::product)
                    .toList());
    when(repo.findById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              return id >= 95101 && id <= 95120 ? Optional.of(product(id)) : Optional.empty();
            });
    when(repo.findLockedById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              jdbc.queryForObject("SELECT id FROM product WHERE id=? FOR UPDATE", Long.class, id);
              return repo.findById(id);
            });
    return repo;
  }

  /** Simula catálogo e mantém lock de tipo real para preservar revisões concorrentes. */
  @Bean
  ProductTypeDefinitionRepository types(DataSource source) {
    var repo = mock(ProductTypeDefinitionRepository.class);
    var jdbc = new JdbcTemplate(source);
    when(repo.findAllByOrderByNameAsc()).thenReturn(List.of(type(951), type(952)));
    when(repo.findById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              return id == 951 || id == 952 ? Optional.of(type(id)) : Optional.empty();
            });
    when(repo.findLockedById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              jdbc.queryForObject(
                  "SELECT id FROM product_type_definition WHERE id=? FOR UPDATE", Long.class, id);
              return repo.findById(id);
            });
    return repo;
  }

  /** Monta plano comercial fictício para o proprietário correspondente. */
  private static CommercialPlan plan(long id) {
    var p = new CommercialPlan();
    p.setId(id);
    p.setName("Plano comercial sintético " + id);
    return p;
  }

  /** Mantém relações comerciais segregadas por produto. */
  @Bean
  CommercialPlanRepository plans() {
    var repo = mock(CommercialPlanRepository.class);
    when(repo.findIdsByProductId(anyLong())).thenAnswer(i -> List.of((Long) i.getArgument(0)));
    when(repo.findByProductId(anyLong())).thenAnswer(i -> List.of(plan(i.getArgument(0))));
    return repo;
  }

  /** Persiste a versão comercial simulada para testar obsolescência e reinício. */
  @Bean
  CommercialPlanVersionRepository versions(DataSource source) {
    var repo = mock(CommercialPlanVersionRepository.class);
    var jdbc = new JdbcTemplate(source);
    when(repo.findTopByPlanIdOrderByVersionNumberDesc(anyLong()))
        .thenAnswer(
            i -> {
              var v = new CommercialPlanVersion();
              v.setVersionNumber(
                  jdbc.queryForObject(
                      "SELECT version_number FROM fixture_plan_version WHERE id=?",
                      Integer.class,
                      (Long) i.getArgument(0)));
              return Optional.of(v);
            });
    return repo;
  }

  /** Simula a fronteira da fila Plutus sem invocar modelos ou endpoints externos. */
  @Bean
  FinancialAgentService plutus(DataSource source) {
    var service = mock(FinancialAgentService.class);
    var jdbc = new JdbcTemplate(source);
    when(service.startRevenueProjection(anyLong(), any()))
        .thenAnswer(
            i -> {
              Long planId = i.getArgument(0);
              StartRevenueProjectionRequest request = i.getArgument(1);
              int version =
                  jdbc.queryForObject(
                      "SELECT version_number FROM fixture_plan_version WHERE id=?",
                      Integer.class,
                      planId);
              var key = new GeneratedKeyHolder();
              jdbc.update(
                  connection -> {
                    var s =
                        connection.prepareStatement(
                            "INSERT INTO fixture_review(plan_id,version_number,status,context_json) VALUES (?,?,'PENDING',?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, planId);
                    s.setInt(2, version);
                    s.setString(3, request.decisionContext());
                    return s;
                  },
                  key);
              long id = Objects.requireNonNull(key.getKey()).longValue();
              jdbc.update("INSERT INTO financial_agent_execution VALUES (?)", id);
              return new FinancialAgentExecutionResponse(
                  id,
                  planId,
                  FinancialAgentExecutionStatus.PENDING,
                  "READ_ONLY_REVENUE_PROJECTION",
                  version,
                  id,
                  request.decisionContext(),
                  "{}",
                  null,
                  null,
                  "fixture-no-paid-model",
                  null,
                  null,
                  null,
                  Instant.now());
            });
    return service;
  }

  /** Recupera status, parecer completo e custos simulados vinculados à revisão exata. */
  @Bean
  FinancialAgentExecutionRepository executions(DataSource source) throws java.io.IOException {
    var repo = mock(FinancialAgentExecutionRepository.class);
    var jdbc = new JdbcTemplate(source);
    String projection;
    try (var resource = getClass().getResourceAsStream("/financial-plan/plutus-response.json")) {
      projection =
          new String(
              Objects.requireNonNull(resource).readAllBytes(),
              java.nio.charset.StandardCharsets.UTF_8);
    }
    when(repo.findById(anyLong()))
        .thenAnswer(
            i -> {
              var rows =
                  jdbc.queryForList(
                      "SELECT * FROM fixture_review WHERE id=?", (Long) i.getArgument(0));
              if (rows.isEmpty()) return Optional.empty();
              var row = rows.getFirst();
              var e = new FinancialAgentExecution();
              e.setId(((Number) row.get("id")).longValue());
              e.setCommercialPlan(plan(((Number) row.get("plan_id")).longValue()));
              e.setCommercialPlanVersion(((Number) row.get("version_number")).intValue());
              e.setStatus(FinancialAgentExecutionStatus.valueOf((String) row.get("status")));
              e.setEstimatedCost((BigDecimal) row.get("cost"));
              e.setProjectionRequest((String) row.get("context_json"));
              e.setModel("fixture-no-paid-model");
              if (e.getStatus() == FinancialAgentExecutionStatus.COMPLETED) {
                e.setDailyReport(
                    "Parecer simulado: premissas revisadas no cenário local, sem comprovar lucro ou autorizar publicação.");
                e.setReconciliationJson(projection);
              }
              if (e.getStatus() == FinancialAgentExecutionStatus.FAILED)
                e.setErrorMessage(
                    "Falha técnica simulada; não repetir consumo com a mesma entrada.");
              return Optional.of(e);
            });
    return repo;
  }

  /** Comandos restritos à fixture local, ausentes da aplicação produtiva. */
  @RestController
  @RequestMapping("/fixture")
  static class FixtureController {
    private final JdbcTemplate jdbc;

    /** Recebe banco isolado para controlar respostas sintéticas. */
    FixtureController(DataSource source) {
      jdbc = new JdbcTemplate(source);
    }

    /** Confirma prontidão sem tocar integrações reais. */
    @GetMapping("/health")
    public Map<String, String> health() {
      return Map.of("status", "local-only");
    }

    /** Simula conclusão ou falha da avaliação que o worker reportaria. */
    @PostMapping("/reviews/{id}/{status}")
    public void review(@PathVariable Long id, @PathVariable String status) {
      if (!Set.of("COMPLETED", "FAILED").contains(status))
        throw new IllegalArgumentException("Status inválido");
      jdbc.update("UPDATE fixture_review SET status=?,cost=0.015 WHERE id=?", status, id);
    }

    /** Permite conferir quantitativo e contexto para provar deduplicação da fila. */
    @GetMapping("/reviews")
    public List<Map<String, Object>> reviews() {
      return jdbc.queryForList("SELECT * FROM fixture_review ORDER BY id");
    }

    /** Simula revisão comercial sem substituir produto ou histórico financeiro. */
    @PostMapping("/plans/{id}/version/{version}")
    public void version(@PathVariable Long id, @PathVariable int version) {
      jdbc.update("UPDATE fixture_plan_version SET version_number=? WHERE id=?", version, id);
    }
  }
}
