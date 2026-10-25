package com.marketinghub.product.modeling.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.*;
import com.marketinghub.opportunitydossier.*;
import com.marketinghub.opportunitydossier.service.OpportunityDossierResearchSyncService;
import com.marketinghub.product.Product;
import com.marketinghub.product.bigidea.v1.ProductBigIdea;
import com.marketinghub.product.modeling.v1.ProductModeling;
import com.marketinghub.product.modeling.v1.controller.ProductModelingController;
import com.marketinghub.productdiscovery.v1.*;
import com.marketinghub.productdiscovery.v1.service.*;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.experiment.*;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.opportunitydossier.*;
import com.marketinghub.repository.jpa.processautomation.*;
import com.marketinghub.repository.jpa.product.*;
import com.marketinghub.repository.jpa.productdiscovery.*;
import com.marketinghub.repository.jpa.productmodeling.*;
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

/** Homologa endpoints e persistência reais com fila e provas sintéticas sem consumo externo. */
@TestConfiguration
@Profile("modeling-fixture")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({
  ModelingService.class,
  ModelingHandoff.class,
  ModelingAdoption.class,
  ProductModelingController.class,
  ModelingLocalApplication.FixtureController.class
})
public class ModelingLocalApplication {
  /** Inicia somente banco e porta locais, ignorando configurações produtivas. */
  public static void main(String[] args) {
    new SpringApplication(ModelingLocalApplication.class)
        .run(
            "--spring.config.location=optional:classpath:modeling/no-production.properties",
            "--spring.profiles.active=modeling-fixture",
            "--spring.datasource.url=jdbc:mysql://sandbox-docker:18531/modeling_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            "--spring.datasource.username=root",
            "--spring.datasource.password=modeling-local-only",
            "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
            "--spring.jpa.open-in-view=false",
            "--spring.liquibase.enabled=false",
            "--server.port=18101",
            "--server.address=127.0.0.1",
            "--logging.level.root=WARN");
  }

  /** Aplica a migração produtiva sobre referências sintéticas mínimas. */
  @Bean
  liquibase.integration.spring.SpringLiquibase liquibase(DataSource source) {
    var jdbc = new JdbcTemplate(source);
    jdbc.execute("CREATE TABLE IF NOT EXISTS product(id BIGINT PRIMARY KEY) ENGINE=InnoDB");
    jdbc.execute("CREATE TABLE IF NOT EXISTS experiment(id BIGINT PRIMARY KEY) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS learning_sales_cycle_v1(id BIGINT PRIMARY KEY) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS product_discovery_cycle(id BIGINT AUTO_INCREMENT PRIMARY KEY,status VARCHAR(32),theme VARCHAR(191),objective TEXT,reference_sources TEXT) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS fixture_audit(cycle_id BIGINT PRIMARY KEY) ENGINE=InnoDB");
    for (long id : List.of(99101L, 99102L)) {
      jdbc.update("INSERT IGNORE INTO product(id) VALUES (?)", id);
      jdbc.update("INSERT IGNORE INTO learning_sales_cycle_v1(id) VALUES (?)", id + 100);
      jdbc.update("INSERT IGNORE INTO experiment(id) VALUES (?)", id + 200);
    }
    var l = new liquibase.integration.spring.SpringLiquibase();
    l.setDataSource(source);
    l.setChangeLog("classpath:modeling-local-changelog.yaml");
    return l;
  }

  /** Valida os nomes e tipos canônicos da entidade sem geração automática de tabelas. */
  @Bean
  @DependsOn("liquibase")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
    var f = new LocalContainerEntityManagerFactoryBean();
    f.setDataSource(source);
    f.setManagedTypes(PersistenceManagedTypes.of(ProductModeling.class.getName()));
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

  /** Coordena JDBC e JPA na mesma transação real. */
  @Bean
  PlatformTransactionManager transactionManager(EntityManagerFactory f) {
    return new JpaTransactionManager(f);
  }

