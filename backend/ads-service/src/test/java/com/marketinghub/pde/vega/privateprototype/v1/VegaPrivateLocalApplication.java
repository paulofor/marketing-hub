package com.marketinghub.pde.vega.privateprototype.v1;

import static org.mockito.Mockito.*;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.vega.privateprototype.v1.controller.VegaPrivateController;
import com.marketinghub.pde.vega.privateprototype.v1.service.VegaPrivateService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.vega.*;
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
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Responsabilidade: executar o protótipo real localmente com cadastros e provedor externos
 * simulados.
 */
@TestConfiguration
@Profile("vega380-local")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({VegaPrivateService.class, VegaPrivateController.class})
public class VegaPrivateLocalApplication {
  /** Inicia apenas a fixture com banco MySQL local e credencial sintética. */
  public static void main(String[] ignored) {
    String host = System.getenv().getOrDefault("VEGA_TEST_DB_HOST", "127.0.0.1");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host))
      throw new IllegalArgumentException("Banco externo proibido.");
    new SpringApplication(VegaPrivateLocalApplication.class)
        .run(
            "--spring.config.location=optional:classpath:vega380/no-production.properties",
            "--spring.profiles.active=vega380-local",
            "--server.port=18080",
            "--server.address=0.0.0.0",
            "--spring.datasource.url=jdbc:mysql://"
                + host
                + ":18316/vega_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            "--spring.datasource.username=vega",
            "--spring.datasource.password=vega-local-only",
            "--spring.liquibase.change-log=classpath:vega380/changelog.yaml",
            "--integrations.pde-platform.internal-token=vega-local-internal-only",
            "--spring.mvc.problemdetails.enabled=true",
            "--spring.jpa.open-in-view=false",
            "--logging.level.root=WARN",
            "--logging.level.com.marketinghub.pde.vega=INFO");
  }

  /** Mantém sessões, tentativas e ciclo em MySQL real com validação do mapeamento canônico. */
  @Bean
  @DependsOn("liquibase")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
    factory.setManagedTypes(
        PersistenceManagedTypes.of(
            VegaPrivateSession.class.getName(),
            VegaAdjustmentExecution.class.getName(),
            LearningSalesCycle.class.getName(),
            com.marketinghub.businessprocesschain.learningcycle.v1.decision
                .LearningCycleDecisionProposal.class
                .getName()));
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setJpaPropertyMap(
        Map.of(
            "hibernate.hbm2ddl.auto",
            "none",
            "hibernate.dialect",
            "org.hibernate.community.dialect.MySQL57Dialect",
            "hibernate.jdbc.time_zone",
            "UTC"));
    return factory;
  }

  /** Garante transações reais nas ações concorrentes da experiência. */
  @Bean
  PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
    return new JpaTransactionManager(factory);
  }

  /** Publica o repository real das sessões. */
  @Bean
  VegaPrivateSessionRepository sessions(EntityManagerFactory factory) {
    return repository(factory, VegaPrivateSessionRepository.class);
  }

  /** Publica o repository real de fila e callbacks. */
  @Bean
  VegaAdjustmentExecutionRepository executions(EntityManagerFactory factory) {
    return repository(factory, VegaAdjustmentExecutionRepository.class);
  }

  /** Publica o repository real da identidade do ciclo. */
  @Bean
  LearningSalesCycleRepository cycles(EntityManagerFactory factory) {
    return repository(factory, LearningSalesCycleRepository.class);
  }

  /** Simula somente o cadastro histórico do produto, mantendo os IDs de teste segregados. */
  @Bean
  ProductRepository products() {
    var repository = mock(ProductRepository.class);
    when(repository.findById(91004L))
        .thenReturn(
            Optional.of(
                Product.builder()
                    .id(91004L)
                    .slug("metodo-musa-7-dias")
                    .internalName("Vega fixture")
                    .pdeExperienceJson("{\"experienceVersion\":\"historical-v7\"}")
                    .build()));
    return repository;
  }

  /** Usa os repositories oficiais com o gerenciador transacional da fixture. */
  private <T> T repository(EntityManagerFactory factory, Class<T> type) {
    return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory))
        .getRepository(type);
  }
}
