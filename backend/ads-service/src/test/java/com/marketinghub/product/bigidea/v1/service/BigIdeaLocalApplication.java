package com.marketinghub.product.bigidea.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marketinghub.product.Product;
import com.marketinghub.product.bigidea.v1.*;
import com.marketinghub.product.bigidea.v1.controller.ProductBigIdeaController;
import com.marketinghub.repository.jpa.product.*;
import jakarta.persistence.EntityManagerFactory;
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

/** Homologa API e persistência reais com produtos sintéticos e transações concorrentes. */
@TestConfiguration
@Profile("big-idea-fixture")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({
  BigIdeaService.class,
  ProductBigIdeaController.class,
  BigIdeaLocalApplication.FixtureController.class
})
public class BigIdeaLocalApplication {
  /** Inicia a fixture isolada sem ler configuração ou credenciais produtivas. */
  public static void main(String[] args) {
    String host = System.getenv().getOrDefault("BIG_IDEA_DB_HOST", "sandbox-docker");
    if (!Set.of("sandbox-docker", "127.0.0.1").contains(host))
      throw new IllegalArgumentException("Banco não local");
    new SpringApplication(BigIdeaLocalApplication.class)
        .run(
            "--spring.config.location=optional:classpath:big-idea/no-production.properties",
            "--spring.profiles.active=big-idea-fixture",
            "--spring.datasource.url=jdbc:mysql://"
                + host
                + ":18529/big_idea_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            "--spring.datasource.username=root",
            "--spring.datasource.password=big-idea-local-only",
            "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
            "--spring.jpa.open-in-view=false",
            "--spring.liquibase.enabled=false",
            "--spring.sql.init.mode=never",
            "--server.port=18099",
            "--server.address=127.0.0.1",
            "--spring.mvc.problemdetails.enabled=true",
            "--logging.level.root=WARN");
  }

  /** Cria referências sintéticas e aplica exatamente os dois changelogs produtivos. */
  @Bean
  liquibase.integration.spring.SpringLiquibase liquibase(DataSource source) {
    var jdbc = new JdbcTemplate(source);
    jdbc.execute("CREATE TABLE IF NOT EXISTS product(id BIGINT PRIMARY KEY) ENGINE=InnoDB");
    jdbc.execute("CREATE TABLE IF NOT EXISTS agent_task(id BIGINT PRIMARY KEY) ENGINE=InnoDB");
    for (long id = 99101; id <= 99105; id++)
      jdbc.update("INSERT IGNORE INTO product(id) VALUES (?)", id);
    jdbc.execute("INSERT IGNORE INTO agent_task(id) VALUES (90001)");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS business_process_definition(id BIGINT AUTO_INCREMENT PRIMARY KEY,process_code VARCHAR(100),name VARCHAR(160),purpose TEXT,owner_name VARCHAR(160),trigger_description TEXT,outcome_description TEXT,version_number INT,status VARCHAR(20),technical_reference TEXT,process_type VARCHAR(40),parent_process_code VARCHAR(100),execution_scope VARCHAR(40),diagram_json LONGTEXT,created_at DATETIME(6),published_at DATETIME(6),UNIQUE KEY uk_process(process_code,version_number)) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS business_process_activity_definition(id BIGINT AUTO_INCREMENT PRIMARY KEY,process_definition_id BIGINT,activity_id VARCHAR(100),name VARCHAR(160),objective TEXT,owner_name VARCHAR(160),execution_resource_code VARCHAR(100),subprocess_code VARCHAR(100),definition_json LONGTEXT,created_at DATETIME(6)) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS business_process_chain_definition(id BIGINT AUTO_INCREMENT PRIMARY KEY,chain_code VARCHAR(100),name VARCHAR(160),purpose TEXT,outcome_description VARCHAR(500),primary_metric VARCHAR(200),version_number INT,status VARCHAR(20),created_at DATETIME(6),published_at DATETIME(6),UNIQUE KEY uk_chain(chain_code,version_number)) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS business_process_chain_item(id BIGINT AUTO_INCREMENT PRIMARY KEY,chain_definition_id BIGINT,process_definition_id BIGINT,sequence_number INT,value_contribution VARCHAR(500),created_at DATETIME(6)) ENGINE=InnoDB");
    if (jdbc.queryForObject("SELECT COUNT(*) FROM business_process_definition", Integer.class)
        == 0) {
      for (int i = 1; i <= 6; i++) {
        String code = i == 2 ? "pde-commercial-plan-offer" : "fixture-process-" + i;
        jdbc.update(
            "INSERT INTO business_process_definition(id,process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,technical_reference,process_type,execution_scope,diagram_json,created_at,published_at) VALUES (?,?,?,'Valor','Atena','Inicio','Entrega',?,'PUBLISHED','fixture','PROCESS','PRODUCT',?,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6))",
            i,
            code,
            "Processo " + i,
            i == 2 ? 12 : 1,
            "{\"nodes\":[{\"id\":\"start\"},{\"id\":\"marketStrategy\",\"description\":\"Estrategia existente\"}]}");
      }
      for (String activity : List.of("marketStrategy", "economics", "productArchitecture"))
        jdbc.update(
            "INSERT INTO business_process_activity_definition(process_definition_id,activity_id,name,objective,owner_name,definition_json,created_at) VALUES(2,?,?,'Contrato original','Atena',?,UTC_TIMESTAMP(6))",
            activity,
            activity,
            "{\"id\":\"" + activity + "\",\"description\":\"Contrato original\"}");
      jdbc.execute(
          "INSERT INTO business_process_chain_definition(id,chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at,published_at) VALUES(26,'pde-value-creation-delivery','Cadeia','Valor','Entrega','Contribuicao',26,'PUBLISHED',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6))");
      for (int i = 1; i <= 6; i++)
        jdbc.update(
            "INSERT INTO business_process_chain_item(chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at) VALUES(26,?,?,'Entrega',UTC_TIMESTAMP(6))",
            i,
            i);
    }
    var lb = new liquibase.integration.spring.SpringLiquibase();
    lb.setDataSource(source);
    lb.setChangeLog("classpath:big-idea-fixture-changelog.yaml");
    return lb;
  }

