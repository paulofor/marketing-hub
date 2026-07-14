package com.marketinghub.fashionchat.service;

import com.marketinghub.fashionchat.service.message.FashionChatMessageRequest;
import com.marketinghub.fashionchat.service.message.FashionChatMessageResponse;
import java.time.Duration;
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

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/** Encaminha mensagens da tela para o serviço executor do Chat Moda. */
@Service
public class FashionChatMessageService {
    private static final Logger log = LoggerFactory.getLogger(FashionChatMessageService.class);

    private final RestTemplate restTemplate;
    private final String serviceBaseUrl;

    /** Inicializa o serviço com cliente HTTP e URL base do executor do Chat Moda. */
    @Autowired
    public FashionChatMessageService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${integrations.fashion-chat.base-url:http://191.252.210.83:8094}") String serviceBaseUrl,
            @Value("${integrations.fashion-chat.connect-timeout:PT2S}") Duration connectTimeout,
            @Value("${integrations.fashion-chat.read-timeout:PT180S}") Duration readTimeout) {
        this(restTemplateBuilder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build(), serviceBaseUrl);
    }

    /** Permite montar o serviço em testes com cliente HTTP controlado. */
    FashionChatMessageService(RestTemplate restTemplate, String serviceBaseUrl) {
        this.restTemplate = restTemplate;
        this.serviceBaseUrl = normalizeBaseUrl(serviceBaseUrl);
    }

    /** Envia a pergunta ao executor e retorna a resposta funcional preservada. */
    public FashionChatMessageResponse answer(FashionChatMessageRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "message e obrigatorio");
        }
        String url = buildUrl("/api/fashion-chat/messages");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        FashionChatMessageRequest payload = new FashionChatMessageRequest(
                blankToNull(request.customerId()),
                request.message().trim());
        try {
            ResponseEntity<FashionChatMessageResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    FashionChatMessageResponse.class);
            return response.getBody();
        } catch (RestClientResponseException ex) {
            log.error("Falha HTTP ao enviar mensagem ao Chat Moda no endpoint {}", url, ex);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Chat Moda recusou a mensagem: " + ex.getRawStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("Erro ao enviar mensagem ao Chat Moda no endpoint {}", url, ex);
            throw new ResponseStatusException(BAD_GATEWAY, "Erro ao conectar no serviço Chat Moda", ex);
        }
    }

    /** Monta a URL completa sem duplicar barras entre base e caminho. */
    private String buildUrl(String path) {
        return UriComponentsBuilder.fromHttpUrl(serviceBaseUrl).path(path).build().toUriString();
    }

    /** Normaliza a URL base configurada para chamadas ao executor. */
    private String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "http://191.252.210.83:8094" : value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    /** Converte texto em branco para nulo antes de encaminhar ao executor. */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
