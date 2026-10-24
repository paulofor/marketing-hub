package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentDto;
import com.marketinghub.experiment.monitoring.ExperimentAcquisitionMetricsReader;
import com.marketinghub.experiment.monitoring.ExperimentAcquisitionMetricsSnapshot;
import com.marketinghub.experiment.monitoring.pde.PdeExperimentAnalyticsReader;
import com.marketinghub.experiment.service.ExperimentCostReconciliationService;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jdbc.learningcycle.LeadPortalCycleMeasurementRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Responsabilidade: provar a conciliação Quartzo com SQL real e integrações externas simuladas. */
class LeadPortalCycleMeasurementIntegrationTest {
  private static final Instant START = Instant.parse("2026-08-20T00:00:00Z");
  private static final Instant END = Instant.parse("2026-09-30T02:59:00Z");
  private static final Instant EVENT = Instant.parse("2026-09-25T06:43:33Z");
  private static final Instant NOW = Instant.parse("2026-10-05T23:00:00Z");
  private static final String PAGE = "https://kit.example.test/flows/exp-88-v1";
  private JdbcTemplate jdbc;
  private LeadPortalCycleMeasurementRepository repository;
  private int eventId;

  /** Cria tabelas efêmeras; MySQL só é aceito no schema dedicado da sandbox. */
  @BeforeEach
  void setUp() throws Exception {
    String url = System.getenv("LEAD_PORTAL_CYCLE_TEST_JDBC_URL");
    boolean h2 = url == null;
    if (!h2
        && !url.matches(
            "jdbc:mysql://(sandbox-docker|127\\.0\\.0\\.1|localhost):[0-9]+/capella_cycle_test(?:\\?.*)?"))
      throw new IllegalArgumentException("Use somente capella_cycle_test da sandbox isolada");
    jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                h2
                    ? "jdbc:h2:mem:cycle_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1"
                    : url,
                h2
                    ? "sa"
                    : System.getenv().getOrDefault("LEAD_PORTAL_CYCLE_TEST_JDBC_USERNAME", "root"),
                h2
                    ? ""
                    : System.getenv()
                        .getOrDefault("LEAD_PORTAL_CYCLE_TEST_JDBC_PASSWORD", "local-cycle-test")));
    for (String table :
        List.of(
            "experiment_landing_analytics_event",
            "experiment_funnel_event",
            "lead_portal_purchase",
            "flow_submissions",
            "lead_portal_flow",
            "experiment_run",
            "experiment",
            "product",
            "mercadopago_webhook_log",
            "agenda_cheia_delivery",
            "facebook_ads_campaign",
            "gera_sales_page_publication_audit")) jdbc.execute("DROP TABLE IF EXISTS " + table);
    jdbc.execute(
        "CREATE TABLE experiment(id BIGINT PRIMARY KEY, lead_portal_flow_id BIGINT, product_id BIGINT, commercial_checkout_url VARCHAR(500), " + Experiment.class.getDeclaredField("unitPrice").getAnnotation(jakarta.persistence.Column.class).name() + " DECIMAL(12,2))");
    jdbc.execute(
        "CREATE TABLE experiment_run(id BIGINT PRIMARY KEY, experiment_id BIGINT, run_number INT, mode VARCHAR(30), asset_bundle_version INT, published_at DATETIME)");
    jdbc.execute(
        "CREATE TABLE lead_portal_flow(id BIGINT PRIMARY KEY, slug VARCHAR(150), experiment_id BIGINT)");
    jdbc.execute(
        "CREATE TABLE facebook_ads_campaign(id VARCHAR(50), external_id VARCHAR(50), experiment_id BIGINT)");
    jdbc.execute(
        "CREATE TABLE gera_sales_page_publication_audit(id BIGINT PRIMARY KEY, experiment_id BIGINT, published_at DATETIME, sales_page_url VARCHAR(500))");
    jdbc.execute(
        "CREATE TABLE experiment_funnel_event(id BIGINT PRIMARY KEY, experiment_id BIGINT, campaign_code VARCHAR(150), payload TEXT)");
    jdbc.execute(
        "CREATE TABLE experiment_landing_analytics_event(id BIGINT PRIMARY KEY, experiment_id BIGINT, funnel_event_id BIGINT, session_id VARCHAR(100), visitor_id VARCHAR(100), traffic_quality VARCHAR(30), event_type VARCHAR(80), occurred_at DATETIME)");
    jdbc.execute(
        "CREATE TABLE flow_submissions(id VARCHAR(100) PRIMARY KEY, flow_slug VARCHAR(150), campaign_code VARCHAR(150), answers TEXT)");
    jdbc.execute(
        "CREATE TABLE lead_portal_purchase(id BIGINT PRIMARY KEY AUTO_INCREMENT, submission_id VARCHAR(100), mp_payment_id VARCHAR(100), amount DECIMAL(12,2), currency VARCHAR(10), mp_status VARCHAR(30), mp_payment_payload TEXT, payment_approved_at DATETIME, checkout_accessed_at DATETIME, updated_at DATETIME, delivered_at DATETIME, zip_generated_at DATETIME, zip_object_key VARCHAR(500))");
    jdbc.execute("CREATE TABLE product(id BIGINT PRIMARY KEY, validation_definition_json TEXT)");
    jdbc.execute(
        "CREATE TABLE mercadopago_webhook_log(id BIGINT PRIMARY KEY AUTO_INCREMENT, resource_id VARCHAR(100), mercadopago_status VARCHAR(40), processing_status VARCHAR(40), mercadopago_response TEXT, created_at DATETIME)");
    jdbc.execute(
        "CREATE TABLE agenda_cheia_delivery(id BIGINT PRIMARY KEY AUTO_INCREMENT, payment_id VARCHAR(100), status VARCHAR(40), artifact_path VARCHAR(300), delivered_at DATETIME)");
    jdbc.update("INSERT INTO experiment(id,lead_portal_flow_id) VALUES(88,60),(94,60)");
    jdbc.update("INSERT INTO lead_portal_flow VALUES(60,'capella',88)");
    jdbc.update(
        "INSERT INTO facebook_ads_campaign VALUES('campaign-88','meta-88',88),('campaign-94','meta-94',94)");
    jdbc.update(
        "INSERT INTO gera_sales_page_publication_audit VALUES(32,88,?,?)",
        Timestamp.from(EVENT),
        PAGE);
    repository =
        new LeadPortalCycleMeasurementRepository(
            jdbc,
            new com.marketinghub.repository.jdbc.learningcycle.DirectCheckoutCyclePaymentRepository(
                jdbc, new ObjectMapper()));
    eventId = 0;
  }

  /** Reproduz checkout direto: a compra não existe na tabela dos pacotes do Lead Portal. */
  @ParameterizedTest
  @ValueSource(longs = {88, 94})
  void countsDirectCommercialPurchaseForDifferentExperiments(long experimentId) {
    directCheckout(experimentId);
    directReceipt("paid-" + experimentId, experimentId, "approved", "true", "67", EVENT, EVENT);
    if (experimentId == 94) {
      patchReceipt("paid-94", "experiment_id", "experimentId");
      patchReceipt("paid-94", "product_id", "productId");
    }
    var payments = repository.read(experimentId, START, END).payments();
    assertThat(payments.blocker()).isNull();
    assertThat(payments.purchases()).isEqualTo(1);
    assertThat(payments.grossRevenueBrl()).isEqualByComparingTo("67");
  }

  /** A mesma transação só conta uma vez; só a entrega final concluída comprova valor entregue. */
  @Test
  void directCheckoutDeduplicatesAndRequiresActualKitDelivery() {
    directCheckout(88);
    directReceipt("paid", 88, "approved", "true", "67", EVENT, EVENT);
    directReceipt("paid", 88, "approved", "true", "67", EVENT, EVENT.plusSeconds(1));
    assertThat(repository.read(88L, START, END).payments().purchases()).isEqualTo(1);
    assertThat(repository.read(88L, START, END).payments().deliveredNetSales()).isZero();
    jdbc.update(
        "INSERT INTO agenda_cheia_delivery(payment_id,status,artifact_path,delivered_at) VALUES('paid','DELIVERED','private/kit.zip',?)",
        Timestamp.from(EVENT.plusSeconds(2)));
    assertThat(repository.read(88L, START, END).payments().deliveredNetSales()).isEqualTo(1);
    jdbc.update("UPDATE agenda_cheia_delivery SET status='FAILED'");
    assertThat(repository.read(88L, START, END).payments().deliveredNetSales()).isZero();
  }

  /** Outro experimento, ambiente de teste e compras fora da janela não viram receita atual. */
  @Test
  void directCheckoutExcludesOtherExperimentsTestsAndOutsideWindow() {
    directCheckout(88);
    directReceipt("other", 94, "approved", "true", "67", EVENT, EVENT);
    directReceipt("sandbox", 88, "approved", "false", "67", EVENT, EVENT);
    directReceipt("old", 88, "approved", "true", "67", START.minusSeconds(1), EVENT);
    directReceipt("future", 88, "approved", "true", "67", END.plusSeconds(1), END.plusSeconds(1));
    directReceipt("qa", 88, "approved", "true", "67", EVENT, EVENT);
    patchReceipt("qa", "\"metadata\":{", "\"metadata\":{\"mh_test\":true,");
    var payment = repository.read(88L, START, END).payments();
    assertThat(payment.blocker()).isNull();
    assertThat(payment.purchases()).isZero();
    assertThat(payment.source()).isEqualTo("COMMERCIAL_CHECKOUT_WEBHOOK");
  }

  /** Recibo inválido, atribuição ambígua e reembolso impedem fabricar receita ou zero válido. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "product",
        "experiment",
        "conflict",
        "amount",
        "currency",
        "refund",
        "partial-refund",
        "date",
        "live",
        "identity",
        "processing",
        "invalid",
        "invalid-twice",
        "identity-changed"
      })
  void directCheckoutBlocksUnreconciledReceipts(String defect) {
    directCheckout(88);
    directReceipt("paid", 88, "approved", "true", "67", EVENT, EVENT);
    switch (defect) {
      case "product" -> patchReceipt("paid", "\"product_id\":1088", "\"product_id\":999");
      case "experiment" -> patchReceipt("paid", "\"experiment_id\":88", "\"experiment_id\":null");
      case "conflict" ->
          patchReceipt("paid", "\"experiment_id\":88", "\"experimentId\":94,\"experiment_id\":88");
      case "amount" ->
          patchReceipt("paid", "\"transaction_amount\":67", "\"transaction_amount\":0.67");
      case "currency" -> patchReceipt("paid", "\"BRL\"", "\"USD\"");
      case "refund" -> {
        directReceipt("paid", 88, "refunded", "true", "67", EVENT, EVENT.plusSeconds(1));
      }
      case "partial-refund" ->
          patchReceipt(
              "paid",
              "\"transaction_amount\":67",
              "\"transaction_amount_refunded\":10,\"transaction_amount\":67");
      case "date" -> patchReceipt("paid", EVENT.toString(), "sem-data");
      case "live" -> patchReceipt("paid", "\"live_mode\":true", "\"live_mode\":null");
      case "identity" -> patchReceipt("paid", "\"id\":\"paid\"", "\"id\":\"different\"");
      case "processing" ->
          jdbc.update("UPDATE mercadopago_webhook_log SET processing_status='ERROR'");
      case "identity-changed" ->
          directReceipt("paid", 94, "approved", "true", "67", EVENT, EVENT.plusSeconds(1));
      case "invalid-twice" -> {
        jdbc.update(
            "INSERT INTO mercadopago_webhook_log(resource_id,processing_status,mercadopago_response,created_at) VALUES('paid','ERROR',NULL,?),( 'paid','ERROR',NULL,?)",
            Timestamp.from(EVENT.plusSeconds(1)),
            Timestamp.from(EVENT.plusSeconds(2)));
      }
      case "invalid" ->
          jdbc.update(
              "INSERT INTO mercadopago_webhook_log(resource_id,processing_status,mercadopago_response,created_at) VALUES('paid','ERROR','invalid',?)",
              Timestamp.from(EVENT.plusSeconds(1)));
      default -> throw new IllegalArgumentException(defect);
    }
    assertThat(repository.read(88L, START, END).payments().blocker()).isNotBlank();
  }

  /** Recibo futuro não substitui o sucesso válido da janela nem dispara pagamento novo. */
  @Test
  void directCheckoutPreservesHistoricalReceiptAndProviderOffset() {
    directCheckout(88);
    directReceipt("paid", 88, "approved", "true", "67", EVENT, EVENT);
    patchReceipt("paid", EVENT.toString(), "2026-09-25T03:43:33-03:00");
    directReceipt("paid", 88, "refunded", "true", "67", EVENT, END.plusSeconds(1));
    assertThat(repository.read(88L, START, END).payments().purchases()).isEqualTo(1);
  }

  /** O checkout explícito não soma uma segunda fonte legada nem exige um fluxo de pacote. */
  @Test
  void directCheckoutDoesNotRequireLeadPortalPaymentPackage() {
    directCheckout(88);
    jdbc.update("DELETE FROM lead_portal_flow");
    directReceipt("paid", 88, "approved", "true", "67", EVENT, EVENT);
    var measurement = repository.read(88L, START, END);
    assertThat(measurement.paymentSourceAvailable()).isTrue();
    assertThat(measurement.payments().purchases()).isEqualTo(1);
    assertThat(measurement.payments().checkoutAccesses()).isZero();
  }

  /** Altera apenas o recibo sintético para representar uma resposta observável do provedor. */
  private void patchReceipt(String paymentId, String before, String after) {
    jdbc.update(
        "UPDATE mercadopago_webhook_log SET mercadopago_response=REPLACE(mercadopago_response,?,?) WHERE resource_id=?",
        before,
        after,
        paymentId);
  }

  /** Prepara a identidade comercial sem preferências ou chamadas externas. */
  private void directCheckout(long experimentId) {
    jdbc.update(
        "INSERT INTO product VALUES(?,?)",
        experimentId + 1000,
        "{\"checkoutIdentity\":{\"externalReference\":\"kit-profissional\"}}");
    jdbc.update(
        "UPDATE experiment SET product_id=?,commercial_checkout_url=?,unit_price_brl=67 WHERE id=?",
        experimentId + 1000,
        "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=test-" + experimentId,
        experimentId);
  }

  /** Persiste recibo simulado com metadados de atribuição, sem dados de compradores. */
  private void directReceipt(
      String paymentId,
      long experimentId,
      String status,
      String liveMode,
      String amount,
      Instant approvedAt,
      Instant receivedAt) {
    String payload =
        "{\"id\":\""
            + paymentId
            + "\",\"status\":\""
            + status
            + "\",\"live_mode\":"
            + liveMode
            + ",\"transaction_amount\":"
            + amount
            + ",\"currency_id\":\"BRL\",\"date_approved\":\""
            + approvedAt
            + "\",\"external_reference\":\"kit-profissional\",\"metadata\":{\"experiment_id\":"
            + experimentId
            + ",\"product_id\":"
            + (experimentId + 1000)
            + "}}";
    jdbc.update(
        "INSERT INTO mercadopago_webhook_log(resource_id,mercadopago_status,processing_status,mercadopago_response,created_at) VALUES(?,?,'PROCESSED',?,?)",
        paymentId,
        status,
        payload,
        Timestamp.from(receivedAt));
  }

  /** Confirma visitantes e sessões humanos, mantendo QA, bots e desconhecidos na auditoria. */
  @Test
  void reconcilesCapellaAndExcludesTechnicalAndOutOfScopeTraffic() {
    for (int index = 0; index < 7; index++)
      event(
          88L, "HUMAN", "page_view", "visitor-" + (index % 6), "session-" + (index % 6), "", EVENT);
    event(88L, "HUMAN", "page_view", "qa-visitor", "qa-session", "url=?mh_test=1", EVENT);
    event(
        88L,
        "HUMAN",
        "page_view",
        "audit-visitor",
        "audit-session",
        "url=?mh_audit=capella",
        EVENT);
    event(88L, "AUTOMATED", "checkout_click", "bot", "bot", "", EVENT);
    event(88L, "UNKNOWN", "page_view", "unknown", "unknown", "", EVENT);
    event(94L, "HUMAN", "page_view", "other", "other", "", EVENT);
    event(88L, "HUMAN", "page_view", "old", "old", "", START.minusSeconds(1));
    event(88L, "HUMAN", "page_view", "future", "future", "", END.plusSeconds(1));

    var snapshot = repository.read(88L, START, END);
    assertThat(snapshot.humanVisitors()).isEqualTo(6);
    assertThat(snapshot.humanSessions()).isEqualTo(6);
    assertThat(snapshot.pageViews()).isEqualTo(7);
    assertThat(snapshot.humanEvents()).isEqualTo(7);
    assertThat(snapshot.rawEvents()).isEqualTo(11);
    assertThat(snapshot.rawSessions()).isEqualTo(10);
    assertThat(snapshot.checkouts()).isZero();
    assertThat(snapshot.payments().purchases()).isZero();
    assertThat(snapshot.payments().blocker()).isNull();
    assertThat(snapshot.publicationId()).isEqualTo(32);
  }

  /** A consulta funciona com outro produto/experimento, sem regra especial por ID. */
  @Test
  void readsAnotherQuartzoThroughTheSameRepository() {
    jdbc.update("INSERT INTO experiment(id,lead_portal_flow_id) VALUES(117,70)");
    jdbc.update("INSERT INTO lead_portal_flow VALUES(70,'another-kit',117)");
    jdbc.update(
        "INSERT INTO gera_sales_page_publication_audit VALUES(40,117,?,?)",
        Timestamp.from(EVENT),
        "https://other.example.test/kit");
    event(117L, "HUMAN", "page_view", "another-person", "another-session", "", EVENT);
    var snapshot = repository.read(117L, START, END);
    assertThat(snapshot.humanVisitors()).isEqualTo(1);
    assertThat(snapshot.paymentSourceAvailable()).isTrue();
    assertThat(snapshot.publicationId()).isEqualTo(40);
  }

  /** Nova revisão ou run não publicado não substitui a publicação congelada no teste anterior. */
  @Test
  void keepsPinnedPublicationAcrossLaterRevisionsAndUnpublishedRuns() {
    publishedRun(19L, 88L, 1, 32L, EVENT.plusSeconds(1));
    jdbc.update(
        "INSERT INTO gera_sales_page_publication_audit VALUES(34,88,?,?)",
        Timestamp.from(EVENT.plusSeconds(60)),
        PAGE);
    publishedRun(20L, 88L, 2, 34L, null);
    publishedRun(21L, 88L, 3, 34L, END.plusSeconds(1));
    jdbc.update(
        "INSERT INTO experiment_run VALUES(22,88,4,'DRY_RUN',34,?)",
        Timestamp.from(EVENT.plusSeconds(120)));
    var snapshot = repository.read(88L, START, END);
    assertThat(snapshot.publicationId()).isEqualTo(32);
    assertThat(snapshot.publishedRunId()).isEqualTo(19);
    assertThat(snapshot.publicationScope()).isEqualTo("PUBLISHED_RUN_ASSET_BUNDLE");
    var result = collected(7L, 88L, "capella-private-v3", START, END, NOW);
    assertThat(result.evidence().at("/sources/leadPortalAnalytics/productVersion").asText())
        .isEqualTo("capella-private-v3");
  }

  /** Outra identidade usa o mesmo vínculo publicado, mesmo com contrato v1 e experiência v8. */
  @Test
  void reconcilesDifferentProductWithPinnedExperience() {
    jdbc.update("UPDATE gera_sales_page_publication_audit SET experiment_id=94 WHERE id=32");
    publishedRun(41L, 94L, 1, 32L, EVENT.plusSeconds(1));
    var result = collected(117L, 94L, "other-kit-v8", START, END, NOW);
    assertThat(result.evidence().at("/sources/leadPortalAnalytics/publishedRunId").asLong())
        .isEqualTo(41);
  }

  /** Vínculo inválido não cai silenciosamente na última página válida de outro contexto. */
  @ParameterizedTest
  @ValueSource(strings = {"foreign", "missing", "future"})
  void rejectsInvalidPinnedPublicationWithoutFallback(String problem) {
    if (!"missing".equals(problem))
      jdbc.update(
          "INSERT INTO gera_sales_page_publication_audit VALUES(34,?,?,?)",
          "foreign".equals(problem) ? 94L : 88L,
          Timestamp.from("future".equals(problem) ? EVENT.plusSeconds(60) : EVENT),
          PAGE);
    publishedRun(19L, 88L, 1, 34L, EVENT.plusSeconds(1));
    var snapshot = repository.read(88L, START, END);
    assertThat(snapshot.publicationId()).isNull();
    assertThat(snapshot.publishedRunId()).isEqualTo(19);
  }

  /** Legado sem asset preserva a página da época da publicação e explicita a limitação. */
  @Test
  void boundsLegacyPublicationByPublishedRunTime() {
    publishedRun(19L, 88L, 1, null, EVENT.plusSeconds(1));
    jdbc.update(
        "INSERT INTO gera_sales_page_publication_audit VALUES(34,88,?,?)",
        Timestamp.from(EVENT.plusSeconds(60)),
        PAGE);
    var snapshot = repository.read(88L, START, END);
    assertThat(snapshot.publicationId()).isEqualTo(32);
    assertThat(snapshot.publicationScope()).isEqualTo("LEGACY_PUBLICATION_AS_OF_RUN_OR_WINDOW");
  }

  /** Pagamento atribuído exige moeda e referência e distingue entrega de simples aprovação. */
  @Test
  void countsOnlyAttributedApprovedPaymentsAndDeliveryWithinWindow() {
    purchase("own", "88", "approved", "pay-own", "67.00", "BRL", EVENT, "{}", true);
    purchase("campaign", "meta-88", "approved", "pay-campaign", "67.00", "BRL", EVENT, "{}", false);
    purchase("other", "94", "approved", "pay-other", "67.00", "BRL", EVENT, "{}", true);
    purchase(
        "sandbox",
        "88",
        "approved",
        "pay-sandbox",
        "67.00",
        "BRL",
        EVENT,
        "{\"live_mode\":\n\tfalse}",
        true);
    purchase("qa", "__mh_internal_test__", "approved", "pay-qa", "67.00", "BRL", EVENT, "{}", true);
    purchase(
        "outside", "88", "approved", "pay-outside", "67.00", "BRL", END.plusSeconds(1), "{}", true);

    var snapshot = repository.read(88L, START, END);
    assertThat(snapshot.payments().blocker()).isNull();
    assertThat(snapshot.payments().purchases()).isEqualTo(2);
    assertThat(snapshot.payments().grossRevenueBrl()).isEqualByComparingTo("134.00");
    assertThat(snapshot.payments().deliveredNetSales()).isEqualTo(1);
  }

  /** Checkout adotado por outro experimento nunca duplica compra nem presume atribuição. */
  @Test
  void blocksUnattributedPurchaseInSharedCheckout() {
    purchase("ambiguous", null, "approved", "pay-unknown", "67.00", "BRL", EVENT, "{}", true);
    assertThat(repository.read(88L, START, END).payments().blocker()).contains("atribuição");
  }

  /** Fluxo exclusivo admite sua identidade histórica, sem exigir campanha para venda direta. */
  @Test
  void acceptsExclusiveFlowWithoutCampaignCode() {
    jdbc.update("DELETE FROM experiment WHERE id=94");
    purchase("direct", null, "approved", "pay-direct", "67.00", "BRL", EVENT, "{}", true);
    assertThat(repository.read(88L, START, END).payments().purchases()).isEqualTo(1);
  }

  /** Referência duplicada, moeda, valor ou status ilegíveis não se convertem em receita segura. */
  @ParameterizedTest
  @ValueSource(
      strings = {"duplicate", "reference", "amount", "currency", "date", "refund", "chargeback"})
  void blocksIncompleteFinancialEvidence(String problem) {
    purchase("first", "88", "approved", "pay-first", "67.00", "BRL", EVENT, "{}", true);
    purchase(
        "second",
        "88",
        "refund".equals(problem)
            ? "refunded"
            : "chargeback".equals(problem) ? "charged_back" : "approved",
        "reference".equals(problem)
            ? null
            : "duplicate".equals(problem) ? "pay-first" : "pay-second",
        "amount".equals(problem) ? null : "67.00",
        "currency".equals(problem) ? "USD" : "BRL",
        EVENT,
        "{}",
        true);
    if ("date".equals(problem))
      jdbc.update(
          "UPDATE lead_portal_purchase SET payment_approved_at=NULL WHERE submission_id='second'");
    var snapshot = repository.read(88L, START, END);
    assertThat(snapshot.payments().blocker()).isNotBlank();
    assertThat(snapshot.payments().grossRevenueBrl()).isNull();
  }

  /** Acesso ao checkout pendente é observado sem inventar compra ou entrega. */
  @Test
  void countsCheckoutAccessWithoutPrematurePurchase() {
    purchase("pending", "88", "pending", null, "67.00", "BRL", EVENT, "{}", false);
    jdbc.update(
        "UPDATE lead_portal_purchase SET payment_approved_at=NULL,checkout_accessed_at=?",
        Timestamp.from(EVENT));
    var snapshot = repository.read(88L, START, END);
    assertThat(snapshot.payments().checkoutAccesses()).isEqualTo(1);
    assertThat(snapshot.payments().purchases()).isZero();
  }

  /**
   * Aprovação anterior à janela não traz venda antiga; entrega posterior não vira prova retroativa.
   */
  @Test
  void neverMovesFinancialOrDeliveryEventsAcrossTheWindow() {
    purchase("old", "88", "approved", "pay-old", "67.00", "BRL", START.minusSeconds(1), "{}", true);
    purchase("late-delivery", "88", "approved", "pay-late", "67.00", "BRL", EVENT, "{}", true);
    jdbc.update(
        "UPDATE lead_portal_purchase SET delivered_at=? WHERE submission_id='late-delivery'",
        Timestamp.from(END.plusSeconds(1)));
    var snapshot = repository.read(88L, START, END);
    assertThat(snapshot.payments().purchases()).isEqualTo(1);
    assertThat(snapshot.payments().deliveredNetSales()).isZero();
  }

  /** Exercita SQL e coletor juntos, com mídia/custos simulados e sem depender de slot PDE. */
  @Test
  void producesValidatedAutomaticEvidenceFromActualSqlSources() {
    event(88L, "HUMAN", "page_view", "human", "session", "", EVENT);
    var result = collected(7L, 88L);
    assertThat(result.ready()).isTrue();
    assertThat(result.evidence().path("humanVisitors").asLong()).isEqualTo(1);
    assertThat(result.evidence().path("contributionBrl").decimalValue())
        .isEqualByComparingTo("-384.83");
  }

  /** A compra direta chega ao relatório com receita, contribuição e fonte próprias. */
  @Test
  void producesCommercialDecisionEvidenceFromDirectCheckout() {
    directCheckout(88);
    directReceipt("paid", 88, "approved", "true", "67", EVENT, EVENT);
    event(88L, "HUMAN", "checkout_click", "human", "session", "", EVENT);
    var result = collected(1088L, 88L);
    assertThat(result.ready()).isTrue();
    assertThat(result.evidence().path("netSales").asLong()).isEqualTo(1);
    assertThat(result.evidence().path("revenueBrl").decimalValue()).isEqualByComparingTo("67");
    assertThat(result.evidence().path("contributionBrl").decimalValue()).isEqualByComparingTo("-317.83");
    assertThat(result.evidence().path("checkoutClicks").asLong()).isEqualTo(1);
    assertThat(result.evidence().at("/sources/financialOutcomes/checkoutAccessEvents").asLong()).isZero();
    assertThat(result.evidence().at("/sources/financialOutcomes/source").asText()).isEqualTo("COMMERCIAL_CHECKOUT_WEBHOOK");
  }

  /** Exporta outra fotografia SQL para o BPM local, sem reescrever identificadores da evidência. */
  @Test
  void exportsSqlMeasurementForLocalBpm() throws Exception {
    Instant observed = Instant.now().minusSeconds(10);
    Instant published = observed.minusSeconds(3600);
    jdbc.update("INSERT INTO experiment(id,lead_portal_flow_id) VALUES(91001,60)");
    jdbc.update("UPDATE lead_portal_flow SET experiment_id=91001 WHERE id=60");
    jdbc.update("UPDATE gera_sales_page_publication_audit SET experiment_id=91001 WHERE id=32");
    jdbc.update(
        "UPDATE gera_sales_page_publication_audit SET published_at=? WHERE id=32",
        Timestamp.from(published));
    publishedRun(91001L, 91001L, 1, 32L, published.plusSeconds(1));
    for (int index = 0; index < 7; index++)
      event(
          91001L,
          "HUMAN",
          "page_view",
          "visitor-" + (index % 6),
          "session-" + (index % 6),
          "",
          observed.minusSeconds(600));
    event(
        91001L, "AUTOMATED", "checkout_click", "qa", "qa", "mh_test=1", observed.minusSeconds(600));
    var result =
        collected(91001L, 91001L, "fixture-commercial-91001-v12", published, observed, observed);
    assertThat(result.ready()).isTrue();
    assertThat(result.evidence().path("experimentId").asLong()).isEqualTo(91001);
    assertThat(result.evidence().path("humanVisitors").asLong()).isEqualTo(6);
    String output = System.getenv("LEAD_PORTAL_CYCLE_TEST_EVIDENCE_DIR");
    if (output != null) {
      Files.createDirectories(Path.of(output));
      Files.writeString(
          Path.of(output, "lead-portal-measurement.json"), result.evidence().toPrettyString());
    }
  }

  /** Concilia o SQL real com apenas mídia e ledger substituídos por fontes determinísticas. */
  private LearningCycleMeasurementCollector.Result collected(Long productId, Long experimentId) {
    return collected(productId, experimentId, "v1", START, END, NOW);
  }

  /** Exercita versões distintas do contrato com período e relógio explícitos. */
  private LearningCycleMeasurementCollector.Result collected(
      Long productId,
      Long experimentId,
      String productVersion,
      Instant start,
      Instant end,
      Instant observed) {
    var analytics = mock(PdeExperimentAnalyticsReader.class);
    var acquisition = mock(ExperimentAcquisitionMetricsReader.class);
    var costs = mock(ExperimentCostReconciliationService.class);
    var product =
        Product.builder()
            .id(productId)
            .slug("kit-local")
            .productTypeDefinition(
                ProductTypeDefinition.builder().code("LOW_TICKET_DIGITAL_PRODUCT").build())
            .validationDefinitionVersion("v1")
            .build();
    var experiment =
        Experiment.builder()
            .id(experimentId)
            .product(product)
            .status(ExperimentStatus.INVALIDATED)
            .platform(ExperimentPlatform.FACEBOOK)
            .followUpActionUrl(PAGE)
            .build();
    when(acquisition.read(experiment))
        .thenReturn(
            new ExperimentAcquisitionMetricsSnapshot(
                ExperimentPlatform.FACEBOOK,
                new BigDecimal("30.49"),
                end.minusSeconds(60),
                end.minusSeconds(60),
                null,
                1051L,
                8L,
                true));
    when(costs.enrich(eq(experiment), any(ExperimentDto.class)))
        .thenAnswer(
            call -> {
              ExperimentDto dto = call.getArgument(1);
              dto.setAuditableTotalCost(new BigDecimal("384.83"));
              dto.setLegacyTotalCost(new BigDecimal("30.63"));
              dto.setUnreconciledLegacyCost(BigDecimal.ZERO);
              return dto;
            });
    var collector =
        new LearningCycleMeasurementCollector(
            analytics, acquisition, costs, repository, new ObjectMapper());
    var cycle = new LearningSalesCycle();
    cycle.setId(5L);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setProductVersion(productVersion);
    cycle.setWindowStart(start);
    cycle.setWindowEnd(end);
    var result = collector.collect(cycle, experiment, observed);
    assertThat(result.ready()).isTrue();
    LearningCycleRules.validateMetrics(cycle, result.evidence(), observed);
    verifyNoInteractions(analytics);
    return result;
  }

  /** Persiste um run sintético para conferir a seleção histórica, sem qualquer publicação real. */
  private void publishedRun(Long id, Long experiment, int number, Long publication, Instant at) {
    jdbc.update(
        "INSERT INTO experiment_run VALUES(?,?,?,'PRODUCTION',?,?)",
        id,
        experiment,
        number,
        publication,
        at == null ? null : Timestamp.from(at));
  }

  /** Ausência de fluxo/publicação fica explícita, sem tratar indisponibilidade como desempenho. */
  @Test
  void preservesMissingSourcesAsMissing() {
    jdbc.update("DELETE FROM lead_portal_flow");
    jdbc.update("DELETE FROM gera_sales_page_publication_audit");
    var snapshot = repository.read(88L, START, END);
    assertThat(snapshot.paymentSourceAvailable()).isFalse();
    assertThat(snapshot.publicationId()).isNull();
  }

  /** Insere evento normalizado e bruto com o mesmo vínculo, sem tráfego comercial real. */
  private void event(
      Long experiment,
      String quality,
      String type,
      String visitor,
      String session,
      String payload,
      Instant at) {
    eventId++;
    jdbc.update(
        "INSERT INTO experiment_funnel_event VALUES(?,?,'',?)", eventId, experiment, payload);
    jdbc.update(
        "INSERT INTO experiment_landing_analytics_event VALUES(?,?,?,?,?,?,?,?)",
        eventId,
        experiment,
        eventId,
        session,
        visitor,
        quality,
        type,
        Timestamp.from(at));
  }

  /** Insere pagamento sintético, mantendo status, valor, entrega e atribuição separados. */
  private void purchase(
      String submission,
      String campaign,
      String status,
      String reference,
      String amount,
      String currency,
      Instant approved,
      String payload,
      boolean delivered) {
    jdbc.update("INSERT INTO flow_submissions VALUES(?,'capella',?,'{}')", submission, campaign);
    jdbc.update(
        "INSERT INTO lead_portal_purchase(submission_id,mp_payment_id,amount,currency,mp_status,mp_payment_payload,payment_approved_at,updated_at,delivered_at,zip_generated_at,zip_object_key) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
        submission,
        reference,
        amount == null ? null : new BigDecimal(amount),
        currency,
        status,
        payload,
        Timestamp.from(approved),
        Timestamp.from(approved),
        delivered ? Timestamp.from(approved) : null,
        delivered ? Timestamp.from(approved) : null,
        delivered ? "test-only/package.zip" : null);
  }
}
