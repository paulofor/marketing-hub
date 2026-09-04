package com.marketinghub.harnesslibraryapi.client;

import com.marketinghub.harnesslibraryapi.api.CardListResponse;
import com.marketinghub.harnesslibraryapi.api.CardTransitionRequest;
import com.marketinghub.harnesslibraryapi.api.CardVersionResponse;
import com.marketinghub.harnesslibraryapi.api.RegisterCardRequest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/** Encaminha contratos autenticados ao backend sem manter estado ou acessar banco. */
@Component
public class HarnessBackendClient {
  private static final Logger log = LoggerFactory.getLogger(HarnessBackendClient.class);
  private static final String CARDS_PATH = "/api/internal/research-intelligence/v1/cards";

  private final RestClient restClient;
  private final InternalRequestSigner signer;

  /** Recebe o cliente com timeout e o assinador interno já configurados. */
  public HarnessBackendClient(RestClient restClient, InternalRequestSigner signer) {
    this.restClient = restClient;
    this.signer = signer;
  }

  /** Cadastra uma versão e conserva a chave idempotente recebida do cliente público. */
  public CardVersionResponse register(
      RegisterCardRequest request, String actor, String idempotencyKey, String requestId) {
    return post(CARDS_PATH, request, actor, idempotencyKey, requestId);
  }

  /** Lista versões com filtros assinados e aplicados pelo backend. */
  public CardListResponse list(
      String status, String collection, int limit, String actor, String requestId) {
    Map<String, List<String>> parameters = new LinkedHashMap<>();
    if (StringUtils.hasText(status)) {
      parameters.put("status", List.of(status));
    }
    if (StringUtils.hasText(collection)) {
      parameters.put("collection", List.of(collection));
    }
    parameters.put("limit", List.of(Integer.toString(limit)));
    String uri =
        UriComponentsBuilder.fromPath(CARDS_PATH)
            .queryParams(toQueryParameters(parameters))
            .build()
            .encode()
            .toUriString();
    byte[] body = signer.serialize(null);
    Map<String, String> signed =
        signer.sign("GET", CARDS_PATH, parameters, actor, null, requestId, body);
    log.info(
        "Chamando backend da Biblioteca requestId={} method=GET url={} actor={}",
        requestId,
        uri,
        actor);
    CardListResponse response =
        execute(
            requestId,
            "GET",
            uri,
            () ->
                restClient
                    .get()
                    .uri(uri)
                    .headers(headers -> applyHeaders(headers, signed, actor, null))
                    .retrieve()
                    .body(CardListResponse.class));
    log.info(
        "Resposta do backend da Biblioteca requestId={} method=GET url={} returnedItems={}",
        requestId,
        uri,
        response == null ? null : response.returnedItems());
    return response;
  }

  /** Consulta uma versão pelo identificador lógico sem replicar dados no gateway. */
  public CardVersionResponse get(String cardKey, int version, String actor, String requestId) {
    String path = versionPath(cardKey, version);
    return getVersion(path, actor, requestId);
  }

  /** Solicita revisão humana mantendo a justificativa no backend. */
  public CardVersionResponse submitForReview(
      String cardKey,
      int version,
      CardTransitionRequest request,
      String actor,
      String idempotencyKey,
      String requestId) {
    return post(
        versionPath(cardKey, version) + "/submit-review",
        request,
        actor,
        idempotencyKey,
        requestId);
  }

  /** Solicita ativação atômica da versão revisada. */
  public CardVersionResponse activate(
      String cardKey,
      int version,
      CardTransitionRequest request,
      String actor,
      String idempotencyKey,
      String requestId) {
    return post(
        versionPath(cardKey, version) + "/activate", request, actor, idempotencyKey, requestId);
  }

  /** Solicita arquivamento preservando a trilha editorial. */
  public CardVersionResponse archive(
      String cardKey,
      int version,
      CardTransitionRequest request,
      String actor,
      String idempotencyKey,
      String requestId) {
    return post(
        versionPath(cardKey, version) + "/archive", request, actor, idempotencyKey, requestId);
  }

