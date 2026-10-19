package com.marketinghub.worker.openai.codex;

import java.net.URI;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Captura o corpo já codificado pelo WebClient sem enviar credenciais ou tráfego à API. */
final class CodexRequestBuffer implements ClientHttpRequest {
  private final URI uri;
  private final HttpMethod method;
  private final HttpHeaders headers = new HttpHeaders();
  private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
  private byte[] body = new byte[0];

  /** Guarda somente endereço e método para executar o BodyInserter original. */
  CodexRequestBuffer(URI uri, HttpMethod method) {
    this.uri = uri;
    this.method = method;
  }

  /** Devolve os bytes do request original, sem headers de autenticação. */
  byte[] body() {
    return body;
  }

  /** Informa a URL do request interceptado. */
  @Override
  public URI getURI() {
    return uri;
  }

  /** Informa o método do request interceptado. */
  @Override
  public HttpMethod getMethod() {
    return method;
  }

  /** Disponibiliza headers usados pelo codificador do corpo. */
  @Override
  public HttpHeaders getHeaders() {
    return headers;
  }

  /** Disponibiliza os cookies requeridos pelo contrato HTTP local. */
  @Override
  public MultiValueMap<String, HttpCookie> getCookies() {
    return cookies;
  }

  /** Usa buffers locais para serialização, sem conexão HTTP. */
  @Override
  public DataBufferFactory bufferFactory() {
    return DefaultDataBufferFactory.sharedInstance;
  }

  /** Recusa acesso a um request nativo que não existe neste transporte. */
  @Override
  public <T> T getNativeRequest() {
    throw new UnsupportedOperationException("Transporte Codex não possui request HTTP nativo");
  }

  /** O corpo é consumido somente após o BodyInserter terminar. */
  @Override
  public boolean isCommitted() {
    return false;
  }

  /** O transporte não permite callbacks de mutação depois da codificação. */
  @Override
  public void beforeCommit(Supplier<? extends Mono<Void>> action) {}

  /** Limita a entrada e libera cada buffer depois de copiar o corpo. */
  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> buffers) {
    return DataBufferUtils.join(buffers, 32 * 1024 * 1024)
        .doOnNext(
            buffer -> {
              try {
                body = new byte[buffer.readableByteCount()];
                buffer.read(body);
              } finally {
                DataBufferUtils.release(buffer);
              }
            })
        .then();
  }

  /** Une partes codificadas sem alterar sua ordem. */
  @Override
  public Mono<Void> writeAndFlushWith(
      Publisher<? extends Publisher<? extends DataBuffer>> buffers) {
    return writeWith(Flux.from(buffers).concatMap(Flux::from));
  }

  /** Conclui requests sem corpo. */
  @Override
  public Mono<Void> setComplete() {
    return Mono.empty();
  }
}
