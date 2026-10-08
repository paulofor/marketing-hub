package com.marketinghub.pde.visualpersonalization.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marketinghub.experiment.*;
import com.marketinghub.financialagent.StudioCostLedgerEntry;
import com.marketinghub.financialagent.service.StudioCostLedgerService;
import com.marketinghub.imagegenerator.ImageGenerationRequest;
import com.marketinghub.imagegenerator.service.ImageGenerationUsageCost;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.pde.visualpersonalization.v1.controller.VisualPreparationController;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import com.marketinghub.repository.jpa.financialagent.*;
import com.marketinghub.repository.jpa.imagegenerator.ImageGenerationRequestRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
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

/**
 * Responsabilidade: executar a API visual com MySQL 5.7 real e fontes sintéticas sem credenciais.
 */
@TestConfiguration
@Profile("visual-preparation-fixture")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({
  VisualPreparationService.class,
  VisualPreparationBudget.class,
  VisualPreparationExecution.class,
  VisualPreparationController.class,
  VisualPreparationLocalApplication.PricingFixtureController.class,
  ImageGenerationUsageCost.class
})
public class VisualPreparationLocalApplication {
  /** Inicia apenas localhost e banco da engine isolada, sem propriedades produtivas. */
  public static void main(String[] args) {
    new SpringApplication(VisualPreparationLocalApplication.class)
        .run(
            "--spring.config.location=optional:classpath:visual-personalization/no-production.properties",
            "--spring.profiles.active=visual-preparation-fixture",
            "--spring.datasource.url=jdbc:mysql://sandbox-docker:18515/financial_plans_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            "--spring.datasource.username=root",
            "--spring.datasource.password=financial-plans-local-only",
            "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
            "--spring.jpa.open-in-view=false",
            "--spring.liquibase.enabled=false",
            "--spring.sql.init.mode=never",
            "--server.address=127.0.0.1",
            "--server.port=18095",
            "--spring.mvc.problemdetails.enabled=true",
            "--integrations.pde-platform.internal-token=visual-local-only",
            "--logging.level.root=WARN");
  }

  /**
   * Aplica os changelogs originais sobre referências locais, sem schema alternativo da auditoria.
   */
  @Bean
  liquibase.integration.spring.SpringLiquibase liquibase(DataSource source) {
    var jdbc = new JdbcTemplate(source);
    for (String table : List.of("product", "commercial_plan", "experiment")) {
      jdbc.execute(
          "CREATE TABLE IF NOT EXISTS " + table + " (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
      for (long id : List.of(95501L, 95502L))
        jdbc.update("INSERT IGNORE INTO " + table + " VALUES (?)", id);
    }
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS video_project (id BIGINT PRIMARY KEY, updated_at DATETIME(6)) ENGINE=InnoDB");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS video_production_cycle (id BIGINT PRIMARY KEY, known_cost_usd DECIMAL(14,6)) ENGINE=InnoDB");
    var lb = new liquibase.integration.spring.SpringLiquibase();
    lb.setDataSource(source);
    lb.setChangeLog("classpath:db/changelog/visual-personalization-local.yaml");
    return lb;
  }

  /** Confere as entidades originais contra o schema Liquibase, sem criação pelo Hibernate. */
  @Bean
  @DependsOn("liquibase")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
    factory.setManagedTypes(
        PersistenceManagedTypes.of(
            ImageGenerationRequest.class.getName(), StudioCostLedgerEntry.class.getName()));
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

