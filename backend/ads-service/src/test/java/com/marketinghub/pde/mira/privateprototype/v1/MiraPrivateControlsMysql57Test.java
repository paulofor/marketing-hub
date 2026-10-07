package com.marketinghub.pde.mira.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.pde.mira.privateprototype.v1.service.MiraPrivateService;
import com.marketinghub.pde.mira.privateprototype.v1.service.contract.MiraPrivateContract.*;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.mira.MiraPrivateSessionRepository;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Responsabilidade: comprovar os controles privados por HTTP e transações MySQL 5.7 isoladas.
 * Cadastros de ciclo são doubles locais; sessões, validação HTTP e transações são reais.
 */
@SpringBootTest(
    classes = MiraPrivateControlsMysql57Test.ControlsConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.config.location=optional:classpath:mira-candidate/no-production.properties",
      "spring.liquibase.change-log=classpath:mira-candidate-local/changelog.yaml",
      "integrations.pde-platform.internal-token=mira-local-internal-only",
      "spring.jpa.open-in-view=false",
      "logging.level.root=WARN"
    })
@ActiveProfiles("mira-candidate-local")
@EnabledIfEnvironmentVariable(
    named = "MIRA_CONTROLS_DB_HOST",
    matches = "127\\.0\\.0\\.1|sandbox-docker")
class MiraPrivateControlsMysql57Test {
  /** Isola a fixture no perfil privado para não contaminar o contexto dos outros testes. */
  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  @org.springframework.context.annotation.Profile("mira-candidate-local")
  @org.springframework.context.annotation.Import(MiraPrivateLocalApplication.class)
  static class ControlsConfiguration {}

  private static final String ROOT = "/api/pde/mira/candidate/v1";
  @Autowired private TestRestTemplate http;
  @Autowired private MiraPrivateSessionRepository sessions;
  @Autowired private LearningSalesCycleRepository cycles;
  @Autowired private org.springframework.transaction.PlatformTransactionManager transactions;

