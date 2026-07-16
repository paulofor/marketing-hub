package com.marketinghub.pde.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Valida credenciais Google usadas para entrada social na Área MUSA. */
@Service
public class GoogleIdentityService {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdentityService.class);

    private final RestClient restClient;
    private final String clientId;

    /** Recebe cliente HTTP e o client id esperado para validar o token. */
    public GoogleIdentityService(
            RestClient.Builder restClientBuilder,
            @Value("${pde.auth.google.client-id:}") String clientId) {
        this.restClient = restClientBuilder.baseUrl("https://oauth2.googleapis.com").build();
        this.clientId = clientId;
    }

    /** Valida o ID token do Google e retorna o e-mail verificado da cliente. */
    public String verifyEmail(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Login com Google ainda nao configurado para a Area MUSA");
        }
        Map<String, Object> tokenInfo;
        try {
            tokenInfo = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/tokeninfo").queryParam("id_token", idToken).build())
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});
        } catch (RuntimeException ex) {
            log.error("Falha ao validar credencial Google da Area MUSA", ex);
            throw ex;
        }
        if (tokenInfo == null) {
            throw new IllegalArgumentException("Credencial Google invalida");
        }
        String audience = String.valueOf(tokenInfo.getOrDefault("aud", ""));
        String email = String.valueOf(tokenInfo.getOrDefault("email", ""));
        String verified = String.valueOf(tokenInfo.getOrDefault("email_verified", ""));
        if (!clientId.equals(audience) || email.isBlank() || !"true".equalsIgnoreCase(verified)) {
            throw new IllegalArgumentException("Credencial Google nao autorizada para a Area MUSA");
        }
        return email;
    }
}
