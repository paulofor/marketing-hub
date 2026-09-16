package com.marketinghub.catalogovivo.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.automation.v1.service.ProcessRunService;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.catalogovivo.v1.controller.CatalogoVivoController;
import com.marketinghub.experiment.*;
import com.marketinghub.opala.commercial.v1.service.OpalaCommercialContext;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jdbc.catalogovivo.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: executar API e catálogo reais em MySQL isolado com dependências comerciais
 * simuladas.
 */
@TestConfiguration
@Profile("catalogo-vivo-fixture")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({
  CatalogoVivoController.class,
  CatalogoVivoService.class,
  CatalogoVivoRepository.class,
  OpalaCycleAdoption.class,
  OpalaAdoptionRepository.class,
  CatalogoVivoLocalApplication.FixtureController.class
})
public class CatalogoVivoLocalApplication {
  /** Sobe apenas a fixture, sem propriedades, agendamentos ou credenciais produtivas. */
  public static void main(String[] args) {
    start();
  }

  /** Fixa banco e porta local independentemente de variáveis de produção herdadas. */
  static org.springframework.context.ConfigurableApplicationContext start() {
    String url = System.getenv("CATALOGO_VIVO_MYSQL_URL");
    if (url == null
        || !url.matches(
            "jdbc:mysql://(127\\.0\\.0\\.1|sandbox-docker):18307/catalogo_vivo_local.*"))
      throw new IllegalArgumentException("Informe somente o banco segregado catalogo_vivo_local.");
    return new SpringApplication(CatalogoVivoLocalApplication.class)
        .run(
            "--spring.config.location=optional:classpath:catalogo-vivo/no-production.properties",
            "--spring.profiles.active=catalogo-vivo-fixture",
            "--spring.datasource.url=" + url,
            "--spring.datasource.username=root",
            "--spring.datasource.password=cycles-root-local-only",
            "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
            "--spring.sql.init.mode=never",
            "--spring.liquibase.change-log=classpath:catalogo-vivo/changelog.yaml",
            "--spring.mvc.problemdetails.enabled=true",
            "--server.port=18091",
            "--server.address=127.0.0.1",
            "--logging.level.root=WARN",
            "--spring.main.banner-mode=off");
  }