  /** Limita o banco à engine efêmera, impedindo execução contra banco publicado. */
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    String host = System.getenv("MIRA_CONTROLS_DB_HOST");
    if (!Set.of("127.0.0.1", "sandbox-docker").contains(host))
      throw new IllegalArgumentException("Banco externo proibido.");
    String port = System.getenv().getOrDefault("MIRA_CONTROLS_DB_PORT", "57183");
    String schema = System.getenv().getOrDefault("MIRA_CONTROLS_DB_SCHEMA", "mira_local");
    if (!Set.of("57183", "3306").contains(port)
        || !Set.of("mira_local", "mira_controls_local").contains(schema))
      throw new IllegalArgumentException("Destino de teste inválido.");
    properties.add(
        "spring.datasource.url",
        () ->
            "jdbc:mysql://"
                + host
                + ":"
                + port
                + "/"
                + schema
                + "?useSSL=false&serverTimezone=UTC");
    properties.add(
        "spring.datasource.username",
        () -> System.getenv().getOrDefault("MIRA_CONTROLS_DB_USER", "mira"));
    properties.add(
        "spring.datasource.password",
        () -> System.getenv().getOrDefault("MIRA_CONTROLS_DB_PASSWORD", "mira-local-only"));
  }

  /** Recompõe duas identidades sintéticas abertas antes de cada cenário. */
  @BeforeEach
  void context() {
    when(cycles.findLockedById(anyLong()))
        .thenAnswer(
            call -> Optional.of(cycle(call.getArgument(0), "OPEN", MiraPrivateService.VERSION)));
  }

  /** Confirma consumo único sob requisições simultâneas e consulta idempotente. */
  @Test
  void concurrentGenerationConsumesOnce() throws Exception {
    var access = create(7006L);
    assertThat(input(access.sessionToken(), 1).getStatusCode()).isEqualTo(HttpStatus.OK);
    var pool = Executors.newFixedThreadPool(8);
    var start = new CountDownLatch(1);
    try {
      List<Future<ResponseEntity<SessionView>>> calls = new ArrayList<>();
      for (int i = 0; i < 8; i++)
        calls.add(
            pool.submit(
                () -> {
                  start.await();
                  return generate(access.sessionToken());
                }));
      start.countDown();
      for (var call : calls) {
        var result = call.get(30, TimeUnit.SECONDS);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().organizationsUsed()).isEqualTo(1);
      }
      assertThat(session(access.sessionToken()).getBody().organizationsUsed()).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  /** Preserva duas organizações e recusa nova entrada sem apagar os resultados. */
  @Test
  void twoOrganizationsRejectThird() {
    var access = create(7017L);
    input(access.sessionToken(), 1);
    assertThat(generate(access.sessionToken()).getBody().organizationsUsed()).isEqualTo(1);
    var second =
        exchange(
            access.sessionToken(),
            "/input",
            HttpMethod.PUT,
            new Input(
                "Organizar outros cuidados",
                List.of(new ProductInput("Outro produto", "Limpar e enxaguar."))),
            SessionView.class);
    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    var ready = generate(access.sessionToken()).getBody();
    assertThat(ready.organizationsUsed()).isEqualTo(2);
    assertThat(ready.previousResults()).hasSize(1);
    var third =
        exchange(
            access.sessionToken(),
            "/input",
            HttpMethod.PUT,
            new Input(
                "Terceira organização",
                List.of(new ProductInput("Terceiro produto", "Limpar e enxaguar."))),
            String.class);
    assertThat(third.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(session(access.sessionToken()).getBody()).isEqualTo(ready);
    assertThat(generate(access.sessionToken()).getBody()).isEqualTo(ready);
  }

  /** Exercita o limite pelo controller real, inclusive validação recursiva dos itens. */
  @Test
  void twelveItemsAcceptedThirteenthRejected() {
    var access = create(7006L);
    assertThat(input(access.sessionToken(), 12).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(generate(access.sessionToken()).getBody().routine()).hasSize(12);
    assertThat(
            exchange(access.sessionToken(), "/input", HttpMethod.PUT, inputs(13), String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(session(access.sessionToken()).getBody().organizationsUsed()).isEqualTo(1);
  }

  /** Faz a expiração exclusivamente na sessão sintética do banco local e comprova a recusa HTTP. */
  @Test
  void expiredCredentialRejected() {
    var access = create(7017L);
    new org.springframework.transaction.support.TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              var row = sessions.findById(access.session().id()).orElseThrow();
              row.setExpiresAt(Instant.now().minusSeconds(1));
              sessions.saveAndFlush(row);
            });
    assertThat(
            exchange(access.sessionToken(), "/session", HttpMethod.GET, null, String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /** Usa a revogação oficial e preserva o registro auditável em vez de apagar a sessão. */
  @Test
  void revokedCredentialRejected() {
    var access = create(7006L);
    var headers = new HttpHeaders();
    headers.set("X-PDE-Internal-Token", "mira-local-internal-only");
    assertThat(
            http.exchange(
                    ROOT + "/internal/sessions/" + access.session().id(),
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    Void.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(
            exchange(access.sessionToken(), "/session", HttpMethod.GET, null, String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(sessions.findById(access.session().id())).isPresent();
  }

  /** Fecha somente o double do ciclo e mantém consulta, recusando entrada, geração e evento. */
  @Test
  void closedContextRejectsMutationsAndPreservesResult() {
    var access = create(7017L);
    input(access.sessionToken(), 1);
    var ready = generate(access.sessionToken()).getBody();
    when(cycles.findLockedById(7017L))
        .thenReturn(Optional.of(cycle(7017L, "CLOSED", MiraPrivateService.VERSION)));
    assertMutationsRejected(access.sessionToken());
    assertThat(session(access.sessionToken()).getBody()).isEqualTo(ready);
  }

  /** Divergência de versão impede mutações da sessão, sem herdar autorização de outro contexto. */
  @Test
  void incompatibleContextRejectsMutations() {
    var access = create(7006L);
    input(access.sessionToken(), 1);
    var ready = generate(access.sessionToken()).getBody();
    when(cycles.findLockedById(7006L))
        .thenReturn(Optional.of(cycle(7006L, "OPEN", "incompatible-version")));
    assertMutationsRejected(access.sessionToken());
    assertThat(session(access.sessionToken()).getBody()).isEqualTo(ready);
  }

  /** Confere separadamente cada comando mutável pelo contrato HTTP canônico. */
  private void assertMutationsRejected(String token) {
    assertThat(exchange(token, "/input", HttpMethod.PUT, inputs(1), String.class).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
    assertThat(exchange(token, "/generate", HttpMethod.POST, null, String.class).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
    assertThat(
            exchange(
                    token, "/events", HttpMethod.POST, new Event("READY_RESULT_USED"), String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
  }

  /** Cria acesso somente por endpoint oficial, mantendo os identificadores segregados. */
  private CreatedSession create(long id) {
    var headers = new HttpHeaders();
    headers.set("X-PDE-Internal-Token", "mira-local-internal-only");
    var request =
        new Create(
            id,
            "experiment:" + (id + 2000),
            MiraPrivateService.VERSION,
            "REDUCED",
            "ADHERENT",
            "DESKTOP_1440");
    var response =
        http.postForEntity(
            ROOT + "/internal/sessions", new HttpEntity<>(request, headers), CreatedSession.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody();
  }

  /** Monta entradas documentais reconhecíveis sem dado pessoal ou provedor externo. */
  private Input inputs(int count) {
    return new Input(
        "Organizar meus cuidados",
        IntStream.range(0, count)
            .mapToObj(i -> new ProductInput("Produto " + i, "Limpar e enxaguar."))
            .toList());
  }

  /** Envia a entrada pelo controller que aplica os limites de tamanho e dos itens. */
  private ResponseEntity<SessionView> input(String token, int count) {
    return exchange(token, "/input", HttpMethod.PUT, inputs(count), SessionView.class);
  }

  /** Solicita geração determinística pela API com a credencial sintética. */
  private ResponseEntity<SessionView> generate(String token) {
    return exchange(token, "/generate", HttpMethod.POST, null, SessionView.class);
  }

  /** Consulta o estado persistido sem consumir outra organização. */
  private ResponseEntity<SessionView> session(String token) {
    return exchange(token, "/session", HttpMethod.GET, null, SessionView.class);
  }

  /** Centraliza somente o transporte de teste sem imprimir credenciais. */
  private <T> ResponseEntity<T> exchange(
      String token, String path, HttpMethod method, Object body, Class<T> type) {
    var headers = new HttpHeaders();
    headers.set("X-Mira-Session", token);
    return http.exchange(ROOT + path, method, new HttpEntity<>(body, headers), type);
  }

  /** Constrói o cadastro sintético; alterações de estado nunca chegam ao banco publicado. */
  private LearningSalesCycle cycle(long id, String status, String version) {
    var value = new LearningSalesCycle();
    value.setId(id);
    value.setProductId(id + 1000);
    value.setExperimentId(id + 2000);
    value.setStage("ADJUSTMENT");
    value.setStatus(status);
    value.setProductVersion(version);
    return value;
  }
}
