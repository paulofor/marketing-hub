package com.marketinghub.fashionchat.service;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import com.marketinghub.fashionchat.service.message.FashionChatMessageRequest;
import com.marketinghub.fashionchat.service.message.FashionChatMessageResponse;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/** Encaminha mensagens da tela para o serviço executor do Chat Moda. */
@Service
public class FashionChatMessageService {
  private static final Logger log = LoggerFactory.getLogger(FashionChatMessageService.class);
  private static final int MAX_ATTEMPTS = 2;

  private final RestTemplate restTemplate;
  private final String serviceBaseUrl;

  /** Inicializa o serviço com cliente HTTP e URL base do executor do Chat Moda. */
  @Autowired
  public FashionChatMessageService(
      RestTemplateBuilder restTemplateBuilder,
      @Value("${integrations.fashion-chat.base-url:http://191.252.210.83:8094}")
          String serviceBaseUrl,
      @Value("${integrations.fashion-chat.connect-timeout:PT2S}") Duration connectTimeout,
      @Value("${integrations.fashion-chat.read-timeout:PT0S}") Duration readTimeout) {
    this(
        restTemplateBuilder.setConnectTimeout(connectTimeout).setReadTimeout(readTimeout).build(),
        serviceBaseUrl);
  }

  /** Permite montar o serviço em testes com cliente HTTP controlado. */
  FashionChatMessageService(RestTemplate restTemplate, String serviceBaseUrl) {
    this.restTemplate = restTemplate;
    this.serviceBaseUrl = normalizeBaseUrl(serviceBaseUrl);
  }

  /** Envia a pergunta ao executor com rastreio e retry curto para falhas transitórias. */
  public FashionChatMessageResponse answer(FashionChatMessageRequest request) {
    if (request == null || request.message() == null || request.message().isBlank()) {
      throw new ResponseStatusException(BAD_REQUEST, "message e obrigatorio");
    }
    String url = buildUrl("/api/fashion-chat/messages");
    String jobId = resolveJobId(request);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Job-Id", jobId);
    headers.set("X-Correlation-Id", jobId);
    FashionChatMessageRequest payload =
        new FashionChatMessageRequest(
            blankToNull(request.customerId()), request.message().trim(), jobId);
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        log.info(
            "Enviando mensagem ao Chat Moda jobId={} attempt={} endpoint={}", jobId, attempt, url);
        ResponseEntity<FashionChatMessageResponse> response =
            restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                FashionChatMessageResponse.class);
        log.info(
            "Mensagem do Chat Moda concluida jobId={} attempt={} status={}",
            jobId,
            attempt,
            response.getStatusCode().value());
        return response.getBody();
      } catch (RestClientResponseException ex) {
        log.error(
            "Falha HTTP ao enviar mensagem ao Chat Moda jobId={} attempt={} endpoint={} status={}",
            jobId,
            attempt,
            url,
            ex.getRawStatusCode(),
            ex);
        if (attempt < MAX_ATTEMPTS && isRetryableStatus(ex)) {
          waitBeforeRetry(jobId, attempt);
          continue;
        }
        throw new ResponseStatusException(
            SERVICE_UNAVAILABLE, "Chat Moda recusou a mensagem: " + ex.getRawStatusCode(), ex);
      } catch (RestClientException ex) {
        log.error(
            "Erro ao enviar mensagem ao Chat Moda jobId={} attempt={} endpoint={}",
            jobId,
            attempt,
            url,
            ex);
        if (attempt < MAX_ATTEMPTS) {
          waitBeforeRetry(jobId, attempt);
          continue;
        }
        throw new ResponseStatusException(BAD_GATEWAY, "Erro ao conectar no serviço Chat Moda", ex);
      }
    }
    throw new ResponseStatusException(BAD_GATEWAY, "Erro ao conectar no serviço Chat Moda");
  }

  /** Monta a URL completa sem duplicar barras entre base e caminho. */
  private String buildUrl(String path) {
    return UriComponentsBuilder.fromHttpUrl(serviceBaseUrl).path(path).build().toUriString();
  }

  /** Normaliza a URL base configurada para chamadas ao executor. */
  private String normalizeBaseUrl(String value) {
    String normalized =
        value == null || value.isBlank() ? "http://191.252.210.83:8094" : value.trim();
    return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
  }

  /** Converte texto em branco para nulo antes de encaminhar ao executor. */
  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** Resolve o identificador operacional da conversa para logs e correlação. */
  private String resolveJobId(FashionChatMessageRequest request) {
    String candidate = request.jobId();
    return candidate == null || candidate.isBlank()
        ? "fashion-chat-" + UUID.randomUUID()
        : candidate.trim();
  }

  /** Indica se o status HTTP pode ser tentado novamente sem mudar o payload. */
  private boolean isRetryableStatus(RestClientResponseException ex) {
    return ex.getRawStatusCode() == 502
        || ex.getRawStatusCode() == 503
        || ex.getRawStatusCode() == 504;
  }

  /** Aplica pausa curta entre tentativas para absorver reinício ou conexão abortada. */
  private void waitBeforeRetry(String jobId, int attempt) {
    try {
      Thread.sleep(200L);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error("Retry do Chat Moda interrompido jobId={} attempt={}", jobId, attempt, ex);
      throw new ResponseStatusException(BAD_GATEWAY, "Retry do Chat Moda interrompido", ex);
    }
  }
}
