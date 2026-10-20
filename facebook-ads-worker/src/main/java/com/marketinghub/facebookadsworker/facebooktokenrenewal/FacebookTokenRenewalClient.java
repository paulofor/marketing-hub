package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.Collections;

/** Confirma no backend os resultados de renovação sem expor credenciais nos logs. */
@Component
public class FacebookTokenRenewalClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookTokenRenewalClient.class);

    private final WebClient backendClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    /** Configura o transporte para o callback oficial do backend. */
    public FacebookTokenRenewalClient(
        WebClient.Builder builder,
        @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
        @Value("${backend.api-prefix:/api}") String apiPrefix
    ) {
        this.backendClient = builder.build();
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /** Persiste a nova credencial e exige confirmação antes de declarar sucesso. */
    public void reportSuccess(
        Long accountId,
        String accessToken,
        LocalDateTime tokenExpiresAt,
        LocalDateTime renewedAt,
        LocalDateTime attemptedAt
    ) {
        FacebookTokenRenewalRequest payload = new FacebookTokenRenewalRequest(
            FacebookTokenRenewalStatus.SUCCESS,
            accessToken,
            tokenExpiresAt,
            renewedAt,
            attemptedAt,
            null
        );
        postRenewal(accountId, payload);
    }

    /** Preserva a falha e o horário da tentativa no backend. */
    public void reportFailure(Long accountId, LocalDateTime attemptedAt, String errorMessage) {
        FacebookTokenRenewalRequest payload = new FacebookTokenRenewalRequest(
            FacebookTokenRenewalStatus.FAILED,
            null,
            null,
            null,
            attemptedAt,
            errorMessage
        );
        postRenewal(accountId, payload);
    }

    /** Envia o resultado sem registrar o corpo retornado, que pode conter credenciais. */
    private void postRenewal(Long accountId, FacebookTokenRenewalRequest payload) {
        String path = "/accounts/facebook/" + accountId + "/token/renewal";
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, path);
        LOGGER.info(
            "Reporting Facebook token renewal result to backend: url==>{}, params={}, payload={}",
            url,
            JsonLogFormatter.wrap(Collections.emptyMap()),
            JsonLogFormatter.wrap(payload)
        );
        try {
            backendClient
                .post()
                .uri(url)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
            LOGGER.info(
                "Backend acknowledged Facebook token renewal result: accountId={}, status={}",
                accountId,
                payload.status()
            );
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOGGER.error(
                "Failed to report Facebook token renewal result: url<=={}, status={}, message={}",
                url,
                ex instanceof WebClientResponseException responseException ? responseException.getRawStatusCode() : "N/A",
                ex.getMessage(),
                ex
            );
            throw new IllegalStateException("Não foi possível persistir a renovação da conta " + accountId, ex);
        }
    }

    /** Estados aceitos pelo callback canônico. */
    public enum FacebookTokenRenewalStatus {
        SUCCESS,
        FAILED
    }

    /** Payload interno de renovação, nunca serializado sem máscara para logs. */
    public record FacebookTokenRenewalRequest(
        FacebookTokenRenewalStatus status,
        String accessToken,
        LocalDateTime tokenExpiresAt,
        LocalDateTime renewedAt,
        LocalDateTime attemptedAt,
        String errorMessage
    ) {}
}

