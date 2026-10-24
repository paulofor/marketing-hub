package com.marketinghub.repository.jdbc.learningcycle;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.reconcileMeasurement.LeadPortalCycleMeasurement.Payments;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Concilia compras do checkout direto pela auditoria financeira existente, com atribuição exata.
 */
@Repository
public class DirectCheckoutCyclePaymentRepository {
  private static final Logger log =
      LoggerFactory.getLogger(DirectCheckoutCyclePaymentRepository.class);
  private static final String SOURCE = "COMMERCIAL_CHECKOUT_WEBHOOK";
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  /** Recebe a conexão canônica e o leitor dos recibos já persistidos pelo pagamento. */
  public DirectCheckoutCyclePaymentRepository(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  /** Seleciona checkout explícito e concilia seus recibos sem consultar ou cobrar o provedor. */
  public Optional<Payments> read(Long experimentId, Instant start, Instant end) {
    var contexts =
        jdbc.query(
            """
        SELECT e.product_id,e.commercial_checkout_url,e.unit_price_brl,p.validation_definition_json
        FROM experiment e LEFT JOIN product p ON p.id=e.product_id WHERE e.id=?
        """,
            (rs, row) ->
                new Context(
                    rs.getObject("product_id") == null ? null : rs.getLong("product_id"),
                    rs.getString("commercial_checkout_url"),
                    rs.getBigDecimal("unit_price_brl"),
                    rs.getString("validation_definition_json")),
            experimentId);
    if (contexts.isEmpty()
        || contexts.getFirst().checkout() == null
        || contexts.getFirst().checkout().isBlank()) return Optional.empty();
    Context context = contexts.getFirst();
    if (!mercadoPago(context.checkout()))
      return Optional.of(blocked("Checkout comercial sem fonte financeira suportada."));
    JsonNode definition = parse(context.definition(), "contrato", experimentId.toString());
    String reference = definition.path("checkoutIdentity").path("externalReference").asText("");
    if (context.productId() == null
        || reference.isBlank()
        || context.price() == null
        || context.price().signum() <= 0)
      return Optional.of(
          blocked("Checkout direto sem identidade e preço canônicos para conciliação."));
    List<Receipt> receipts =
        jdbc.query(
            """
        SELECT w.resource_id,w.mercadopago_status,w.processing_status,w.mercadopago_response,
          EXISTS(SELECT 1 FROM agenda_cheia_delivery d WHERE d.payment_id=w.resource_id
            AND d.status='DELIVERED' AND d.delivered_at<=?
            AND NULLIF(TRIM(d.artifact_path),'') IS NOT NULL) AS delivered
        FROM mercadopago_webhook_log w
        WHERE w.created_at<=? AND EXISTS(SELECT 1 FROM mercadopago_webhook_log recent
          WHERE (recent.resource_id=w.resource_id OR (recent.resource_id IS NULL AND w.resource_id IS NULL)) AND recent.created_at>=? AND recent.created_at<=?)
        ORDER BY w.id DESC
        """,
            (rs, row) ->
                new Receipt(
                    rs.getString("resource_id"),
                    rs.getString("mercadopago_status"),
                    rs.getString("processing_status"),
                    rs.getString("mercadopago_response"),
                    rs.getBoolean("delivered")),
            Timestamp.from(end),
            Timestamp.from(end),
            Timestamp.from(start),
            Timestamp.from(end));
    long purchases = 0, deliveries = 0;
    BigDecimal gross = BigDecimal.ZERO.setScale(2);
    var groups = new LinkedHashMap<String, List<Receipt>>();
    for (Receipt receipt : receipts)
      groups.computeIfAbsent(receipt.id(), ignored -> new ArrayList<>()).add(receipt);
    for (var history : groups.values()) {
      Receipt receipt = history.getFirst();
      var payloads =
          history.stream().map(item -> parse(item.payload(), "recibo", item.id())).toList();
      boolean relevant =
          payloads.stream()
              .anyMatch(
                  item -> {
                    String targetId = metadata(item, "experimentId", "experiment_id");
                    return targetId.equals(experimentId.toString())
                        || ((!targetId.matches("[1-9][0-9]*"))
                            && reference.equals(item.path("external_reference").asText()));
                  });
      if (!relevant) continue;
      JsonNode payload = payloads.getFirst();
      String target = metadata(payload, "experimentId", "experiment_id");
      if ("CONFLICT".equals(target))
        return Optional.of(blocked("Aliases de atribuição financeira conflitantes."));
      if (payload.path("live_mode").isBoolean() && !payload.path("live_mode").booleanValue())
        continue;
      if (testReceipt(payload)) continue;
      if (!target.equals(experimentId.toString()))
        return Optional.of(
            blocked(
                "Pagamento do produto sem atribuição ao experimento; não será contado por proximidade."));
      if (!payload.isObject() || !"PROCESSED".equals(receipt.processing()))
        return Optional.of(
            blocked(
                "Recibo atual do checkout indisponível ou processamento incompleto; não reutilize sucesso anterior."));
      if (!metadata(payload, "experimentId", "experiment_id").equals(target)
          || !metadata(payload, "productId", "product_id").equals(context.productId().toString())
          || !reference.equals(payload.path("external_reference").asText())
          || receipt.id() == null
          || !receipt.id().equals(payload.path("id").asText()))
        return Optional.of(
            blocked("Identidade do pagamento diverge do produto, experimento ou recibo."));
      if (!payload.path("live_mode").isBoolean() || !payload.path("live_mode").booleanValue())
        return Optional.of(blocked("Recibo sem identificação de ambiente real ou de teste."));
      String status = payload.path("status").asText();
      if (!status.equals(receipt.status()))
        return Optional.of(blocked("Status financeiro divergente no recibo do checkout."));
      if (List.of("pending", "in_process", "authorized", "rejected", "cancelled").contains(status))
        continue;
      if (!"approved".equals(status)
          || payload.path("transaction_amount_refunded").decimalValue().signum() > 0)
        return Optional.of(
            blocked("Reembolso ou desfecho financeiro exige conciliação datada antes da decisão."));
      Instant approved = date(payload.path("date_approved").asText(), receipt.id());
      if (approved == null)
        return Optional.of(blocked("Compra aprovada sem data financeira válida."));
      if (approved.isBefore(start) || approved.isAfter(end)) continue;
      JsonNode amount = payload.path("transaction_amount");
      if (!amount.isNumber()
          || amount.decimalValue().compareTo(context.price()) != 0
          || !"BRL".equals(payload.path("currency_id").asText()))
        return Optional.of(
            blocked(
                "Valor ou moeda do checkout diverge da oferta; concilie descontos ou testes antes de contar a venda."));
      purchases++;
      gross = gross.add(amount.decimalValue());
      if (receipt.delivered()) deliveries++;
    }
    return Optional.of(
        new Payments(
            0, purchases, 0, gross, BigDecimal.ZERO.setScale(2), deliveries, null, SOURCE));
  }

  /** Identifica o provedor explícito, sem aceitar host parecido ou protocolo inseguro. */
  private boolean mercadoPago(String checkout) {
    try {
      URI uri = URI.create(checkout);
      return "https".equals(uri.getScheme())
          && "www.mercadopago.com.br".equals(uri.getHost())
          && uri.getUserInfo() == null;
    } catch (IllegalArgumentException ex) {
      log.warn("Ciclos: URL de checkout inválida na conciliação", ex);
      return false;
    }
  }

  /** Recupera os aliases do provedor sem aceitar identidades conflitantes. */
  private String metadata(JsonNode payload, String camel, String snake) {
    JsonNode metadata = payload.path("metadata");
    String first = metadata.path(camel).isNull() ? "" : metadata.path(camel).asText("");
    String second = metadata.path(snake).isNull() ? "" : metadata.path(snake).asText("");
    if (!first.isBlank() && !second.isBlank() && !first.equals(second)) return "CONFLICT";
    return first.isBlank() ? second : first;
  }

  /** Exclui marcadores explícitos de QA sem presumir que pagamento nominal seja venda real. */
  private boolean testReceipt(JsonNode payload) {
    JsonNode metadata = payload.path("metadata");
    for (String key : List.of("mh_test", "is_test", "test_mode")) {
      String value = metadata.path(key).asText("");
      if ("1".equals(value) || "true".equalsIgnoreCase(value)) return true;
    }
    return metadata.hasNonNull("mh_audit") || metadata.toString().contains("__mh_internal_test__");
  }

  /** Lê JSON sem expor conteúdo financeiro ou pessoal em logs de diagnóstico. */
  private JsonNode parse(String value, String source, String id) {
    if (value == null || value.isBlank()) return json.nullNode();
    try {
      JsonNode node =
          json.reader().without(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION).readTree(value);
      return node == null ? json.nullNode() : node;
    } catch (JsonProcessingException ex) {
      log.warn("Ciclos: JSON de {} inválido na conciliação id={}", source, id, ex);
      return json.nullNode();
    }
  }

  /** Converte a data do provedor com seu offset explícito, sem depender do fuso do host. */
  private Instant date(String value, String id) {
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException ex) {
      log.warn("Ciclos: data financeira inválida no recibo id={}", id, ex);
      return null;
    }
  }

  /** Preserva fonte e motivo sem transformar evidência inválida em receita zero. */
  private Payments blocked(String reason) {
    return new Payments(0, 0, 0, null, null, 0, reason, SOURCE);
  }

  /** Mantém somente a identidade necessária para atribuir a compra à oferta. */
  private record Context(Long productId, String checkout, BigDecimal price, String definition) {}

  /** Transporta a auditoria internamente; nenhum payload pessoal compõe o relatório do ciclo. */
  private record Receipt(
      String id, String status, String processing, String payload, boolean delivered) {}
}
