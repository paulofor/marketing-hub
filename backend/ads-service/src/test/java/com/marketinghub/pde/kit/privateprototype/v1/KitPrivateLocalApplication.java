package com.marketinghub.pde.kit.privateprototype.v1;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.kit.privateprototype.v1.controller.KitPrivateController;
import com.marketinghub.pde.kit.privateprototype.v1.service.*;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.kit.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import jakarta.persistence.*;
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
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.util.ReflectionTestUtils;
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
   * Usa o resolvedor real sobre contratos sintéticos: formato novo e referência histórica de perfis
   * distintos, sem substituir a decisão de capacidade.
   */
  @Bean
  KitPrototypeCapabilities capabilities() {
    var tasks = mock(AgentTaskRepository.class);
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var plan = new BusinessProcessDefinition();
    plan.setId(8116L);
    plan.setProcessCode("pde-commercial-plan-offer");
    var construction = new BusinessProcessDefinition();
    construction.setId(8117L);
    construction.setProcessCode("pde-construction-approval");
    var first = new BusinessProcessChainItem();
    first.setProcessDefinition(plan);
    var second = new BusinessProcessChainItem();
    second.setProcessDefinition(construction);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(List.of(first, second));
    when(chains.findById(8026L)).thenReturn(Optional.of(chain));
    var architectures =
        Map.of(
            "experiment:9007",
                "{\"format\":\"Kit privado determinístico para nails-v1\",\"strategyReference\":\"Atena: fixture sintética\"}",
            "experiment:9018",
                "{\"format\":\"PRIVATE_HARNESS\",\"strategyReference\":\"Perfil barber-v1\"}",
            "experiment:9029",
                "{\"format\":\"PRIVATE_HARNESS\",\"strategyReference\":\"Perfil barber-v1\"}");
    when(tasks.findFunctionalSnapshotsByProcessSince(eq(8116L), anyString(), any(Instant.class)))
        .thenAnswer(
            a -> {
              String architecture = architectures.get(a.<String>getArgument(1));
              return architecture == null
                  ? List.of()
                  : List.of(
                      snapshot(
                          100L,
                          8116L,
                          "pde-commercial-plan-offer",
                          "productArchitecture",
                          "{\"decision\":\"APPROVE\",\"productArchitecture\":"
                              + architecture
                              + "}"));
            });
    when(tasks.findFunctionalSnapshotsByProcessSince(eq(8117L), anyString(), any(Instant.class)))
        .thenAnswer(
            a ->
                architectures.containsKey(a.<String>getArgument(1))
                    ? List.of(
                        snapshot(
                            101L,
                            8117L,
                            "pde-construction-approval",
                            "journey",
                            "{\"decision\":\"READY\"}"),
                        snapshot(
                            102L,
                            8117L,
                            "pde-construction-approval",
                            "deliverables",
                            "{\"decision\":\"READY\"}"),
                        snapshot(
                            103L,
                            8117L,
                            "pde-construction-approval",
                            "access",
                            "{\"decision\":\"READY\"}"))
                    : List.of());
    var c = new KitPrototypeCapabilities(tasks, chains, new ObjectMapper());
    ReflectionTestUtils.setField(
        c, "prototypeUrl", "http://127.0.0.1:57282/api/pde/kit/private/v1/prototype");
    return c;
  }

  /** Representa uma aprovação sintética auditável sem chamada paga ou alteração em dados reais. */
  private AgentTaskFunctionalSnapshot snapshot(
      long id, long processId, String processCode, String activity, String result) {
    return new AgentTaskFunctionalSnapshot(
        id,
        processId,
        processCode,
        activity,
        "landing-generator",
        "COMPLETED",
        Instant.EPOCH,
        Instant.EPOCH,
        result);
  }

  /** Cria os repositories sobre o mesmo contexto transacional compartilhado. */
  private <T> T repository(EntityManagerFactory f, Class<T> type) {
    return new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(f))
        .getRepository(type);
  }
}