  /** Resolve processos e atividades pela mesma projeção relacional da migração real. */
  @Bean
  BusinessProcessDefinitionRepository processes(JdbcTemplate jdbc) {
    var repository = mock(BusinessProcessDefinitionRepository.class);
    when(repository.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            anyString(), anyString()))
        .thenAnswer(
            i ->
                jdbc
                    .query(
                        "SELECT * FROM business_process_definition WHERE process_code=? AND status=? ORDER BY version_number DESC LIMIT 1",
                        (r, n) -> {
                          var p = new BusinessProcessDefinition();
                          p.setId(r.getLong("id"));
                          p.setProcessCode(r.getString("process_code"));
                          p.setVersionNumber(r.getInt("version_number"));
                          p.setStatus(r.getString("status"));
                          p.setDiagramJson(r.getString("diagram_json"));
                          p.setExecutionScope("PRODUCT");
                          p.setParentProcessCode(r.getString("parent_process_code"));
                          return p;
                        },
                        i.getArgument(0),
                        i.getArgument(1))
                    .stream()
                    .findFirst());
    return repository;
  }

  /** Entrega as atividades reais do SQL para o cálculo de cobertura, sem lista paralela. */
  @Bean
  BusinessProcessActivityDefinitionRepository activities(JdbcTemplate jdbc) {
    var repository = mock(BusinessProcessActivityDefinitionRepository.class);
    when(repository.findAllByProcessDefinitionIdOrderByIdAsc(anyLong()))
        .thenAnswer(
            i ->
                jdbc.query(
                    "SELECT * FROM business_process_activity_definition WHERE process_definition_id=? ORDER BY id",
                    (r, n) -> {
                      var a = new BusinessProcessActivityDefinition();
                      a.setId(r.getLong("id"));
                      a.setActivityId(r.getString("activity_id"));
                      a.setName(r.getString("name"));
                      a.setDefinitionJson(r.getString("definition_json"));
                      return a;
                    },
                    (Object) i.getArgument(0)));
    return repository;
  }

  /** Mantém o produto sintético e a permissão PLAY sem ler configurações comerciais reais. */
  @Bean
  ProductRepository products() {
    var repository = mock(ProductRepository.class);
    when(repository.findById(anyLong())).thenAnswer(i -> Optional.of(product(i.getArgument(0))));
    when(repository.findLockedById(anyLong()))
        .thenAnswer(i -> Optional.of(product(i.getArgument(0))));
    return repository;
  }

  /** Constrói identidades de produto com tipos separados para os testes de isolamento. */
  static Product product(long id) {
    var p = new Product();
    p.setId(id);
    p.setSlug("fixture-opala-" + id);
    p.setAutomaticExecutionEnabled(true);
    var type = new ProductTypeDefinition();
    type.setId(id == 900004L ? 900001L : 900002L);
    type.setCode(id == 900004L ? "PDE" : "OTHER");
    p.setProductTypeDefinition(type);
    return p;
  }

  /** Lê identidade e estado do experimento na tabela local, sem integração de mídia. */
  @Bean
  ExperimentRepository experiments(JdbcTemplate jdbc) {
    var repository = mock(ExperimentRepository.class);
    when(repository.findById(anyLong()))
        .thenAnswer(
            i ->
                jdbc
                    .query(
                        "SELECT * FROM experiment WHERE id=?",
                        (r, n) -> {
                          var e = new Experiment();
                          e.setId(r.getLong("id"));
                          e.setProduct(product(r.getLong("product_id")));
                          e.setStatus(ExperimentStatus.valueOf(r.getString("status")));
                          return e;
                        },
                        (Object) i.getArgument(0))
                    .stream()
                    .findFirst());
    return repository;
  }

  /** Preserva a passagem histórica no banco e adquire o mesmo lock utilizado em produção. */
  @Bean
  LearningSalesCycleRepository cycles(JdbcTemplate jdbc) {
    var repository = mock(LearningSalesCycleRepository.class);
    when(repository.findById(anyLong())).thenAnswer(i -> cycle(jdbc, i.getArgument(0), false));
    when(repository.findLocked(anyLong(), anyLong()))
        .thenAnswer(
            i ->
                cycle(jdbc, i.getArgument(1), true)
                    .filter(c -> c.getProductId().equals(i.getArgument(0))));
    return repository;
  }

  /** Converte a passagem sintética preservando orçamento, versão, estágio e revisão. */
  static Optional<LearningSalesCycle> cycle(JdbcTemplate jdbc, long id, boolean lock) {
    return jdbc
        .query(
            "SELECT * FROM learning_sales_cycle_v1 WHERE id=?" + (lock ? " FOR UPDATE" : ""),
            (r, n) -> {
              var c = new LearningSalesCycle();
              c.setId(r.getLong("id"));
              c.setProductId(r.getLong("product_id"));
              c.setExperimentId(r.getLong("experiment_id"));
              c.setChainDefinitionId(r.getLong("chain_definition_id"));
              c.setProductVersion(r.getString("product_version"));
              c.setRevision(r.getLong("revision"));
              c.setStatus(r.getString("status"));
              c.setStage(r.getString("stage"));
              c.setBudgetLimitBrl(r.getBigDecimal("budget_limit_brl"));
              c.setWindowEnd(r.getTimestamp("window_end").toInstant());
              return c;
            },
            id)
        .stream()
        .findFirst();
  }

  /** Simula apenas o contexto comercial externo; gates completos são cobertos pela suíte Opala. */
  @Bean
  OpalaCommercialContext context(JdbcTemplate jdbc, ExperimentRepository experiments) {
    var context = mock(OpalaCommercialContext.class);
    when(context.scope(anyString()))
        .thenAnswer(
            i -> {
              long experimentId = Long.parseLong(((String) i.getArgument(0)).substring(11));
              var c = cycle(jdbc, 900002L, false).orElseThrow();
              var e = experiments.findById(experimentId).orElseThrow();
              if (!"OPEN".equals(c.getStatus())
                  || !Set.of("AUTHORIZATION", "PUBLICATION").contains(c.getStage())
                  || e.getStatus() != ExperimentStatus.PLANNED)
                throw new IllegalStateException("Passagem indisponível para preparação.");
              return new OpalaCommercialContext.Scope(c, e);
            });
    return context;
  }

  /** Isola a partida do coordenador; o contrato e a progressão real têm suíte própria. */
  @Bean
  ProcessRunService runs() {
    return mock(ProcessRunService.class);
  }

  /** Responsabilidade: disponibilizar somente sondas segregadas para navegação local e inspeção. */
  @RestController
  static class FixtureController {
    /**
     * Retorna falha explícita a APIs não participantes, impedindo fallback para backend publicado.
     */
    @GetMapping("/fixture/health")
    Map<String, Object> health() {
      return Map.of("fixture", true, "externalEffects", false);
    }
  }
}
