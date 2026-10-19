package com.marketinghub.aiworkerconnection;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.aiworkerconnection.controller.AiWorkerConnectionController;
import com.marketinghub.aiworkerconnection.service.AiWorkerConnectionService;
import com.marketinghub.repository.jpa.aiworkerconnection.AiWorkerConnectionRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Exercita HTTP, validação, persistência e exclusão mútua com banco sintético isolado. */
@ActiveProfiles("worker-connection-fixture")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = AiWorkerConnectionFlowTest.Fixture.class,
    properties = {
      "spring.config.name=connection-test",
      "spring.liquibase.enabled=false",
      "spring.datasource.url=jdbc:h2:mem:com.marketinghub.aiworkerconnection.AiWorkerConnectionFlowTest-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=0",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class AiWorkerConnectionFlowTest {
  @Autowired TestRestTemplate http;
  @Autowired JdbcTemplate database;
  private static final String ROOT = "/api/ai/worker-connection";

  /**
   * Comprova o fluxo, os callbacks repetidos e a impossibilidade de confirmar comandos vencidos.
   */
  @Test
  void connectsOnceExpiresAndRecoversWithoutCredentials() {
    assertThat(current().get("connected")).isEqualTo(false);
    Map<String, Object> first = request();
    assertThat(request().get("id")).isEqualTo(first.get("id"));
    assertThat(http.getForEntity(ROOT + "/internal/pending", Map.class).getBody().get("status"))
        .isEqualTo("STARTING");
    assertThat(http.getForEntity(ROOT + "/internal/pending", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);
    String command = ROOT + "/internal/" + first.get("id");
    assertThat(
            http.postForEntity(
                    command + "/device-code",
                    Map.of(
                        "verificationUrl",
                        "https://invalid.example/login",
                        "userCode",
                        "TEST-1234"),
                    Map.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
    Map<String, String> code =
        Map.of("verificationUrl", "https://auth.openai.com/codex/device", "userCode", "TEST-1234");
    for (int i = 0; i < 2; i++) {
      assertThat(http.postForEntity(command + "/device-code", code, Map.class).getStatusCode())
          .isEqualTo(HttpStatus.OK);
    }
    for (int i = 0; i < 2; i++) {
      Map<String, Object> result =
          http.postForEntity(command + "/completion", Map.of("authenticated", true), Map.class)
              .getBody();
      assertThat(result.get("connected")).isEqualTo(true);
      assertThat(result.get("userCode")).isNull();
    }
    assertThat(current().keySet()).doesNotContain("token", "email", "auth", "apiKey");
    database.update(
        "UPDATE ai_worker_codex_connection SET checked_at=? WHERE id=1",
        Timestamp.from(Instant.now().minusSeconds(240)));
    assertThat(current().get("connected")).isEqualTo(false);
    assertThat(current().get("workerAvailable")).isEqualTo(false);
    Map<String, Object> next = request();
    database.update(
        "UPDATE ai_worker_codex_connection SET expires_at=? WHERE id=?",
        Timestamp.from(Instant.now().minusSeconds(1)),
        next.get("id"));
    assertThat(current().get("status")).isEqualTo("EXPIRED");
    assertThat(
            http.postForEntity(
                    ROOT + "/internal/" + next.get("id") + "/completion",
                    Map.of("authenticated", true),
                    Map.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
    Map<String, Object> last = request();
    assertThat(last.get("id")).isNotEqualTo(next.get("id"));

    List<CompletableFuture<HttpStatus>> claims = new ArrayList<>();
    for (int i = 0; i < 4; i++)
      claims.add(
          CompletableFuture.supplyAsync(
              () ->
                  HttpStatus.valueOf(
                      http.getForEntity(ROOT + "/internal/pending", Map.class)
                          .getStatusCode()
                          .value())));
    assertThat(claims.stream().map(CompletableFuture::join).filter(HttpStatus.OK::equals).count())
        .isEqualTo(1);
    assertThat(
            database.queryForObject(
                "SELECT COUNT(*) FROM ai_worker_codex_connection", Integer.class))
        .isEqualTo(4);
  }

  /** Consulta o estado administrativo pelo mesmo contrato usado pelo frontend. */
  private Map<String, Object> current() {
    return http.getForObject(ROOT, Map.class);
  }

  /** Solicita nova conexão sem qualquer campo de credenciais. */
  private Map<String, Object> request() {
    return http.postForObject(ROOT, null, Map.class);
  }

  /** Disponibiliza um backend sintético reutilizável pela navegação local da matriz. */
  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(Fixture.class);
    application.setAdditionalProfiles("worker-connection-fixture");
    application.setDefaultProperties(
        Map.of(
            "spring.config.name",
            "connection-test",
            "spring.liquibase.enabled",
            "false",
            "spring.datasource.url",
            "jdbc:h2:mem:com.marketinghub.aiworkerconnection.AiWorkerConnectionFlowTest-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=0",
            "spring.datasource.username",
            "sa",
            "spring.datasource.password",
            "",
            "spring.jpa.hibernate.ddl-auto",
            "create-drop",
            "server.address",
            "127.0.0.1"));
    application.run(args);
  }

  /** Restringe o contexto ao módulo de conexão, sem filas, integrações ou dados comerciais. */
  @Configuration(proxyBeanMethods = false)
  @Profile("worker-connection-fixture")
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = AiWorkerConnection.class)
  @EnableJpaRepositories(basePackageClasses = AiWorkerConnectionRepository.class)
  @Import({AiWorkerConnectionController.class, AiWorkerConnectionService.class})
  static class Fixture {
    /** Prepara a mesma linha de controle criada pelo changelog produtivo. */
    @Bean
    ApplicationRunner seed(AiWorkerConnectionRepository repository) {
      return args -> {
        AiWorkerConnection control = new AiWorkerConnection(Instant.now(), Instant.now());
        control.finish("DISCONNECTED", Instant.now());
        repository.saveAndFlush(control);
      };
    }
  }
}
