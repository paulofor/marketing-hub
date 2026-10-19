package com.marketinghub.worker.openai.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Redireciona somente Responses do provedor configurado ao App Server dentro do próprio worker. */
public final class CodexResponsesFilter implements ExchangeFilterFunction {
  private static final Logger log = LoggerFactory.getLogger(CodexResponsesFilter.class);
  private final URI endpoint;
  private final CodexAppServerTransport transport;
  private final ObjectMapper mapper;

  /** Fixa o endpoint permitido sem alterar chamadas aos contratos do backend ou de imagem. */
  public CodexResponsesFilter(
      String baseUrl, CodexAppServerTransport transport, ObjectMapper mapper) {
    this.endpoint = URI.create(baseUrl.replaceAll("/$", "") + "/responses");
    this.transport = transport;
    this.mapper = mapper;
  }

  /** Captura o payload textual e usa o transporte Codex sem abrir conexão à Responses API. */
  @Override
  public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
    boolean providerEndpoint =
        endpoint.equals(request.url())
            || URI.create("https://api.openai.com/v1/responses").equals(request.url());
    if (!providerEndpoint || request.method() != HttpMethod.POST) return next.exchange(request);
    CodexRequestBuffer buffer = new CodexRequestBuffer(request.url(), request.method());
    BodyInserter.Context context =
        new BodyInserter.Context() {
          /** Reutiliza os codificadores JSON padrão do WebClient. */
          @Override
          public List<HttpMessageWriter<?>> messageWriters() {
            return ExchangeStrategies.withDefaults().messageWriters();
          }

          /** Indica que a serialização pertence ao cliente interno. */
          @Override
          public Optional<ServerHttpRequest> serverRequest() {
            return Optional.empty();
          }

          /** Não acrescenta dicas que alterem o contrato original. */
          @Override
          public Map<String, Object> hints() {
            return Map.of();
          }
        };
    return request
        .body()
        .insert(buffer, context)
        .then(
            Mono.defer(
                () -> {
                  try {
                    var body = mapper.readTree(buffer.body());
                    if (body.path("tools").isArray()) {
                      for (var tool : body.path("tools")) {
                        // A geração de imagens continua sendo uma integração especializada
                        // explícita.
                        if ("image_generation".equals(tool.path("type").asText()))
                          return next.exchange(request);
                      }
                    }
                    return Mono.fromCallable(
                            () ->
                                ClientResponse.create(HttpStatus.OK)
                                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                    .header("X-MarketingHub-Transport", "codex-app-server")
                                    .body(transport.execute(buffer.body()))
                                    .build())
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(this::blocked);
                  } catch (Exception ex) {
                    log.error(
                        "Falha ao preparar request Codex; operation=responses; endpoint={}",
                        request.url(),
                        ex);
                    return blocked(ex);
                  }
                }));
  }

  /** Converte somente falhas do transporte Codex, preservando erros das integrações de imagem. */
  private Mono<ClientResponse> blocked(Throwable ex) {
    log.error("Transporte Codex bloqueou request; operation=responses", ex);
    try {
      return Mono.just(
          ClientResponse.create(HttpStatus.UNPROCESSABLE_ENTITY)
              .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .body(
                  mapper.writeValueAsString(
                      Map.of(
                          "error",
                          Map.of(
                              "code",
                              "codex_execution_blocked",
                              "message",
                              String.valueOf(ex.getMessage())))))
              .build());
    } catch (Exception serializationError) {
      log.error("Falha ao serializar erro Codex; operation=responses", serializationError);
      return Mono.error(serializationError);
    }
  }
}
