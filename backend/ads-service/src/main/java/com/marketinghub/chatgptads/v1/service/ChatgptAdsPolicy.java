package com.marketinghub.chatgptads.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.chatgptads.v1.service.managecampaign.ChatgptAdsContracts.Draft;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Valida o contrato comercial e constrói somente payloads documentados do ChatGPT Ads. */
public final class ChatgptAdsPolicy {
  /** Impede instâncias de uma política determinística. */
  private ChatgptAdsPolicy() {}

  /** Valida um rascunho sem assumir orçamento, datas ou moeda. */
  public static List<String> validate(Draft d, Instant now) {
    var issues = new ArrayList<String>();
    if (d.experimentId() == null || d.creativeId() == null)
      issues.add("Escolha o experimento e o criativo revisado.");
    if (d.name() == null || d.name().trim().length() < 3 || d.name().length() > 200)
      issues.add("Informe um nome de 3 a 200 caracteres.");
    if (d.title() == null || d.title().length() < 3 || d.title().length() > 50)
      issues.add("Título deve ter de 3 a 50 caracteres.");
    if (d.body() == null || d.body().isBlank() || d.body().length() > 100)
      issues.add("Mensagem deve ter de 1 a 100 caracteres.");
    if (!publicHttps(d.imageUrl()) || !publicHttps(d.targetUrl()))
      issues.add("Imagem e destino devem usar URLs públicas HTTPS.");
    if (d.contextHints() == null
        || d.contextHints().isEmpty()
        || d.contextHints().size() > 10
        || d.contextHints().stream().anyMatch(s -> s == null || s.isBlank() || s.length() > 300))
      issues.add("Informe de uma a dez situações de busca, até 300 caracteres cada.");
    if (d.currency() == null || !d.currency().matches("[A-Z]{3}"))
      issues.add("Informe a moeda da conta, como BRL ou USD.");
    if (d.totalBudget() == null
        || d.totalBudget().compareTo(BigDecimal.ONE) < 0
        || d.totalBudget().scale() > 2
        || d.totalBudget().compareTo(new BigDecimal("1000000")) > 0)
      issues.add("Informe teto total positivo, com até duas casas decimais.");
    if (d.maxCpc() == null
        || d.maxCpc().signum() <= 0
        || d.maxCpc().scale() > 2
        || d.totalBudget() == null
        || d.maxCpc().compareTo(d.totalBudget()) > 0)
      issues.add("O lance por clique deve ser positivo e não ultrapassar o teto total.");
    if (d.startsAt() == null
        || d.endsAt() == null
        || !d.endsAt().isAfter(d.startsAt())
        || !d.endsAt().isAfter(now)
        || d.endsAt().isAfter(now.plusSeconds(366L * 86400)))
      issues.add("Informe uma janela futura válida de até um ano.");
    return issues;
  }

  /** Recusa destinos locais e esquemas inseguros sem baixar conteúdo no backend. */
  public static boolean publicHttps(String value) {
    if (value == null || value.length() > 2048) return false;
    URI uri;
    try {
      uri = URI.create(value);
    } catch (IllegalArgumentException invalid) {
      return false;
    }
    String host = uri.getHost();
    return "https".equals(uri.getScheme())
        && uri.getUserInfo() == null
        && host != null
        && host.contains(".")
        && !host.matches("[0-9.]+")
        && !host.contains(":")
        && !host.endsWith(".local")
        && !host.endsWith(".localhost")
        && uri.getFragment() == null;
  }

  /** Fixa a identidade integral de material, orçamento e janela. */
  public static String fingerprint(ObjectMapper mapper, Draft draft) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(mapper.valueToTree(draft).toString().getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      throw new AssertionError("SHA-256 indisponível", ex);
    }
  }

  /** Converte moeda em micros inteiros, sem arredondamento silencioso. */
  public static long micros(BigDecimal value) {
    return value.movePointRight(6).longValueExact();
  }

  /** Constrói uma campanha pausada com teto vitalício e janela nativos. */
  public static Map<String, Object> campaign(Draft d) {
    return Map.of(
        "name",
        d.name(),
        "status",
        "paused",
        "bidding_type",
        "clicks",
        "budget",
        Map.of("lifetime_spend_limit_micros", micros(d.totalBudget())),
        "start_time",
        d.startsAt().getEpochSecond(),
        "end_time",
        d.endsAt().getEpochSecond(),
        "targeting",
        Map.of("locations", Map.of("countries", List.of("BR"))),
        "landing_page_configuration",
        Map.of(
            "query_string_template",
            "utm_source=openai&utm_medium=paid&utm_campaign={campaign_id}&utm_content={ad_id}&oppref={oppref}"));
  }

  /** Cria o conjunto pausado com contexto e lance fixo. */
  public static Map<String, Object> group(Draft d, String campaignId) {
    return Map.of(
        "campaign_id",
        campaignId,
        "name",
        d.name(),
        "status",
        "paused",
        "context_hints",
        d.contextHints(),
        "bidding_config",
        Map.of(
            "billing_event_type",
            "click",
            "strategy",
            "fixed_bid",
            "max_bid_micros",
            micros(d.maxCpc())));
  }

  /** Publica somente a imagem e a copy do snapshot revisado. */
  public static Map<String, Object> ad(Draft d, String groupId, String fileId) {
    return Map.of(
        "ad_group_id",
        groupId,
        "name",
        d.name(),
        "status",
        "paused",
        "creative",
        Map.of(
            "type",
            "chat_card",
            "title",
            d.title(),
            "body",
            d.body(),
            "target_url",
            d.targetUrl(),
            "file_id",
            fileId));
  }

  /** Confere IDs externos antes de incorporá-los a caminhos HTTP. */
  public static String id(JsonNode response, String field) {
    String id = response.path(field).asText();
    if (!id.matches("[A-Za-z0-9_-]{3,200}"))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Resposta do canal sem identificador válido.");
    return id;
  }

  /** Confere a trava efetiva devolvida pelo provedor. */
  public static boolean budgetMatches(Draft d, JsonNode response) {
    return response.path("budget").path("lifetime_spend_limit_micros").canConvertToLong()
        && response.path("budget").path("lifetime_spend_limit_micros").longValue()
            == micros(d.totalBudget())
        && response.path("start_time").asLong() == d.startsAt().getEpochSecond()
        && response.path("end_time").asLong() == d.endsAt().getEpochSecond();
  }
}