  /** Usa o repository produtivo de modelagens e seus locks. */
  @Bean
  ProductModelingRepository models(EntityManagerFactory f) {
    return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(f))
        .getRepository(ProductModelingRepository.class);
  }

  /** Simula apenas o cadastro externo, conservando lock SQL e identidade. */
  @Bean
  ProductRepository products(DataSource source) {
    var r = mock(ProductRepository.class);
    var jdbc = new JdbcTemplate(source);
    when(r.findById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              if (jdbc.queryForList("SELECT id FROM product WHERE id=?", id).isEmpty())
                return Optional.empty();
              var p = new Product();
              p.setId(id);
              p.setName("Estrela teste " + id);
              p.setInternalName(p.getName());
              p.setAutomaticExecutionEnabled(true);
              var b = new ProductBigIdea();
              b.setContentJson("{\"centralIdea\":\"Direção própria preservada\"}");
              p.setBigIdea(b);
              return Optional.of(p);
            });
    when(r.findLockedById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              jdbc.queryForList("SELECT id FROM product WHERE id=? FOR UPDATE", id);
              return r.findById(id);
            });
    return r;
  }

  /** Conserva a fila de descoberta em MySQL, simulando apenas seu mapeamento não alterado. */
  @Bean
  ProductDiscoveryCycleRepository research(DataSource source) {
    var r = mock(ProductDiscoveryCycleRepository.class);
    var jdbc = new JdbcTemplate(source);
    when(r.save(any()))
        .thenAnswer(
            i -> {
              ProductDiscoveryCycle c = i.getArgument(0);
              if (c.getId() == null) {
                jdbc.update(
                    "INSERT INTO product_discovery_cycle(status,theme,objective,reference_sources) VALUES (?,?,?,?)",
                    c.getStatus().name(),
                    c.getTheme(),
                    c.getObjective(),
                    c.getReferenceSources());
                c.setId(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
              } else
                jdbc.update(
                    "UPDATE product_discovery_cycle SET status=? WHERE id=?",
                    c.getStatus().name(),
                    c.getId());
              return c;
            });
    when(r.findById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              var rows = jdbc.queryForList("SELECT * FROM product_discovery_cycle WHERE id=?", id);
              if (rows.isEmpty()) return Optional.empty();
              var row = rows.getFirst();
              var c = new ProductDiscoveryCycle();
              c.setId(id);
              c.setTheme((String) row.get("theme"));
              c.setObjective((String) row.get("objective"));
              c.setReferenceSources((String) row.get("reference_sources"));
              c.setStatus(ProductDiscoveryCycleStatus.valueOf((String) row.get("status")));
              return Optional.of(c);
            });
    when(r.findByIdForUpdate(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              jdbc.queryForList("SELECT id FROM product_discovery_cycle WHERE id=? FOR UPDATE", id);
              return r.findById(id);
            });
    return r;
  }

  /** Exercita o serviço real de preparo/início com somente a auditoria externa simulada. */
  @Bean
  ProductDiscoveryService discovery(ProductDiscoveryCycleRepository r, DataSource source) {
    var audit = mock(ProductDiscoveryBpmAuditService.class);
    var jdbc = new JdbcTemplate(source);
    when(audit.open(any()))
        .thenAnswer(
            i -> {
              ProductDiscoveryCycle c = i.getArgument(0);
              jdbc.update("INSERT INTO fixture_audit(cycle_id) VALUES (?)", c.getId());
              return null;
            });
    return new ProductDiscoveryService(
        r,
        mock(ProductDiscoveryOpportunityRepository.class),
        mock(OpportunityDossierResearchSyncService.class),
        audit);
  }

  /** Simula dependência de descoberta fora da preparação testada. */
  @Bean
  com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService intelligence() {
    return mock(com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService.class);
  }

  /** Simula dependência de descoberta fora da preparação testada. */
  @Bean
  ProductDiscoveryCustomerInterviewService customerEvidence() {
    return mock(ProductDiscoveryCustomerInterviewService.class);
  }

  /** Simula dependência de descoberta fora da preparação testada. */
  @Bean
  ProductDiscoveryGapResearchContractService gapContract() {
    return mock(ProductDiscoveryGapResearchContractService.class);
  }

  /** Simula dependência de descoberta fora da preparação testada. */
  @Bean
  ProductDiscoveryMetaAdSessionLinkService metaSession() {
    return mock(ProductDiscoveryMetaAdSessionLinkService.class);
  }

  /** Simula dependência de descoberta fora da preparação testada. */
  @Bean
  ProductDiscoverySupervisedMetaSessionService supervisedSession() {
    return mock(ProductDiscoverySupervisedMetaSessionService.class);
  }

  /** Oferece duas preparações segregadas, sem tarefas comerciais reais. */
  @Bean
  LearningSalesCycleRepository cycles() {
    var r = mock(LearningSalesCycleRepository.class);
    for (long p : List.of(99101L, 99102L)) {
      var c = new LearningSalesCycle();
      c.setId(p + 100);
      c.setProductId(p);
      c.setExperimentId(p + 200);
      c.setChainDefinitionId(998L);
      c.setProductVersion("v-test");
      c.setStage("PLANNING");
      c.setStatus("OPEN");
      when(r.findById(p + 100)).thenReturn(Optional.of(c));
      when(r.findLocked(p, p + 100)).thenReturn(Optional.of(c));
      when(r.findByProductIdAndOpenSlot(p, 1)).thenReturn(List.of(c));
    }
    return r;
  }

  /** Mantém experimentos simulados em planejamento. */
  @Bean
  ExperimentRepository experiments() {
    var r = mock(ExperimentRepository.class);
    when(r.findById(anyLong()))
        .thenAnswer(
            i -> {
              var e = new Experiment();
              e.setStatus(ExperimentStatus.PLANNED);
              return Optional.of(e);
            });
    return r;
  }

  /** Nenhuma tarefa paga existe nesta topologia. */
  @Bean
  AgentTaskRepository tasks() {
    return mock(AgentTaskRepository.class);
  }

  /** Nenhuma execução externa existe nesta topologia. */
  @Bean
  ProcessRunRepository runs() {
    return mock(ProcessRunRepository.class);
  }

  /** Simula callback concluído de Argos com dossiê do ciclo consultado. */
  @Bean
  OpportunityDossierRepository dossiers(ProductDiscoveryCycleRepository research) {
    var r = mock(OpportunityDossierRepository.class);
    when(r.findAllByProductDiscoveryCycleIdOrderByIdAsc(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              if (research.findById(id).orElseThrow().getStatus()
                  != ProductDiscoveryCycleStatus.COMPLETED) return List.of();
              var d = new OpportunityDossier();
              d.setId(id);
              d.setTitle("Sinal público sintético");
              d.setKnownRisks("Não comprova vendas");
              var o = new ProductDiscoveryOpportunity();
              org.springframework.test.util.ReflectionTestUtils.setField(o, "id", id);
              o.setMaturity(ProductDiscoveryOpportunityMaturity.DOSSIER_READY);
              d.setProductDiscoveryOpportunity(o);
              return List.of(d);
            });
    return r;
  }

  /** Entrega fato sintético explicitamente distinto das referências reais do piloto. */
  @Bean
  OpportunityEvidenceRepository evidence() {
    var r = mock(OpportunityEvidenceRepository.class);
    when(r.findByDossierIdOrderByCreatedAtAsc(anyLong()))
        .thenAnswer(
            i -> {
              var e = new OpportunityEvidence();
              e.setId(i.getArgument(0));
              e.setSourceUrl("https://reference.example/");
              e.setSummary("Referência simulada sem valor comercial");
              return List.of(e);
            });
    return r;
  }

  /** Expõe somente recursos locais para a matriz de integração. */
  @RestController
  static class FixtureController {
    private final JdbcTemplate jdbc;
    private final ModelingHandoff handoff;

    /** Recebe dependências locais sem autenticação ou credencial produtiva. */
    FixtureController(DataSource source, ModelingHandoff handoff) {
      this.jdbc = new JdbcTemplate(source);
      this.handoff = handoff;
    }

    /** Informa contagens de auditoria para comprovar preparo sem execução. */
    @GetMapping("/fixture/health")
    public Map<String, Object> health() {
      return Map.of(
          "audit", jdbc.queryForObject("SELECT COUNT(*) FROM fixture_audit", Integer.class));
    }

    /** Simula resultado Argos sem chamar modelos, preservando a fila em banco. */
    @PostMapping("/fixture/complete/{id}")
    public void complete(@PathVariable Long id) {
      jdbc.update("UPDATE product_discovery_cycle SET status='COMPLETED' WHERE id=?", id);
    }

    /** Consulta o contexto real que será entregue às etapas da cadeia. */
    @GetMapping("/fixture/context/{id}")
    public Object context(@PathVariable Long id) {
      return handoff.snapshot("experiment:" + id, java.time.Instant.now().plusSeconds(1));
    }

    /** Oferece produtos sintéticos ao formulário real. */
    @GetMapping("/api/products")
    public Object products() {
      return List.of(
          Map.of("id", 99101, "name", "Vega sintética", "internalName", "Vega sintética"),
          Map.of("id", 99102, "name", "Outro produto", "internalName", "Outro produto"));
    }
  }
}
