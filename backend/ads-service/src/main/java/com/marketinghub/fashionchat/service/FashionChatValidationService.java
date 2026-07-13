package com.marketinghub.fashionchat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.fashionchat.service.login.StartFashionChatLoginResponse;
import com.marketinghub.fashionchat.service.status.FashionChatValidationStatusResponse;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.web.util.UriComponentsBuilder;

/** Orquestra a leitura de saúde e o início de autenticação do serviço Chat Moda. */
@Service
public class FashionChatValidationService {
    private static final Logger log = LoggerFactory.getLogger(FashionChatValidationService.class);
    private static final String AUTHENTICATED = "AUTHENTICATED";
    private static final String NOT_AUTHENTICATED = "NOT_AUTHENTICATED";
    private static final String UNKNOWN = "UNKNOWN";
    private static final String UNAVAILABLE = "UNAVAILABLE";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String serviceBaseUrl;

    /** Inicializa o serviço com timeouts e URL base do Chat Moda. */
    @Autowired
    public FashionChatValidationService(RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${integrations.fashion-chat.base-url:http://191.252.210.83:8094}") String serviceBaseUrl,
            @Value("${integrations.fashion-chat.connect-timeout:PT2S}") Duration connectTimeout,
            @Value("${integrations.fashion-chat.read-timeout:PT10S}") Duration readTimeout) {
        this(restTemplateBuilder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build(), objectMapper, serviceBaseUrl);
    }

    /** Permite montar o serviço em testes com cliente HTTP controlado. */
    FashionChatValidationService(RestTemplate restTemplate, ObjectMapper objectMapper, String serviceBaseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.serviceBaseUrl = normalizeBaseUrl(serviceBaseUrl);
    }

    /** Consulta o healthcheck de prontidão e a conta autenticada no serviço Chat Moda. */
    public FashionChatValidationStatusResponse status() {
        Instant checkedAt = Instant.now();
        ProbeResult ready = getJson("/health/ready");
        ProbeResult account = getJson("/codex-app-server/account/read");
        String accountStatus = resolveAccountStatus(account);
        Boolean authenticated = resolveAuthenticated(account);
        return new FashionChatValidationStatusResponse(
                serviceBaseUrl,
                checkedAt,
                ready.success(),
                ready.httpStatus(),
                ready.errorMessage(),
                accountStatus,
                authenticated,
                account.httpStatus(),
                account.errorMessage(),
                account.body());
    }

    /** Solicita ao serviço Chat Moda o início do login ChatGPT por device code. */
    public StartFashionChatLoginResponse startLogin() {
        String path = "/codex-app-server/account/login/start";
        String url = buildUrl(path);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        StartLoginRequest request = new StartLoginRequest("chatgptDeviceCode");
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    JsonNode.class);
            JsonNode body = response.getBody();
            return new StartFashionChatLoginResponse(
                    serviceBaseUrl,
                    response.getStatusCode().value(),
                    text(body, "verification_uri", "verificationUri", "verificationUrl"),
                    text(body, "user_code", "userCode"),
                    integer(body, "expires_in", "expiresIn"),
                    integer(body, "interval"),
                    body,
                    null);
        } catch (RestClientResponseException ex) {
            log.error("Falha HTTP ao iniciar login do Chat Moda no endpoint {}", url, ex);
            JsonNode body = parseJson(ex.getResponseBodyAsString());
            return new StartFashionChatLoginResponse(
                    serviceBaseUrl,
                    ex.getRawStatusCode(),
                    text(body, "verification_uri", "verificationUri", "verificationUrl"),
                    text(body, "user_code", "userCode"),
                    integer(body, "expires_in", "expiresIn"),
                    integer(body, "interval"),
                    body,
                    "Falha HTTP ao iniciar login: " + ex.getStatusText());
        } catch (RestClientException ex) {
            log.error("Erro ao iniciar login do Chat Moda no endpoint {}", url, ex);
            return new StartFashionChatLoginResponse(
                    serviceBaseUrl,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Erro ao conectar no serviço Chat Moda");
        }
    }

    /** Executa uma leitura GET e preserva o payload retornado pelo serviço externo. */
    private ProbeResult getJson(String path) {
        String url = buildUrl(path);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    JsonNode.class);
            return new ProbeResult(response.getStatusCode().value(), response.getStatusCode().is2xxSuccessful(),
                    response.getBody(), null);
        } catch (RestClientResponseException ex) {
            log.error("Falha HTTP ao consultar validação do Chat Moda no endpoint {}", url, ex);
            return new ProbeResult(ex.getRawStatusCode(), false, parseJson(ex.getResponseBodyAsString()),
                    "Falha HTTP: " + ex.getStatusText());
        } catch (RestClientException ex) {
            log.error("Erro ao consultar validação do Chat Moda no endpoint {}", url, ex);
            return new ProbeResult(null, false, null, "Erro ao conectar no serviço Chat Moda");
        }
    }

    /** Resolve o estado de autenticação a partir do contrato real do account/read. */
    private String resolveAccountStatus(ProbeResult account) {
        Boolean authenticated = resolveAuthenticated(account);
        if (Boolean.TRUE.equals(authenticated)) {
            return AUTHENTICATED;
        }
        String code = text(account.body(), "code", "errorCode", "error");
        if ("CODEX_NOT_AUTHENTICATED".equals(code) || Boolean.FALSE.equals(authenticated)) {
            return NOT_AUTHENTICATED;
        }
        if (!account.success()) {
            return UNAVAILABLE;
        }
        return UNKNOWN;
    }

    /** Lê o booleano de autenticação quando o serviço externo informa esse campo. */
    private Boolean resolveAuthenticated(ProbeResult account) {
        JsonNode body = account.body();
        if (body == null) {
            return null;
        }
        JsonNode authenticated = body.get("authenticated");
        if (authenticated != null && authenticated.isBoolean()) {
            return authenticated.asBoolean();
        }
        JsonNode accountNode = body.get("account");
        if (accountNode != null
                && accountNode.has("authenticated")
                && accountNode.get("authenticated").isBoolean()) {
            return accountNode.get("authenticated").asBoolean();
        }
        return null;
    }

    /** Monta a URL completa sem duplicar barras entre base e caminho. */
    private String buildUrl(String path) {
        return UriComponentsBuilder.fromHttpUrl(serviceBaseUrl).path(path).build().toUriString();
    }

    /** Normaliza a URL base configurada para chamadas e retorno à tela. */
    private String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "http://191.252.210.83:8094" : value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    /** Faz parsing defensivo de payloads JSON retornados em erro HTTP. */
    private JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            log.error("Erro ao interpretar payload JSON do Chat Moda", ex);
            return null;
        }
    }

    /** Retorna o primeiro campo textual existente no payload informado. */
    private String text(JsonNode body, String... names) {
        if (body == null) {
            return null;
        }
        for (String name : names) {
            JsonNode node = body.get(name);
            if (node != null && !node.isNull()) {
                return node.asText();
            }
        }
        return null;
    }

    /** Retorna o primeiro campo numérico inteiro existente no payload informado. */
    private Integer integer(JsonNode body, String... names) {
        if (body == null) {
            return null;
        }
        for (String name : names) {
            JsonNode node = body.get(name);
            if (node != null && node.canConvertToInt()) {
                return node.asInt();
            }
        }
        return null;
    }

    /** Representa a requisição enviada para iniciar o login por device code. */
    private record StartLoginRequest(String type) {
    }

    /** Representa o resultado bruto de uma consulta de validação externa. */
    private record ProbeResult(Integer httpStatus, boolean success, JsonNode body, String errorMessage) {
    }
}
