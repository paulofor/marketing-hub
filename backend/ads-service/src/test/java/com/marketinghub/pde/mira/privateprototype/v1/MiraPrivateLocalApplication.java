package com.marketinghub.pde.mira.privateprototype.v1;

import static org.mockito.Mockito.*;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.mira.privateprototype.v1.controller.MiraPrivateController;
import com.marketinghub.pde.mira.privateprototype.v1.service.MiraPrivateService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.mira.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
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
@Profile("mira-candidate-local")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({MiraPrivateService.class, MiraPrivateController.class})
public class MiraPrivateLocalApplication {
  /** Inicia a experiência real com banco MySQL isolado e cadastro sintético. */
  public static void main(String[] ignored) {
    String host = System.getenv().getOrDefault("MIRA_TEST_DB_HOST", "127.0.0.1");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host))
      throw new IllegalArgumentException("Banco externo proibido.");
    new SpringApplication(MiraPrivateLocalApplication.class)
        .run(
            "--spring.config.location=optional:classpath:mira-candidate/no-production.properties",
            "--spring.profiles.active=mira-candidate-local",
            "--server.port=57182",
            "--server.address=127.0.0.1",
            "--spring.datasource.url=jdbc:mysql://"
                + host
                + ":57183/mira_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            "--spring.datasource.username=mira",
            "--spring.datasource.password=mira-local-only",
            "--spring.liquibase.change-log=classpath:mira-candidate-local/changelog.yaml",
            "--integrations.pde-platform.internal-token=mira-local-internal-only",
            "--spring.mvc.problemdetails.enabled=true",
            "--spring.jpa.open-in-view=false",
            "--logging.level.root=WARN",
            "--logging.level.com.marketinghub.pde.mira=INFO");
  }

  /** Mantém sessões, tentativas e ciclo em MySQL real com validação do mapeamento canônico. */
  @Bean
  @DependsOn("liquibase")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
    factory.setManagedTypes(PersistenceManagedTypes.of(MiraPrivateSession.class.getName()));
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

  /** Garante transações reais nas ações concorrentes da experiência. */
  @Bean
  PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
    return new JpaTransactionManager(factory);
  }

  /** Publica o repository real das sessões. */
  @Bean
  MiraPrivateSessionRepository sessions(EntityManagerFactory factory) {
    return repository(factory, MiraPrivateSessionRepository.class);
  }

  /** Simula o cadastro canônico de dois ciclos sem alcançar serviços externos. */
  @Bean
  LearningSalesCycleRepository cycles() {
    var repository = mock(LearningSalesCycleRepository.class);
    when(repository.findLockedById(anyLong()))
        .thenAnswer(
            a -> {
              long id = a.getArgument(0);
              if (!Set.of(7006L, 7017L).contains(id)) return Optional.empty();
              var cycle = new LearningSalesCycle();
              cycle.setId(id);
              cycle.setProductId(id + 1000);
              cycle.setExperimentId(id + 2000);
              cycle.setStage("ADJUSTMENT");
              cycle.setStatus("OPEN");
              cycle.setProductVersion("mira-commercial-v1");
              return Optional.of(cycle);
            });
    return repository;
  }

  /** Simula somente o cadastro histórico do produto, mantendo os IDs de teste segregados. */
  @Bean
  ProductRepository products() {
    var repository = mock(ProductRepository.class);
    when(repository.findById(anyLong()))
        .thenAnswer(
            a ->
                Optional.of(
                    Product.builder()
                        .id(a.getArgument(0))
                        .slug("pde-planejado-36")
                        .internalName("Mira fixture")
                        .build()));
    return repository;
  }

  /** Usa os repositories oficiais com o gerenciador transacional da fixture. */
  private <T> T repository(EntityManagerFactory factory, Class<T> type) {
    return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory))
        .getRepository(type);
  }
}