  /** Mantém locks de referência e gravações JPA dentro da mesma transação real. */
  @Bean
  PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
    return new JpaTransactionManager(factory);
  }

  /** Usa o repository original da auditoria, incluindo queries e locks pessimistas. */
  @Bean
  ImageGenerationRequestRepository images(EntityManagerFactory factory) {
    return repository(factory, ImageGenerationRequestRepository.class);
  }

  /** Usa o ledger original sem mock de custo ou duplicação de entradas. */
  @Bean
  StudioCostLedgerEntryRepository costs(EntityManagerFactory factory) {
    return repository(factory, StudioCostLedgerEntryRepository.class);
  }

  /** Reutiliza o serviço original do ledger somente com sua dependência de imagens. */
  @Bean
  StudioCostLedgerService ledger(StudioCostLedgerEntryRepository costs) {
    return new StudioCostLedgerService(costs);
  }

  /** Constrói proxies reais para as entidades delimitadas à fixture. */
  private <T> T repository(EntityManagerFactory factory, Class<T> type) {
    return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory))
        .getRepository(type);
  }

  /** Fornece referências sintéticas, mantendo o lock de produto no MySQL real. */
  @Bean
  ProductRepository products(DataSource source) {
    var repo = mock(ProductRepository.class);
    var jdbc = new JdbcTemplate(source);
    when(repo.findById(anyLong())).thenAnswer(i -> Optional.ofNullable(product(i.getArgument(0))));
    when(repo.findLockedById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              jdbc.queryForObject("SELECT id FROM product WHERE id=? FOR UPDATE", Long.class, id);
              return repo.findById(id);
            });
    return repo;
  }

  /** Declara dois produtos distintos com contrato privado de referência, sem dados comerciais. */
  private Product product(long id) {
    if (id != 95501 && id != 95502) return null;
    var p = new Product();
    p.setId(id);
    p.setName("Produto local " + id);
    p.setAutomaticExecutionEnabled(true);
    p.setPdeExperienceJson(
        "{\"privatePrototypeAcceptance\":{\"prototypeVersion\":\"private-local-v1\",\"privateAccessUrl\":\"http://127.0.0.1:15184\"},\"agentValidation\":{\"status\":\"PASS\"}}");
    return p;
  }

  /** Disponibiliza o experimento PLANNED exclusivamente do próprio produto local. */
  @Bean
  ExperimentRepository experiments() {
    var repo = mock(ExperimentRepository.class);
    when(repo.findById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              var p = product(id);
              if (p == null) return Optional.empty();
              var e = new Experiment();
              e.setId(id);
              e.setProduct(p);
              e.setStatus(ExperimentStatus.PLANNED);
              return Optional.of(e);
            });
    return repo;
  }

  /** Declara autorização fictícia explícita, sem importar ou alterar limites do usuário real. */
  @Bean
  CommercialPlanRepository plans(ExperimentRepository experiments) {
    var repo = mock(CommercialPlanRepository.class);
    when(repo.findById(anyLong()))
        .thenAnswer(
            i -> {
              long id = i.getArgument(0);
              var e = experiments.findById(id);
              if (e.isEmpty()) return Optional.empty();
              var p = new CommercialPlan();
              p.setId(id);
              p.setExperiment(e.get());
              p.setNextAction(
                  "Autorização explícita do usuário em 05/10/2026: teto TOTAL de USD 10 para homologar geração personalizada real (produto "
                      + id
                      + ", experimento "
                      + id
                      + ", execução 950). sem mídia, campanha, cobrança real.");
              return Optional.of(p);
            });
    return repo;
  }

  /** Mantém pareceres de Plutus vazios, sem produzir aprovação em nome do especialista. */
  @Bean
  FinancialAgentExecutionRepository financial() {
    return mock(FinancialAgentExecutionRepository.class);
  }

  /** Mantém pareceres de Atena vazios, sem produzir aprovação em nome do especialista. */
  @Bean
  ExperimentStrategistExecutionRepository strategist() {
    return mock(ExperimentStrategistExecutionRepository.class);
  }

  /** Substitui somente a tarifa de texto; tokens e imagem usam o estimador real. */
  @Bean
  OpenAiPricingService pricing() {
    var service = mock(OpenAiPricingService.class);
    when(service.estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong()))
        .thenReturn(Optional.of(new BigDecimal("0.01")));
    return service;
  }

  /**
   * Responsabilidade: simular indisponibilidade e recuperação da tarifa somente na fixture local.
   */
  @org.springframework.web.bind.annotation.RestController
  public static class PricingFixtureController {
    private final OpenAiPricingService pricing;

    /** Recebe somente o mock de preço utilizado pelo app local. */
    public PricingFixtureController(OpenAiPricingService pricing) {
      this.pricing = pricing;
    }

    /** Muda a fonte simulada sem tocar contrato, resposta, status ou custo diretamente. */
    @org.springframework.web.bind.annotation.PostMapping("/fixture/pricing/{state}")
    public void change(
        @org.springframework.web.bind.annotation.PathVariable("state") String state) {
      if (!List.of("missing", "available").contains(state))
        throw new IllegalArgumentException("Estado de teste inválido");
      doReturn("available".equals(state) ? Optional.of(new BigDecimal("0.01")) : Optional.empty())
          .when(pricing)
          .estimateTaskCost(anyString(), anyString(), anyLong(), anyLong(), anyLong());
    }
  }
}
