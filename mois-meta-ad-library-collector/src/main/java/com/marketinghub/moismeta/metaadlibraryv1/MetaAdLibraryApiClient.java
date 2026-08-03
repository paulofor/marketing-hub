package com.marketinghub.moismeta.metaadlibraryv1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Consulta exclusivamente a API oficial da Meta e preserva cada resposta bruta. */
@Component
@Slf4j
public class MetaAdLibraryApiClient {

  private static final String FIELDS =
      "id,page_id,page_name,ad_creation_time,ad_delivery_start_time,ad_delivery_stop_time,ad_snapshot_url,publisher_platforms,ad_creative_bodies,ad_creative_link_captions,ad_creative_link_descriptions,ad_creative_link_titles";

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
  private final ObjectMapper objectMapper;
  private final String graphBaseUrl;
  private final String accessToken;
  private final ExternalCommercialSignalInspector signalInspector;

  /** Configura o cliente sem expor o token em URL ou log. */
  public MetaAdLibraryApiClient(
      ObjectMapper objectMapper,
      @Value("${marketinghub.meta.graph-base-url}") String graphBaseUrl,
      @Value("${marketinghub.meta.access-token:}") String accessToken,
      ExternalCommercialSignalInspector signalInspector) {
    this.objectMapper = objectMapper;
    this.graphBaseUrl = graphBaseUrl;
    this.accessToken = accessToken;
    this.signalInspector = signalInspector;
  }

  /** Busca anúncios reais ou bloqueia explicitamente quando falta acesso oficial. */
  public List<MetaAdLibraryContracts.Observation> search(
      MetaAdLibraryContracts.PendingInvestigation investigation) {
    if (accessToken == null || accessToken.isBlank()) {
      throw new IllegalStateException("META_AD_LIBRARY_ACCESS_TOKEN não configurado");
    }
    String url = buildUrl(investigation);
    log.info(
        "MOIS Meta request investigationId={} url={} terms={} country={}",
        investigation.id(),
        url,
        investigation.searchTerms(),
        investigation.countryCode());
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(url))
              .timeout(Duration.ofSeconds(60))
              .header("Authorization", "Bearer " + accessToken)
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      log.info(
          "MOIS Meta response investigationId={} status={} payload={}",
          investigation.id(),
          response.statusCode(),
          response.body());
      if (response.statusCode() / 100 != 2) {
        throw new IllegalStateException("Meta Ad Library respondeu HTTP " + response.statusCode());
      }
      return normalize(response.body(), investigation.id());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Falha interrompida na API Meta investigationId={} url={}", investigation.id(), url, ex);
      throw new IllegalStateException("Coleta Meta interrompida", ex);
    } catch (Exception ex) {
      log.error("Falha na API Meta investigationId={} url={}", investigation.id(), url, ex);
      throw new IllegalStateException("Falha na coleta Meta", ex);
    }
  }

  /** Monta a URL oficial sem incluir credenciais. */
  private String buildUrl(MetaAdLibraryContracts.PendingInvestigation investigation) {
    String countries = "[\"" + investigation.countryCode() + "\"]";
    return graphBaseUrl
        + "/ads_archive?ad_active_status=ALL&ad_type=ALL&limit=100&fields="
        + encode(FIELDS)
        + "&search_terms="
        + encode(investigation.searchTerms())
        + "&ad_reached_countries="
        + encode(countries);
  }

  /** Normaliza somente campos comprovados pela resposta oficial. */
  private List<MetaAdLibraryContracts.Observation> normalize(
      String rawResponse, long investigationId) throws Exception {
    JsonNode root = objectMapper.readTree(rawResponse);
    List<MetaAdLibraryContracts.Observation> result = new ArrayList<>();
    for (JsonNode ad : root.path("data")) {
      String destinationUrl = first(ad.path("ad_creative_link_captions"));
      ExternalCommercialSignalInspector.Result externalSignal =
          signalInspector.inspect(destinationUrl, investigationId);
      result.add(
          new MetaAdLibraryContracts.Observation(
              ad.path("id").asText(),
              ad.path("page_id").asText(null),
              ad.path("page_name").asText(null),
              ad.path("ad_delivery_stop_time").isMissingNode() ? "ACTIVE" : "INACTIVE",
              strings(ad.path("publisher_platforms")),
              combinedTexts(ad),
              List.of(),
              destinationUrl,
              ad.path("ad_snapshot_url").asText(null),
              externalSignal.pageActive(),
              externalSignal.commercialSignal(),
              objectMapper.writeValueAsString(ad)));
    }
    return result;
  }

  /** Une textos criativos fornecidos pela API sem inventar sinais. */
  private List<String> combinedTexts(JsonNode ad) {
    List<String> values = new ArrayList<>();
    values.addAll(strings(ad.path("ad_creative_bodies")));
    values.addAll(strings(ad.path("ad_creative_link_titles")));
    values.addAll(strings(ad.path("ad_creative_link_descriptions")));
    return values;
  }

  /** Converte um array JSON textual em lista. */
  private List<String> strings(JsonNode node) {
    List<String> values = new ArrayList<>();
    if (node.isArray()) node.forEach(value -> values.add(value.asText()));
    return values;
  }

  /** Retorna o primeiro item textual de um array. */
  private String first(JsonNode node) {
    return node.isArray() && !node.isEmpty() ? node.get(0).asText(null) : null;
  }

  /** Escapa parâmetros conforme UTF-8. */
  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
