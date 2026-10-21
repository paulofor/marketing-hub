package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.reactive.function.client.WebClient;

/** Reproduz a troca sem duração e comprova a passagem HTTP até o callback sem credenciais reais. */
@ExtendWith(OutputCaptureExtension.class)
class FacebookTokenVerifiedLifetimeTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String OLD = "synthetic-original-credential";
    private static final String NEXT = "synthetic-candidate-credential";
    private static final String SECRET = "synthetic-private-secret";

    /** Recupera o caso original e outra conta pelo prazo oficial de dados, sem inferir sessenta dias. */
    @ParameterizedTest
    @ValueSource(strings = {"account-one", "account-twenty-nine"})
    void verifiesAndPersistsOfficialDataWindow(String app, CapturedOutput output) throws Exception {
        try (MockWebServer graph = new MockWebServer(); MockWebServer backend = new MockWebServer()) {
            long horizon = Instant.now().plusSeconds(7200000).getEpochSecond();
            graph.enqueue(json("{\"access_token\":\"" + NEXT + "\",\"token_type\":\"bearer\"}"));
            graph.enqueue(json(debug(app, "profile-" + app, 0, horizon)));
            graph.enqueue(json(debug(app, "profile-" + app, Instant.now().plusSeconds(600).getEpochSecond(), horizon)));
            backend.enqueue(json("{}").setResponseCode(202));
            var client = new FacebookAdsService(WebClient.builder(), graph.url("/").toString(), "v23.0", JSON, null);
            String base = backend.url("/").toString();
            var service = new FacebookTokenRenewalService(WebClient.builder(), client,
                    new FacebookTokenRenewalClient(WebClient.builder(), base, "/api"), base, "/api");
            var result = service.renewTokenForAccount(73L, app, SECRET, OLD, false);
            assertThat(result.status()).isEqualTo(FacebookTokenRenewalClient.FacebookTokenRenewalStatus.SUCCESS);
            assertThat(result.tokenExpiresAt()).isEqualTo(LocalDateTime.ofEpochSecond(horizon, 0, ZoneOffset.UTC));
            var callback = backend.takeRequest(1, TimeUnit.SECONDS);
            assertThat(callback).isNotNull();
            JsonNode body = JSON.readTree(callback.getBody().readUtf8());
            assertThat(body.path("status").asText()).isEqualTo("SUCCESS");
            assertThat(body.path("accessToken").asText()).isEqualTo(NEXT);
            assertThat(graph.getRequestCount()).isEqualTo(3);
            assertThat(client.getCurrentAccessToken()).isNull();
            assertThat(output.getAll()).doesNotContain(OLD, NEXT, SECRET);
        }
    }

    /** Mantém sem novas consultas o caminho de duração positiva já aceito. */
    @Test
    void preservesPositiveDuration() throws Exception {
        try (MockWebServer graph = new MockWebServer()) {
            graph.enqueue(json("{\"access_token\":\"" + NEXT + "\",\"expires_in\":3600}"));
            var client = new FacebookAdsService(WebClient.builder(), graph.url("/").toString(), "v23.0", JSON, null);
            assertThat(client.renewLongLivedToken("app", SECRET, OLD).expiresInSeconds()).isEqualTo(3600);
            assertThat(graph.getRequestCount()).isEqualTo(1);
        }
    }

    /** Rejeita candidatas sem horizonte comprovável, sem escopos ou com identidade divergente. */
    @ParameterizedTest
    @ValueSource(strings = {"missing", "negative", "expired", "invalid", "app", "user", "type", "scopes", "unbounded", "string"})
    void rejectsUnverifiedCandidate(String failure, CapturedOutput output) throws Exception {
        try (MockWebServer graph = new MockWebServer()) {
            long horizon = Instant.now().plusSeconds(7200000).getEpochSecond();
            var data = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(debug("app", "profile", 0, horizon)).path("data");
            switch (failure) {
                case "missing" -> data.remove("expires_at");
                case "negative" -> data.put("expires_at", -1);
                case "expired" -> data.put("data_access_expires_at", 1);
                case "invalid" -> data.put("is_valid", false);
                case "app" -> data.put("app_id", "other-app");
                case "user" -> data.put("user_id", "other-profile");
                case "type" -> data.put("type", "PAGE");
                case "scopes" -> data.putArray("scopes").add("ads_read");
                case "unbounded" -> data.put("data_access_expires_at", 0);
                case "string" -> data.put("expires_at", "0");
                default -> throw new IllegalArgumentException(failure);
            }
            graph.enqueue(json("{\"access_token\":\"" + NEXT + "\"}"));
            graph.enqueue(json(JSON.writeValueAsString(java.util.Map.of("data", data))));
            graph.enqueue(json(debug("app", "profile", Instant.now().plusSeconds(600).getEpochSecond(), horizon)));
            var client = new FacebookAdsService(WebClient.builder(), graph.url("/").toString(), "v23.0", JSON, null);
            assertThatThrownBy(() -> client.renewLongLivedToken("app", SECRET, OLD))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(output.getAll()).doesNotContain(OLD, NEXT, SECRET);
        }
    }

    /** Produz metadados sintéticos com todos os controles de identidade e permissão. */
    private String debug(String app, String user, long expiry, long dataExpiry) {
        return "{\"data\":{\"is_valid\":true,\"app_id\":\"" + app + "\",\"user_id\":\"" + user
                + "\",\"type\":\"USER\",\"expires_at\":" + expiry + ",\"data_access_expires_at\":" + dataExpiry
                + ",\"scopes\":[\"ads_read\",\"ads_management\"]}}";
    }

    /** Devolve somente respostas locais JSON. */
    private MockResponse json(String value) {
        return new MockResponse().addHeader("Content-Type", "application/json").setBody(value);
    }
}
