package com.marketinghub.chatgptads.v1;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.marketinghub.chatgptads.v1.service.*;
import com.marketinghub.chatgptads.v1.service.managecampaign.ChatgptAdsContracts.*;
import com.marketinghub.creative.*;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.run.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jdbc.chatgptads.ChatgptAdsRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

/** Verifica o percurso real do serviço com persistência SQL e provedores determinísticos. */
class ChatgptAdsServiceTest {
  ObjectMapper mapper;
  JdbcTemplate jdbc;
  ChatgptAdsRepository repository;
  ChatgptAdsService service;
  ExperimentRepository experiments;
  CreativeRepository creatives;
  ExperimentRunRepository runs;
  ExperimentRunGateResultRepository gates;
  Experiment experiment;
  Creative creative;
  ExperimentRun run;
  Draft draft;

  /** Monta fontes separadas e schema descartável sem consultar nem alterar produção. */
  @BeforeEach
  void setup() {
    mapper = new ObjectMapper().findAndRegisterModules();
    String url =
        System.getProperty(
            "chatgpt.test.jdbc",
            "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                url,
                System.getProperty("chatgpt.test.user", "sa"),
                System.getProperty("chatgpt.test.password", "")));
    jdbc.execute("DROP TABLE IF EXISTS chatgpt_ads_operation");
    jdbc.execute("DROP TABLE IF EXISTS chatgpt_ads_campaign");
    jdbc.execute("DROP TABLE IF EXISTS chatgpt_ads_connection");
    jdbc.execute(
        "CREATE TABLE chatgpt_ads_connection(id BIGINT PRIMARY KEY,details_json LONGTEXT NOT NULL)");
    jdbc.execute("INSERT INTO chatgpt_ads_connection VALUES(1,'{}')");
    jdbc.execute(
        "CREATE TABLE chatgpt_ads_campaign(id BIGINT AUTO_INCREMENT PRIMARY KEY,experiment_id BIGINT NOT NULL UNIQUE,draft_json LONGTEXT NOT NULL,fingerprint VARCHAR(64) NOT NULL,authorization_json LONGTEXT,status VARCHAR(32),remote_json LONGTEXT,metrics_json LONGTEXT,created_at DATETIME,updated_at DATETIME)");
    jdbc.execute(
        "CREATE TABLE chatgpt_ads_operation(id BIGINT AUTO_INCREMENT PRIMARY KEY,campaign_id BIGINT,kind VARCHAR(32),request_json LONGTEXT,response_json LONGTEXT,status VARCHAR(24),lease_token VARCHAR(64),lease_until DATETIME,attempts INT,created_at DATETIME,finished_at DATETIME)");
    repository = new ChatgptAdsRepository(jdbc);
    experiments = mock(ExperimentRepository.class);
    creatives = mock(CreativeRepository.class);
    runs = mock(ExperimentRunRepository.class);
    gates = mock(ExperimentRunGateResultRepository.class);
    service = new ChatgptAdsService(repository, experiments, creatives, runs, gates, mapper);
    experiment =
        Experiment.builder()
            .id(991L)
            .name("Produto sintético")
            .product(Product.builder().id(881L).name("Teste").build())
            .platform(ExperimentPlatform.CHATGPT_ADS)
            .status(ExperimentStatus.PLANNED)
            .followUpActionUrl("https://example.org/sample")
            .build();
    creative =
        Creative.builder()
            .id(771L)
            .experiment(experiment)
            .status(CreativeStatus.READY)
            .agentReviewStatus(CreativeAgentReviewStatus.APPROVED)
            .headline("Seu trabalho merece destaque")
            .primaryText("Veja uma amostra de divulgação pronta para usar.")
            .imageUrl("https://example.org/image.png")
            .destinationUrl(experiment.getFollowUpActionUrl())
            .build();
    run =
        ExperimentRun.builder()
            .id(661L)
            .experiment(experiment)
            .mode(ExperimentRunMode.PRODUCTION)
            .status(ExperimentRunStatus.READY_TO_PUBLISH)
            .preflightCompletedAt(Instant.now())
            .dataQualityStatus(ExperimentRunDataQualityStatus.VALID)
            .build();
    when(experiments.existsById(991L)).thenReturn(true);
    when(experiments.findById(991L)).thenReturn(Optional.of(experiment));
    when(creatives.findById(771L)).thenReturn(Optional.of(creative));
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(991L, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));
    when(gates.findByExperimentRunIdOrderByGateGroupAscGateCodeAsc(661L))
        .thenReturn(
            List.of(
                    "LANDING_QUALITY_REVIEW_APPROVED",
                    "CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED",
                    "CHATGPT_ADS_READINESS_CONFIRMED",
                    "DATA_FRESHNESS_VALID")
                .stream()
                .map(
                    code ->
                        ExperimentRunGateResult.builder()
                            .gateCode(code)
                            .status(ExperimentRunGateStatus.PASS)
                            .evidenceReference("sandbox://proof/" + code)
                            .build())
                .toList());
    draft =
        new Draft(
            991L,
            771L,
            "Teste do novo canal",
            creative.getHeadline(),
            creative.getPrimaryText(),
            creative.getImageUrl(),
            creative.getDestinationUrl(),
            List.of("Organizar a divulgação da semana"),
            "BRL",
            new BigDecimal("25.00"),
            new BigDecimal("1.00"),
            Instant.now().minusSeconds(7200).truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
            Instant.now().plusSeconds(86400).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    repository.account("{\"id\":\"adacct_test\",\"status\":\"active\",\"currency_code\":\"BRL\"}");
  }

  /** Autoriza explicitamente o rascunho de teste depois de persistir sua identidade. */
  private long prepared() {
    var c = service.save(null, draft);
    service.authorize(
        c.id(), new Authorization(c.fingerprint(), "AUTORIZO CHATGPT ADS", "Operador de teste"));
    return c.id();
  }

  /** Entrega resposta sintética para a operação esperada e retorna a reserva usada. */
  private Work reply(String kind, Object payload) {
    Work w = service.pending();
    assertNotNull(w);
    assertEquals(kind, w.kind());
    if (kind.equals("METRICS"))
      assertTrue(w.path().contains("time_granularity=none&aggregation_level=campaign"));
    service.receive(w.id(), new Receipt(w.lease(), 200, mapper.valueToTree(payload), null));
    return w;
  }

  /** Prepara a cadeia inteira até o parecer do provedor, sem ativação implícita. */
  private long published() {
    long id = prepared();
    service.publish(id);
    reply("CAMPAIGN", campaignResponse("paused"));
    reply("GROUP", Map.of("id", "adgrp_test", "status", "paused"));
    reply("IMAGE", Map.of("file_id", "file_test"));
    reply("AD", Map.of("id", "ad_test", "status", "paused"));
    reply("INSPECT", Map.of("id", "ad_test", "review_status", "approved"));
    return id;
  }

  /** Cria contrato do provedor com limites nativos efetivos para os cenários. */
  private Map<String, Object> campaignResponse(String status) {
    return Map.of(
        "id",
        "camp_test",
        "status",
        status,
        "budget",
        Map.of("lifetime_spend_limit_micros", 25000000),
        "start_time",
        draft.startsAt().getEpochSecond(),
        "end_time",
        draft.endsAt().getEpochSecond());
  }

  /** Reconfere teto, lance e material exatamente como o provedor devolveria. */
  private void verified() {
    reply("VERIFY_CAMPAIGN", campaignResponse("paused"));
    reply(
        "VERIFY_GROUP",
        Map.of(
            "id",
            "adgrp_test",
            "bidding_config",
            Map.of("max_bid_micros", 1000000, "strategy", "fixed_bid")));
    reply(
        "VERIFY_AD",
        Map.of(
            "id",
            "ad_test",
            "review_status",
            "approved",
            "creative",
            Map.of(
                "title",
                draft.title(),
                "body",
                draft.body(),
                "target_url",
                draft.targetUrl(),
                "file_id",
                "file_test")));
  }

  /** Percorre preparação, publicação pausada, revisão, ativação, exposição e pausa com SQL real. */
  @Test
  void completesLifecycleWithoutFabricatingPurchases() {
    long id = published();
    assertEquals("READY", repository.get(id).status());
    assertEquals(ExperimentStatus.PLANNED, experiment.getStatus());
    assertNull(repository.get(id).metrics());
    service.activate(id);
    verified();
    reply("ACTIVATE_AD", Map.of("status", "active"));
    reply("ACTIVATE_GROUP", Map.of("status", "active"));
    reply("ACTIVATE_CAMPAIGN", campaignResponse("active"));
    assertEquals(ExperimentStatus.PLANNED, experiment.getStatus());
    reply(
        "METRICS",
        Map.of(
            "data",
            List.of(
                Map.of(
                    "campaign_id",
                    "camp_test",
                    "impressions",
                    10,
                    "clicks",
                    1,
                    "spend",
                    new BigDecimal("0.80"))),
            "has_more",
            false));
    assertEquals(ExperimentStatus.RUNNING, experiment.getStatus());
    assertFalse(repository.get(id).metrics().contains("purchases"));
    service.pause(id);
    reply("PAUSE", campaignResponse("paused"));
    assertEquals("PAUSED", repository.get(id).status());
    assertEquals(ExperimentStatus.PAUSED, experiment.getStatus());
  }

  /**
   * Mantém a autorização Meta completamente separada mesmo com valores disponíveis no experimento.
   */
  @Test
  void refusesInheritedBudgetAndWrongChannel() {
    var c = service.save(null, draft);
    assertThrows(ResponseStatusException.class, () -> service.publish(c.id()));
    assertNull(service.pending());
    service.authorize(c.id(), new Authorization(c.fingerprint(), "AUTORIZO CHATGPT ADS", "Teste"));
    experiment.setPlatform(ExperimentPlatform.FACEBOOK);
    assertThrows(ResponseStatusException.class, () -> service.publish(c.id()));
  }

  /** Revoga autorização quando qualquer campo do material é editado. */
  @Test
  void editingRevokesAuthorization() {
    long id = prepared();
    service.save(id, draft);
    assertNull(repository.get(id).authorization());
    assertThrows(ResponseStatusException.class, () -> service.publish(id));
  }

  /** Bloqueia a revisão de outra versão, mesmo que exista uma aprovação antiga. */
  @Test
  void rejectsChangedCreativeAndExpiredPreflight() {
    long id = prepared();
    creative.setPrimaryText("Texto diferente aprovado em outro contexto");
    assertThrows(ResponseStatusException.class, () -> service.publish(id));
    creative.setPrimaryText(draft.body());
    run.setPreflightCompletedAt(Instant.now().minusSeconds(8 * 86400));
    assertThrows(ResponseStatusException.class, () -> service.publish(id));
  }

  /** Repetir callback não duplica a próxima etapa nem a publicação no provedor. */
  @Test
  void duplicateCallbacksAndConcurrentCommandsDoNotDuplicateWork() {
    long id = prepared();
    service.publish(id);
    service.publish(id);
    Work w = reply("CAMPAIGN", campaignResponse("paused"));
    service.receive(
        w.id(), new Receipt(w.lease(), 200, mapper.valueToTree(campaignResponse("paused")), null));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM chatgpt_ads_operation WHERE kind='GROUP'", Integer.class));
  }

  /** Recuperar uma falha usa o mesmo registro e permite a próxima etapa sem duplicação. */
  @Test
  void failedOperationResumesWithSameIdentity() {
    long id = prepared();
    service.publish(id);
    reply("CAMPAIGN", campaignResponse("paused"));
    Work failed = service.pending();
    service.receive(
        failed.id(), new Receipt(failed.lease(), 503, mapper.createObjectNode(), "HTTP_503"));
    assertEquals("BLOCKED", repository.get(id).status());
    service.retry(id);
    Work retried = service.pending();
    assertEquals(failed.id(), retried.id());
    assertEquals(failed.idempotencyKey(), retried.idempotencyKey());
    service.receive(
        retried.id(),
        new Receipt(
            retried.lease(),
            200,
            mapper.valueToTree(Map.of("id", "adgrp_test", "status", "paused")),
            null));
    assertEquals("IMAGE", service.pending().kind());
  }

  /** Lease expirado mantém a mesma chave de deduplicação do request original. */
  @Test
  void expiredLeaseReusesIdempotencyKey() {
    long id = prepared();
    service.publish(id);
    Work first = service.pending();
    jdbc.update(
        "UPDATE chatgpt_ads_operation SET lease_until=?",
        java.sql.Timestamp.from(Instant.now().minusSeconds(10)));
    Work second = service.pending();
    assertEquals(first.idempotencyKey(), second.idempotencyKey());
    assertNotEquals(first.lease(), second.lease());
    assertThrows(
        ResponseStatusException.class,
        () ->
            service.receive(
                first.id(),
                new Receipt(
                    first.lease(), 200, mapper.valueToTree(campaignResponse("paused")), null)));
  }

  /** Uma campanha publicada sem trava nativa é pausada antes de criar o anúncio. */
  @Test
  void nativeBudgetMismatchTriggersCompensation() {
    long id = prepared();
    service.publish(id);
    reply(
        "CAMPAIGN",
        Map.of(
            "id",
            "camp_test",
            "status",
            "paused",
            "budget",
            Map.of("lifetime_spend_limit_micros", 99999999)));
    assertEquals("PAUSING", repository.get(id).status());
    reply("PAUSE", campaignResponse("paused"));
    assertFalse(service.overview(null).campaigns().getFirst().blockers().isEmpty());
    assertFalse(service.overview(null).campaigns().getFirst().canActivate());
  }

  /** Uma pausa durante a ativação impede que a operação pendente volte a gastar. */
  @Test
  void pauseCancelsQueuedActivation() {
    long id = published();
    service.activate(id);
    service.pause(id);
    reply("PAUSE", campaignResponse("paused"));
    assertNull(service.pending());
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM chatgpt_ads_operation WHERE kind='ACTIVATE_AD'", Integer.class));
  }

  /** Se a ativação não receber resposta após três leases, a pausa tem prioridade. */
  @Test
  void uncertainActivationIsCompensated() {
    long id = published();
    service.activate(id);
    verified();
    Work operation = service.pending();
    jdbc.update(
        "UPDATE chatgpt_ads_operation SET attempts=3,lease_until=? WHERE id=?",
        java.sql.Timestamp.from(Instant.now().minusSeconds(10)),
        operation.id());
    assertNull(service.pending());
    assertEquals("BLOCKED", repository.get(id).status());
    assertEquals("PAUSE", service.pending().kind());
  }

  /** Um lance alterado fora do Hub não pode ativar com o aceite antigo. */
  @Test
  void changedNativeBidDoesNotActivate() {
    long id = published();
    service.activate(id);
    reply("VERIFY_CAMPAIGN", campaignResponse("paused"));
    reply(
        "VERIFY_GROUP",
        Map.of(
            "id",
            "adgrp_test",
            "bidding_config",
            Map.of("max_bid_micros", 9000000, "strategy", "fixed_bid")));
    assertEquals("BLOCKED", repository.get(id).status());
    assertNull(service.pending());
  }

  /** Alteração da moeda e da conta exige decisão própria sem conversão implícita. */
  @Test
  void rejectsAccountChangeAndCurrencyMismatch() {
    long id = prepared();
    repository.account(
        "{\"id\":\"other_account\",\"status\":\"active\",\"currency_code\":\"USD\"}");
    assertThrows(ResponseStatusException.class, () -> service.publish(id));
  }

  /** Revisão recusada não equivale a prontidão nem libera o botão de ativação. */
  @Test
  void rejectedReviewRemainsBlocked() {
    long id = published();
    service.sync(id);
    reply("INSPECT", Map.of("id", "ad_test", "review_status", "rejected"));
    assertFalse(service.overview(null).campaigns().getFirst().canActivate());
  }

  /** Rejeita dados de outra campanha e preserva recibo para diagnóstico. */
  @Test
  void rejectsWrongCampaignMetrics() {
    long id = published();
    service.activate(id);
    verified();
    reply("ACTIVATE_AD", Map.of("status", "active"));
    reply("ACTIVATE_GROUP", Map.of("status", "active"));
    reply("ACTIVATE_CAMPAIGN", campaignResponse("active"));
    reply(
        "METRICS",
        Map.of(
            "data",
            List.of(Map.of("campaign_id", "camp_other", "impressions", 20)),
            "has_more",
            false));
    assertEquals("BLOCKED", repository.get(id).status());
    assertNull(repository.get(id).metrics());
  }

  /** Uma falha de pausa continua recuperável após os gates comerciais vencerem. */
  @Test
  void pauseRecoveryDoesNotRequireSpendingAuthorization() {
    long id = published();
    service.pause(id);
    Work work = service.pending();
    service.receive(
        work.id(), new Receipt(work.lease(), 503, mapper.createObjectNode(), "HTTP_503"));
    run.setPreflightCompletedAt(Instant.now().minusSeconds(8 * 86400));
    service.retry(id);
    reply("PAUSE", campaignResponse("paused"));
    assertEquals("PAUSED", repository.get(id).status());
    assertFalse(repository.get(id).remote().contains("HTTP_503"));
  }

  /** Valida os limites antes de criar rascunho ou reservar trabalho. */
  @Test
  void policyRejectsPrivateUrlsAndInvalidCurrency() {
    assertFalse(ChatgptAdsPolicy.publicHttps("http://example.org/a"));
    assertFalse(ChatgptAdsPolicy.publicHttps("https://127.0.0.1/a"));
    assertFalse(ChatgptAdsPolicy.publicHttps("https://user:secret@example.org/a"));
    assertEquals(1000000, ChatgptAdsPolicy.micros(BigDecimal.ONE));
  }

  /** Outra identidade percorre o mesmo contrato sem exceções por produto. */
  @Test
  void supportsAnotherProductIdentity() {
    experiment.setName("Outro produto");
    experiment.getProduct().setId(882L);
    experiment.getProduct().setName("Outra experiência");
    long id = published();
    assertEquals("Outra experiência", service.overview(null).campaigns().getFirst().productName());
    assertEquals("READY", repository.get(id).status());
  }

  /** Após a pausa, espera cobertura da última hora antes de declarar a conciliação final. */
  @Test
  void reconcilesFinalSpendAfterPauseWithoutReactivatingOrRepeatingForever() throws Exception {
    long id = published();
    service.pause(id);
    reply("PAUSE", campaignResponse("paused"));
    assertNull(service.pending());
    var remote =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.readTree(repository.get(id).remote());
    remote.put("stopCompletedAt", Instant.now().minusSeconds(3600).toString());
    repository.state(id, "PAUSED", remote.toString(), Instant.now().minusSeconds(120));
    reply(
        "METRICS",
        Map.of(
            "data",
            List.of(
                Map.of("campaign_id", "camp_test", "spend", 4.5, "impressions", 100, "clicks", 3)),
            "has_more",
            false));
    var metrics = mapper.readTree(repository.get(id).metrics());
    assertTrue(metrics.hasNonNull("finalSyncedAt"));
    assertEquals("PAUSED", repository.get(id).status());
    assertNull(service.pending());
    assertTrue(repository.monitored().isEmpty());
    var canonical =
        new com.marketinghub.experiment.monitoring.ExperimentAcquisitionMetricsReader(
                repository, mapper)
            .read(experiment);
    assertEquals(new BigDecimal("4.5"), canonical.spendBrl());
    assertNotNull(canonical.finalSyncedAt());
    assertEquals("OPERATOR_PAUSE", canonical.stopReason());
    service.activate(id);
    assertFalse(mapper.readTree(repository.get(id).metrics()).hasNonNull("finalSyncedAt"));
    assertFalse(mapper.readTree(repository.get(id).remote()).hasNonNull("stopCompletedAt"));
  }
}