  /** Executa uma mutação JSON com os mesmos bytes usados no hash e na assinatura. */
  private CardVersionResponse post(
      String path, Object request, String actor, String idempotencyKey, String requestId) {
    byte[] body = signer.serialize(request);
    Map<String, String> signed =
        signer.sign("POST", path, Map.of(), actor, idempotencyKey, requestId, body);
    log.info(
        "Chamando backend da Biblioteca requestId={} method=POST url={} actor={} payload={}",
        requestId,
        path,
        actor,
        new String(body, StandardCharsets.UTF_8));
    CardVersionResponse response =
        execute(
            requestId,
            "POST",
            path,
            () ->
                restClient
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> applyHeaders(headers, signed, actor, idempotencyKey))
                    .body(body)
                    .retrieve()
                    .body(CardVersionResponse.class));
    log.info(
        "Resposta do backend da Biblioteca requestId={} method=POST url={} cardKey={} version={} status={}",
        requestId,
        path,
        response == null ? null : response.cardKey(),
        response == null ? null : response.version(),
        response == null ? null : response.status());
    return response;
  }

  /** Executa a leitura unitária com corpo vazio autenticado. */
  private CardVersionResponse getVersion(String path, String actor, String requestId) {
    byte[] body = signer.serialize(null);
    Map<String, String> signed = signer.sign("GET", path, Map.of(), actor, null, requestId, body);
    log.info(
        "Chamando backend da Biblioteca requestId={} method=GET url={} actor={}",
        requestId,
        path,
        actor);
    CardVersionResponse response =
        execute(
            requestId,
            "GET",
            path,
            () ->
                restClient
                    .get()
                    .uri(path)
                    .headers(headers -> applyHeaders(headers, signed, actor, null))
                    .retrieve()
                    .body(CardVersionResponse.class));
    log.info(
        "Resposta do backend da Biblioteca requestId={} method=GET url={} cardKey={} version={} status={}",
        requestId,
        path,
        response == null ? null : response.cardKey(),
        response == null ? null : response.version(),
        response == null ? null : response.status());
    return response;
  }

  /** Aplica apenas metadados de autenticação, nunca o valor do secret compartilhado. */
  private void applyHeaders(
      HttpHeaders headers, Map<String, String> signed, String actor, String idempotencyKey) {
    signed.forEach(headers::set);
    headers.set("X-Actor", actor);
    if (idempotencyKey != null) {
      headers.set("Idempotency-Key", idempotencyKey);
    }
  }

  /** Converte o mapa simples na coleção exigida pelo construtor de URI. */
  private org.springframework.util.MultiValueMap<String, String> toQueryParameters(
      Map<String, List<String>> parameters) {
    org.springframework.util.LinkedMultiValueMap<String, String> result =
        new org.springframework.util.LinkedMultiValueMap<>();
    parameters.forEach(result::put);
    return result;
  }

  /** Forma um caminho seguro porque cardKey e versão já foram validados no controller. */
  private String versionPath(String cardKey, int version) {
    return CARDS_PATH + "/" + cardKey + "/versions/" + version;
  }

  /** Traduz falhas de transporte e respostas internas sem vazar corpo ou assinatura. */
  private <T> T execute(String requestId, String method, String uri, Supplier<T> call) {
    try {
      T response = call.get();
      if (response == null) {
        throw new BackendApiException(
            HttpStatus.BAD_GATEWAY, "Backend respondeu sem conteúdo.", null);
      }
      return response;
    } catch (RestClientResponseException ex) {
      log.error(
          "Backend da Biblioteca rejeitou chamada requestId={} method={} url={} status={}",
          requestId,
          method,
          uri,
          ex.getStatusCode().value(),
          ex);
      throw translateResponse(ex);
    } catch (ResourceAccessException ex) {
      log.error(
          "Timeout ou conexão falhou no backend da Biblioteca requestId={} method={} url={}",
          requestId,
          method,
          uri,
          ex);
      throw new BackendApiException(
          HttpStatus.GATEWAY_TIMEOUT, "Backend da Biblioteca não respondeu no prazo.", ex);
    } catch (RestClientException ex) {
      log.error(
          "Integração com backend da Biblioteca falhou requestId={} method={} url={}",
          requestId,
          method,
          uri,
          ex);
      throw new BackendApiException(
          HttpStatus.BAD_GATEWAY, "Backend da Biblioteca está indisponível.", ex);
    }
  }

  /** Preserva erros funcionais 4xx e converte falhas internas em erro de gateway. */
  private BackendApiException translateResponse(RestClientResponseException ex) {
    int status = ex.getStatusCode().value();
    if (status == 400) {
      return new BackendApiException(
          HttpStatus.BAD_REQUEST, "Backend rejeitou os dados do cartão.", ex);
    }
    if (status == 404) {
      return new BackendApiException(HttpStatus.NOT_FOUND, "Cartão ou versão não encontrado.", ex);
    }
    if (status == 409) {
      return new BackendApiException(
          HttpStatus.CONFLICT, "Operação conflita com o estado editorial atual.", ex);
    }
    return new BackendApiException(
        HttpStatus.BAD_GATEWAY, "Backend canônico recusou a integração.", ex);
  }
}
