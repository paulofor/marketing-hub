package com.marketinghub.pde.kit.privateprototype.v1;

import static org.mockito.Mockito.*;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.kit.privateprototype.v1.controller.KitPrivateController;
import com.marketinghub.pde.kit.privateprototype.v1.service.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.Capability;
import com.marketinghub.repository.jpa.kit.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import jakarta.persistence.*;
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
 * Responsabilidade: homologar a implementação real com MySQL 5.7 e contratos de agentes simulados.
 */
@TestConfiguration
@Profile("private-kit-local")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({KitPrivateService.class, KitArtifactContract.class, KitPrivateController.class})
public class KitPrivateLocalApplication {
  /**
   * Inicializa somente portas locais e credenciais fictícias, sem carregar configuração produtiva.
   */
  public static void main(String[] ignored) {
    String host = System.getenv().getOrDefault("KIT_TEST_DB_HOST", "127.0.0.1");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host))
      throw new IllegalArgumentException("Banco externo proibido.");
    new SpringApplication(KitPrivateLocalApplication.class)
        .run(
            "--spring.config.location=optional:classpath:private-kit-local/no-production.properties",
            "--spring.profiles.active=private-kit-local",
            "--server.port=57282",
            "--server.address=127.0.0.1",
            "--spring.datasource.url=jdbc:mysql://"
                + host
                + ":57283/kit_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            "--spring.datasource.username=kit",
            "--spring.datasource.password=kit-local-only",
            "--spring.liquibase.change-log=classpath:private-kit-local/changelog.yaml",
            "--integrations.pde-platform.internal-token=kit-local-internal-only",
            "--integrations.lead-portal-payments.auth-token=kit-local-payments-only",
            "--spring.mvc.problemdetails.enabled=true",
            "--spring.jpa.open-in-view=false",
            "--logging.level.root=WARN",
            "--logging.level.com.marketinghub.pde.kit=INFO");
  }

  /** Confere no MySQL as entidades e o changelog produtivo, sem criação automática de schema. */
  @Bean
  @DependsOn("liquibase")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
    var f = new LocalContainerEntityManagerFactoryBean();
    f.setDataSource(source);
    f.setManagedTypes(
        PersistenceManagedTypes.of(
            KitPrivateSession.class.getName(),
            KitPrivateArtifact.class.getName(),
            LearningSalesCycle.class.getName()));
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

  /** Garante que claims, limites e entradas concorrentes utilizem transações reais. */
  @Bean
  PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
    return new JpaTransactionManager(factory);
  }

  /** Usa o repository canônico das sessões privadas. */
  @Bean
  KitPrivateSessionRepository sessions(EntityManagerFactory factory) {
    return repository(factory, KitPrivateSessionRepository.class);
  }

  /** Usa o repository canônico dos artefatos e sua fila no banco. */
  @Bean
  KitPrivateArtifactRepository artifacts(EntityManagerFactory factory) {
    return repository(factory, KitPrivateArtifactRepository.class);
  }

  /** Simula somente a interface do cadastro de ciclos, preservando seus locks e dados reais. */
  @Bean
  LearningSalesCycleRepository cycles(EntityManagerFactory factory) {
    var em = SharedEntityManagerCreator.createSharedEntityManager(factory);
    var r = mock(LearningSalesCycleRepository.class);
    when(r.findLockedById(anyLong()))
        .thenAnswer(
            a ->
                Optional.ofNullable(
                    em.find(
                        LearningSalesCycle.class,
                        a.getArgument(0),
                        LockModeType.PESSIMISTIC_WRITE)));
    when(r.findById(anyLong()))
        .thenAnswer(a -> Optional.ofNullable(em.find(LearningSalesCycle.class, a.getArgument(0))));
    when(r.findLocked(anyLong(), anyLong()))
        .thenAnswer(
            a -> {
              LearningSalesCycle c =
                  em.find(
                      LearningSalesCycle.class, a.getArgument(1), LockModeType.PESSIMISTIC_WRITE);
              return c != null && c.getProductId().equals(a.getArgument(0))
                  ? Optional.of(c)
                  : Optional.empty();
            });
    return r;
  }

  /**
   * Substitui a consulta aos agentes por aprovações sintéticas de dois perfis explicitamente
   * distintos.
   */
  @Bean
  KitPrototypeCapabilities capabilities() {
    var c = mock(KitPrototypeCapabilities.class);
    when(c.resolve(any()))
        .thenAnswer(
            a -> {
              LearningSalesCycle cycle = a.getArgument(0);
              return new Capability(
                  true,
                  cycle.getProductId() == 8007L ? "nails-v1" : "barber-v1",
                  "Contratos de QA simulados",
                  "http://127.0.0.1:57282/api/pde/kit/private/v1/prototype");
            });
    return c;
  }

  /** Cria os repositories sobre o mesmo contexto transacional compartilhado. */
  private <T> T repository(EntityManagerFactory f, Class<T> type) {
    return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(f))
        .getRepository(type);
  }
}