  /** Valida os mapeamentos reais no MySQL 5.7 sem geração automática de schema. */
  @Bean
  @DependsOn("liquibase")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
    var f = new LocalContainerEntityManagerFactoryBean();
    f.setDataSource(source);
    f.setManagedTypes(
        PersistenceManagedTypes.of(
            ProductBigIdea.class.getName(), ProductBigIdeaBinding.class.getName()));
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

  /** Coordena locks JDBC e revisões JPA na mesma transação. */
  @Bean
  PlatformTransactionManager transactionManager(EntityManagerFactory f) {
    return new JpaTransactionManager(f);
  }

  /** Usa o repository produtivo das revisões. */
  @Bean
  ProductBigIdeaRepository revisions(EntityManagerFactory f) {
    return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(f))
        .getRepository(ProductBigIdeaRepository.class);
  }

  /** Usa o repository produtivo das referências fixadas. */
  @Bean
  ProductBigIdeaBindingRepository bindings(EntityManagerFactory f) {
    return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(f))
        .getRepository(ProductBigIdeaBindingRepository.class);
  }

  /** Mantém lock e ponteiro reais e simula somente o restante do cadastro de produto. */
  @Bean
  ProductRepository products(DataSource source, ProductBigIdeaRepository revisions) {
    var repo = mock(ProductRepository.class);
    var jdbc = new JdbcTemplate(source);
    when(repo.findById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              var rows =
                  jdbc.queryForList("SELECT id,current_big_idea_id FROM product WHERE id=?", id);
              if (rows.isEmpty()) return Optional.empty();
              var p = new Product();
              p.setId(id);
              p.setName("Produto sintético " + id);
              Object revision = rows.getFirst().get("current_big_idea_id");
              if (revision != null)
                p.setBigIdea(revisions.findById(((Number) revision).longValue()).orElseThrow());
              return Optional.of(p);
            });
    when(repo.findLockedById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              jdbc.queryForList("SELECT id FROM product WHERE id=? FOR UPDATE", id);
              return repo.findById(id);
            });
    when(repo.save(any()))
        .thenAnswer(
            i -> {
              Product p = i.getArgument(0);
              jdbc.update(
                  "UPDATE product SET current_big_idea_id=? WHERE id=?",
                  p.getBigIdea().getId(),
                  p.getId());
              return p;
            });
    return repo;
  }

  /** Oferece somente diagnóstico e disparos sintéticos para a matriz local. */
  @RestController
  static class FixtureController {
    private final BigIdeaService service;
    private final JdbcTemplate jdbc;

    /** Recebe serviços locais sem dependências externas. */
    FixtureController(BigIdeaService service, DataSource source) {
      this.service = service;
      this.jdbc = new JdbcTemplate(source);
    }

    /** Indica prontidão local. */
    @GetMapping("/fixture/health")
    Map<String, String> health() {
      return Map.of("status", "UP");
    }

    /** Consulta produto sintético para a tela real. */
    @GetMapping("/api/products/{id}")
    Map<String, Object> product(@PathVariable Long id) {
      return Map.of("id", id, "internalName", "Produto sintético " + id);
    }

    /** Fixa um contexto sintético pelo service real. */
    @PostMapping("/fixture/pin/{id}")
    Object pin(@PathVariable Long id, @RequestBody Map<String, String> body) {
      return service.pin(id, body.get("reference"));
    }

    /** Confere publicação versionada e preservação da composição antiga. */
    @GetMapping("/fixture/chain")
    Object chain() {
      return jdbc.queryForList(
          "SELECT c.version_number,c.status,COUNT(i.id) items FROM business_process_chain_definition c JOIN business_process_chain_item i ON i.chain_definition_id=c.id GROUP BY c.id ORDER BY c.version_number");
    }
  }
}
