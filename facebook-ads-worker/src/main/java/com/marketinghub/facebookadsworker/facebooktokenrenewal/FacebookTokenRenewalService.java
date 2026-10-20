package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalClient.FacebookTokenRenewalStatus;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import com.marketinghub.facebookadsworker.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Renova as contas elegíveis e informa ao backend a validade e o resultado efetivos. */
@Service
public class FacebookTokenRenewalService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookTokenRenewalService.class);

    private final WebClient backendClient;
    private final FacebookAdsService facebookAdsService;
    private final FacebookTokenRenewalClient tokenRenewalClient;
    private final String backendBaseUrl;
    private final String apiPrefix;

    /** Recebe o cliente do backend e a integração Meta já configurados. */
    public FacebookTokenRenewalService(
        WebClient.Builder builder,
        FacebookAdsService facebookAdsService,
        FacebookTokenRenewalClient tokenRenewalClient,
        @Value("${backend.base-url:http://localhost:8000}") String backendBaseUrl,
        @Value("${backend.api-prefix:/api}") String apiPrefix
    ) {
        this.backendClient = builder.build();
        this.facebookAdsService = facebookAdsService;
        this.tokenRenewalClient = tokenRenewalClient;
        this.backendBaseUrl = backendBaseUrl;
        this.apiPrefix = apiPrefix;
    }

    /** Consulta e renova as contas elegíveis sem interferir no token de campanhas em curso. */
    public void renewTokensIfNeeded() {
        List<FacebookAccountRenewalCandidate> candidates = fetchEligibleAccounts();
        if (candidates.isEmpty()) {
            LOGGER.debug("No Facebook accounts require token renewal");
            return;
        }

        for (FacebookAccountRenewalCandidate candidate : candidates) {
            try {
                renewCandidateToken(candidate);
            } catch (RuntimeException ex) {
                LOGGER.error("Falha na passagem de renovação Meta: accountId={}", candidate.id(), ex);
            }
        }
    }

    /** Busca o contrato de contas autorizadas à renovação automática. */
    private List<FacebookAccountRenewalCandidate> fetchEligibleAccounts() {
        String url = UrlUtils.joinPath(backendBaseUrl, apiPrefix, "/accounts/facebook/renewal/eligible");
        LOGGER.info(
            "Requesting Facebook accounts eligible for token renewal from backend: url==>{}, params={}",
            url,
            JsonLogFormatter.wrap(Collections.emptyMap())
        );
        try {
            List<FacebookAccountRenewalCandidate> response = backendClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(FacebookAccountRenewalCandidate.class)
                .collectList()
                .blockOptional()
                .orElse(Collections.emptyList());
            LOGGER.info(
                "Received eligible Facebook accounts response from backend: url<=={}, response={}",
                url,
                JsonLogFormatter.wrap(response)
            );
            return response;
        } catch (WebClientResponseException | WebClientRequestException ex) {
            LOGGER.error(
                "Failed to fetch Facebook accounts for token renewal: url<=={}, status={}, message={}",
                url,
                ex instanceof WebClientResponseException responseException ? responseException.getRawStatusCode() : "N/A",
                ex.getMessage(),
                ex
            );
            return Collections.emptyList();
        }
    }

    /** Renova uma conta e deixa os consumidores carregarem a nova credencial do backend. */
    private void renewCandidateToken(FacebookAccountRenewalCandidate candidate) {
        LOGGER.info(
            "Attempting token renewal for Facebook account id={}, name={}, expiresAt={}",
            candidate.id(),
            candidate.name(),
            candidate.tokenExpiresAt()
        );

        TokenRenewalAttemptResult attempt = renewTokenForAccount(
            candidate.id(),
            candidate.appId(),
            candidate.appSecret(),
            candidate.accessToken(),
            false
        );

        if (attempt.status() == FacebookTokenRenewalStatus.SUCCESS) {
            LOGGER.info(
                "Facebook token generation succeeded for account id={} via Graph API",
                candidate.id()
            );
        } else {
            LOGGER.error(
                "Token generation failed for Facebook account id={}, name={}: {}",
                candidate.id(),
                candidate.name(),
                attempt.errorMessage()
            );
        }
    }

    /** Persiste o resultado antes de confirmar a renovação ao chamador. */
    public TokenRenewalAttemptResult renewTokenForAccount(
        Long accountId,
        String appId,
        String appSecret,
        String currentToken,
        boolean updateInMemory
    ) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(appId, "appId");
        Objects.requireNonNull(appSecret, "appSecret");
        Objects.requireNonNull(currentToken, "currentToken");

        TokenRenewalAttemptResult attempt = performTokenRenewal(accountId, appId, appSecret, currentToken);

        if (attempt.status() == FacebookTokenRenewalStatus.SUCCESS) {
            tokenRenewalClient.reportSuccess(
                accountId,
                attempt.accessToken(),
                attempt.tokenExpiresAt(),
                attempt.renewedAt(),
                attempt.attemptedAt()
            );
            if (updateInMemory && attempt.accessToken() != null) {
                facebookAdsService.updateAccessToken(attempt.accessToken());
                LOGGER.info(
                    "Updated in-memory Facebook access token after Graph API renewal for account id={}",
                    accountId
                );
            }
        } else {
            tokenRenewalClient.reportFailure(accountId, attempt.attemptedAt(), attempt.errorMessage());
        }

        return attempt;
    }

    /** Executa a troca oficial e rejeita respostas sem validade verificável. */
    private TokenRenewalAttemptResult performTokenRenewal(
        Long accountId,
        String appId,
        String appSecret,
        String currentToken
    ) {
        LocalDateTime attemptedAt = LocalDateTime.now(ZoneOffset.UTC);
        try {
            FacebookAdsService.TokenRenewalResponse response = facebookAdsService.renewLongLivedToken(
                appId,
                appSecret,
                currentToken
            );
            LocalDateTime renewedAt = attemptedAt;
            LocalDateTime expiresAt = calculateExpiration(renewedAt, response.expiresInSeconds());
            return new TokenRenewalAttemptResult(
                FacebookTokenRenewalStatus.SUCCESS,
                response.accessToken(),
                expiresAt,
                renewedAt,
                attemptedAt,
                null
            );
        } catch (Exception ex) {
            LOGGER.error(
                "Failed to renew Facebook token via Graph API for account id={}: {}",
                accountId,
                ex.getMessage(),
                ex
            );
            return new TokenRenewalAttemptResult(
                FacebookTokenRenewalStatus.FAILED,
                null,
                null,
                null,
                attemptedAt,
                ex.getMessage()
            );
        }
    }

    /** Calcula em UTC a validade exata informada pelo provedor, sem fallback inventado. */
    private LocalDateTime calculateExpiration(LocalDateTime renewedAt, long expiresInSeconds) {
        if (expiresInSeconds <= 0) {
            throw new IllegalStateException("Meta não informou prazo de validade positivo; credencial anterior preservada");
        }
        return renewedAt.plusSeconds(expiresInSeconds);
    }

    /** Contrato de conta elegível recebido exclusivamente do backend. */
    public record FacebookAccountRenewalCandidate(
        Long id,
        String name,
        String appId,
        String appSecret,
        String accessToken,
        LocalDateTime tokenExpiresAt
    ) {}

    /** Resultado da troca, com datas oficiais ou falha sem nova credencial. */
    public record TokenRenewalAttemptResult(
        FacebookTokenRenewalStatus status,
        String accessToken,
        LocalDateTime tokenExpiresAt,
        LocalDateTime renewedAt,
        LocalDateTime attemptedAt,
        String errorMessage
    ) {}
}

