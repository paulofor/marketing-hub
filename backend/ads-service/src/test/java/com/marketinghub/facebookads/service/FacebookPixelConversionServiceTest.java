package com.marketinghub.facebookads.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketinghub.repository.jdbc.facebookads.FacebookPixelConversionJdbcRepository;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Valida a fila idempotente de compras PDE enviada à Conversions API da Meta. */
class FacebookPixelConversionServiceTest {
  private JdbcTemplate jdbc;
  private FacebookPixelConversionService service;

  /** Monta um schema mínimo isolado e uma compra real de Mira aprovada. */
  @BeforeEach
  void setUp() {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL(
        "jdbc:h2:mem:facebook_pixel_conversion;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
    jdbc = new JdbcTemplate(dataSource);
    dropSchema();
    createSchema();
    seedApprovedMiraPayment();
    service = new FacebookPixelConversionService(new FacebookPixelConversionJdbcRepository(jdbc));
  }

  /** Entrega somente uma vez o pagamento real com valor, hash e destino autoritativos. */
  @Test
  void queuesAndAcknowledgesApprovedPdePaymentIdempotently() {
    List<FacebookPixelConversionService.PdePixelConversion> pending =
        service.listApprovedPdePurchasesPendingPixel(50);

    assertThat(pending).singleElement();
    var conversion = pending.getFirst();
    assertThat(conversion.sourceReference()).isEqualTo("MERCADO_PAGO:mp-mira-1");
    assertThat(conversion.eventId()).isEqualTo("pde:MERCADO_PAGO:mp-mira-1");
    assertThat(conversion.experimentId()).isEqualTo(93L);
    assertThat(conversion.pixelId()).isEqualTo("pixel-mira");
    assertThat(conversion.amount()).isEqualByComparingTo("49.00");
    assertThat(conversion.normalizedCurrency()).isEqualTo("BRL");
    assertThat(conversion.hashedEmail()).hasSize(64);
    assertThat(conversion.eventSourceUrl()).isEqualTo("https://mira.example");

    service.markPdeConversionRecorded(conversion.sourceReference(), conversion.pixelId());
    service.markPdeConversionRecorded(conversion.sourceReference(), conversion.pixelId());

    assertThat(service.listApprovedPdePurchasesPendingPixel(50)).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM facebook_pixel_conversion_delivery", Integer.class))
        .isEqualTo(1);
  }

  /** Impede que um ACK de outro pixel confirme ou retire a compra da fila vigente. */
  @Test
  void rejectsPdeAcknowledgementForAnotherPixel() {
    assertThatThrownBy(
            () ->
                service.markPdeConversionRecorded("MERCADO_PAGO:mp-mira-1", "pixel-de-outro-nicho"))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> {
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(exception.getReason()).contains("pixel vigente");
            });

    assertThat(service.listApprovedPdePurchasesPendingPixel(50)).hasSize(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM facebook_pixel_conversion_delivery", Integer.class))
        .isZero();
  }

  /** Remove tabelas da execução anterior preservada pelo banco em memória. */
  private void dropSchema() {
    jdbc.execute("DROP ALL OBJECTS");
  }

  /** Cria apenas as colunas usadas pelo contrato de seleção e confirmação. */
  private void createSchema() {
    jdbc.execute(
        """
        CREATE TABLE pde_payment_audit (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          provider VARCHAR(40) NOT NULL,
          transaction_id VARCHAR(191) NOT NULL,
          product_slug VARCHAR(191) NOT NULL,
          experience_version VARCHAR(80),
          amount_cents INT NOT NULL,
          currency VARCHAR(3) NOT NULL,
          payment_status VARCHAR(40) NOT NULL,
          buyer_reference_hash CHAR(64) NOT NULL,
          verified_at DATETIME NOT NULL
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE pde_production_slot (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          product_slug VARCHAR(191) NOT NULL,
          experience_version VARCHAR(120) NOT NULL,
          source_experiment_id BIGINT,
          status VARCHAR(32) NOT NULL,
          public_url VARCHAR(512) NOT NULL
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE experiment (
          id BIGINT PRIMARY KEY,
          name VARCHAR(255) NOT NULL,
          niche_id BIGINT NOT NULL,
          platform VARCHAR(40),
          campaign_objective VARCHAR(40)
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE market_niche (
          id BIGINT PRIMARY KEY,
          facebook_pixel_id VARCHAR(64)
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE facebook_pixel_conversion_delivery (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          source_type VARCHAR(32) NOT NULL,
          source_reference VARCHAR(255) NOT NULL,
          pixel_id VARCHAR(64) NOT NULL,
          meta_event_id VARCHAR(255) NOT NULL,
          recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
          UNIQUE (source_type, source_reference)
        )
        """);
  }

  /** Persiste a identidade comercial exata de Mira e um pagamento aprovado de R$ 49. */
  private void seedApprovedMiraPayment() {
    jdbc.update("INSERT INTO market_niche(id, facebook_pixel_id) VALUES (34, 'pixel-mira')");
    jdbc.update(
        "INSERT INTO experiment(id, name, niche_id, platform, campaign_objective) "
            + "VALUES (93, 'Mira', 34, 'FACEBOOK', 'SALES')");
    jdbc.update(
        "INSERT INTO pde_production_slot(product_slug, experience_version, "
            + "source_experiment_id, status, public_url) "
            + "VALUES ('pde-planejado-36', 'mira-commercial-v1', 93, 'READY', "
            + "'https://mira.example')");
    jdbc.update(
        "INSERT INTO pde_payment_audit(provider, transaction_id, product_slug, "
            + "experience_version, amount_cents, currency, payment_status, "
            + "buyer_reference_hash, verified_at) VALUES "
            + "('MERCADO_PAGO', 'mp-mira-1', 'pde-planejado-36', "
            + "'mira-commercial-v1', 4900, 'brl', 'approved', ?, CURRENT_TIMESTAMP)",
        "a".repeat(64));
  }
}
