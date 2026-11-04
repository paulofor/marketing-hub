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

/** Responsabilidade: homologar o fluxo real com banco local e contratos de agentes simulados. */
@TestConfiguration
@Profile("private-kit-local")
@EnableAutoConfiguration(
    exclude = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableTransactionManagement
@Import({
  KitPrivateService.class,
  KitArtifactContract.class,
  KitPrivateArtifactStorage.class,
  KitPrivateController.class,
  KitPrivateStorageLocalFixture.class,
  KitPrivateVideoLocalFixture.class
})
public class KitPrivateLocalApplication {
  /**
   * Inicializa somente portas locais e credenciais fictícias, sem carregar configuração produtiva.
   */
  public static void main(String[] ignored) {
    String host = System.getenv().getOrDefault("KIT_TEST_DB_HOST", "127.0.0.1");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host))
      throw new IllegalArgumentException("Banco externo proibido.");
    boolean memory = "h2".equals(System.getenv("KIT_TEST_DATABASE"));
    new SpringApplication(KitPrivateLocalApplication.class)
        .run(
            "--spring.config.location=optional:classpath:private-kit-local/no-production.properties",
            "--spring.profiles.active=private-kit-local",
            "--server.port=57282",
            "--server.address=127.0.0.1",
            "--spring.datasource.url="
                + (memory
                    ? "jdbc:h2:mem:kit_local;MODE=MySQL;DB_CLOSE_DELAY=-1"
                    : "jdbc:mysql://"
                        + host
                        + ":57283/kit_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"),
            "--spring.liquibase.enabled=" + !memory,
            "--kit.local.mysql-proof=" + !memory,
            "--spring.datasource.username=kit",
            "--spring.datasource.password=kit-local-only",
            "--spring.liquibase.change-log=classpath:private-kit-local/changelog.yaml",
            "--integrations.pde-platform.internal-token=kit-local-internal-only",
            "--integrations.lead-portal-payments.auth-token=kit-local-payments-only",
            "--spring.mvc.problemdetails.enabled=true",
            "--spring.servlet.multipart.max-file-size=32MB",
            "--spring.servlet.multipart.max-request-size=33MB",
            "--spring.jpa.open-in-view=false",
            "--logging.level.root=WARN",
            "--logging.level.com.marketinghub.pde.kit=INFO");
  }

  /** Mantém MySQL validado por Liquibase; H2 explícito serve somente ao fluxo funcional local. */
  @Bean
  LocalContainerEntityManagerFactoryBean entityManagerFactory(
      DataSource source,
      org.springframework.beans.factory.ObjectProvider<liquibase.integration.spring.SpringLiquibase>
          migrations) {
    migrations.getIfAvailable();
    boolean memory = "h2".equals(System.getenv("KIT_TEST_DATABASE"));
    var f = new LocalContainerEntityManagerFactoryBean();
    f.setDataSource(source);
    f.setManagedTypes(
        PersistenceManagedTypes.of(
            KitPrivateSession.class.getName(),
            KitPrivateArtifact.class.getName(),
            KitPrivateArtifactChunk.class.getName(),
            LearningSalesCycle.class.getName()));
    f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    f.setJpaPropertyMap(
        Map.of(
            "hibernate.hbm2ddl.auto",
            memory ? "create-drop" : "validate",
            "hibernate.dialect",
            memory
                ? "org.hibernate.dialect.H2Dialect"
                : "org.hibernate.community.dialect.MySQL57Dialect",
            "hibernate.jdbc.time_zone",
            "UTC"));
    return f;
  }

  /** Semeia apenas identidades sintéticas no H2; não afirma validação de MySQL ou de Liquibase. */
  @Bean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
      name = "kit.local.mysql-proof",
      havingValue = "false")
  ApplicationRunner seedMemoryDatabase(DataSource source, EntityManagerFactory factory) {
    return args -> {
      var jdbc = new org.springframework.jdbc.core.JdbcTemplate(source);
      for (long id : List.of(7007L, 7018L, 7029L, 7037L, 7048L)) {
        String version =
            id == 7018 ? "barber-candidate-v1" : id >= 7037 ? "beauty-candidate-v1" : "v1";
        jdbc.update(
            """
            INSERT INTO learning_sales_cycle_v1
            (id,product_id,chain_definition_id,chain_code,process_definition_id,experiment_id,
            request_key,creation_json,brief_json,inherited_learning_json,stage,status,product_version,
            budget_limit_brl,open_slot,revision,baseline,created_at,updated_at,version_changed_at)
            VALUES (?, ?, 8026, 'private-kit-local', 8125, ?, ?, '{}', '{}', '{}', 'ADJUSTMENT',
            'OPEN', ?, 0, 1, 0, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            id,
            id + 1000,
            id + 2000,
            UUID.randomUUID().toString(),
            version);
      }
      System.out.println("PRIVATE_KIT_FUNCTIONAL_H2_READY mysqlProof=false syntheticOnly=true");
    };
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

  /** Usa partes reais para conferir limites de pacote e integridade no MySQL. */
  @Bean
  KitPrivateArtifactChunkRepository chunks(EntityManagerFactory factory) {
    return repository(factory, KitPrivateArtifactChunkRepository.class);
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
    when(r.saveAndFlush(any(LearningSalesCycle.class)))
        .thenAnswer(
            a -> {
              var saved = em.merge(a.<LearningSalesCycle>getArgument(0));
              em.flush();
              return saved;
            });
    return r;
  }

  /**
   * Usa o resolvedor real sobre contratos sintéticos: formato novo e referência histórica de perfis
   * distintos, incluindo referências v2/v3 quando solicitadas, sem substituir a decisão de
   * capacidade.
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
                "{\"format\":\"Kit privado determinístico para"
                    + " nails-v1\",\"strategyReference\":\"Atena: fixture sintética\"}",
            "experiment:9018",
                "{\"format\":\"PRIVATE_HARNESS\",\"strategyReference\":\"Perfil barber-v1\"}",
            "experiment:9029",
                "{\"format\":\"PRIVATE_HARNESS\",\"strategyReference\":\"Perfil barber-v1\"}",
            "experiment:9037", "{\"format\":\"PRIVATE_HARNESS\",\"kitProfileCode\":\"beauty-v1\"}",
            "experiment:9048", "{\"format\":\"PRIVATE_HARNESS\",\"kitProfileCode\":\"beauty-v1\"}");
    when(tasks.findFunctionalSnapshotsByProcessSince(eq(8116L), anyString(), any(Instant.class)))
        .thenAnswer(
            a -> {
              String architecture = architectures.get(a.<String>getArgument(1));
              if (architecture != null && "true".equals(System.getenv("KIT_TEST_REFERENCE")))
                architecture =
                    architecture.replace(
                        "beauty-v1",
                        "beauty-v"
                            + System.getenv().getOrDefault("KIT_TEST_REFERENCE_VERSION", "2"));
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

  /** Usa o comando real de vinculação com recibos locais e desenhos sintéticos segregados. */
  @Bean
  KitImplementationBinding implementations(LearningSalesCycleRepository cycles) {
    var events =
        mock(com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository.class);
    var history =
        new ArrayList<
            com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent>();
    when(events.findByCycleIdAndActionOrderByRevisionDesc(anyLong(), anyString()))
        .thenAnswer(
            a ->
                history.stream()
                    .filter(
                        e ->
                            e.getCycleId().equals(a.<Long>getArgument(0))
                                && e.getAction().equals(a.<String>getArgument(1)))
                    .sorted(
                        Comparator.comparing(
                                com.marketinghub.businessprocesschain.learningcycle.v1
                                        .LearningSalesCycleEvent
                                    ::getRevision)
                            .reversed())
                    .toList());
    when(events.findByCycleIdAndRequestKey(anyLong(), anyString()))
        .thenAnswer(
            a ->
                history.stream()
                    .filter(
                        e ->
                            e.getCycleId().equals(a.<Long>getArgument(0))
                                && e.getRequestKey().equals(a.<String>getArgument(1)))
                    .findFirst());
    when(events.saveAndFlush(any()))
        .thenAnswer(
            a -> {
              var event =
                  a
                      .<com.marketinghub.businessprocesschain.learningcycle.v1
                              .LearningSalesCycleEvent>
                          getArgument(0);
              history.add(event);
              return event;
            });
    var designs = mock(AgentTaskRepository.class);
    when(designs.findFunctionalSnapshots(anyString(), anySet(), any()))
        .thenAnswer(
            a ->
                List.of(
                    snapshot(
                        6037,
                        8116,
                        "pde-commercial-plan-offer",
                        "productArchitecture",
                        "{\"decision\":\"APPROVE\",\"productArchitecture\":{\"staticResultFixtures\":{\"contractVersion\":\"PDE_STATIC_RESULT_FIXTURES_V1\",\"mode\":\"DETERMINISTIC_HOMOLOGATION_ONLY\",\"providerCallsAuthorized\":0,\"externalSideEffects\":false,\"commercialEvidenceEligible\":false}}}")));
    return new KitImplementationBinding(cycles, events, designs, new ObjectMapper());
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
