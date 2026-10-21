package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

/** Confirma identidade e horizonte operacional oficial da credencial devolvida sem duração. */
public final class FacebookTokenValidityVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookTokenValidityVerifier.class);
    private final WebClient graph;
    private final String debugPath;

    /** Reutiliza o transporte e a versão oficiais da integração Meta. */
    public FacebookTokenValidityVerifier(WebClient graph, String debugPath) {
        this.graph = graph;
        this.debugPath = debugPath;
    }

    /** Admite somente o menor limite positivo oficial, mantendo aplicativo, titular e permissões. */
    public LocalDateTime verify(String app, String secret, String current, String candidate) {
        JsonNode next = inspect(app, secret, candidate);
        require(next.path("is_valid").isBoolean() && next.path("is_valid").booleanValue(), "credencial inválida");
        require(app.equals(next.path("app_id").asText()) && "USER".equals(next.path("type").asText()), "aplicativo ou tipo divergente");
        long now = Instant.now().getEpochSecond();
        long expires = nonnegative(next, "expires_at");
        long dataExpires = next.has("data_access_expires_at") ? nonnegative(next, "data_access_expires_at") : 0;
        require(expires == 0 || expires > now, "credencial expirada");
        require(dataExpires == 0 || dataExpires > now, "acesso aos dados expirado");
        long horizon = expires == 0 ? dataExpires : dataExpires == 0 ? expires : Math.min(expires, dataExpires);
        require(horizon > now, "horizonte positivo ausente");
        Set<String> scopes = new HashSet<>();
        next.path("scopes").forEach(scope -> scopes.add(scope.asText()));
        require(scopes.containsAll(Set.of("ads_read", "ads_management")), "permissões insuficientes");
        JsonNode previous = inspect(app, secret, current);
        require(previous.path("is_valid").isBoolean() && previous.path("is_valid").booleanValue()
                && app.equals(previous.path("app_id").asText()) && "USER".equals(previous.path("type").asText())
                && !previous.path("user_id").asText().isBlank()
                && previous.path("user_id").asText().equals(next.path("user_id").asText()), "titular ou origem divergente");
        LOG.info("Validade operacional Meta confirmada por debug_token: appId={}, horizonteUtc={}", app, Instant.ofEpochSecond(horizon));
        return LocalDateTime.ofEpochSecond(horizon, 0, ZoneOffset.UTC);
    }

    /** Consulta metadados sem registrar URL com token nem transportar segredo no diagnóstico. */
    private JsonNode inspect(String app, String secret, String token) {
        try {
            JsonNode response = graph.get()
                    .uri(builder -> builder.path(debugPath).queryParam("input_token", token).build())
                    .headers(headers -> headers.setBearerAuth(app + "|" + secret))
                    .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(20));
            require(response != null && response.path("data").isObject() && !response.has("error"), "metadados ausentes");
            return response.path("data");
        } catch (RuntimeException ex) {
            IllegalStateException safe = new IllegalStateException("Falha ao inspecionar validade Meta: " + ex.getClass().getSimpleName());
            safe.setStackTrace(ex.getStackTrace());
            LOG.error("Renovação Meta sem validade confirmada: appId={}, operação=debug_token", app, safe);
            throw safe;
        }
    }

    /** Distingue zero explícito de ausência, tipos indevidos e datas negativas. */
    private long nonnegative(JsonNode data, String field) {
        JsonNode value = data.path(field);
        require(value.isIntegralNumber() && value.canConvertToLong() && value.longValue() >= 0, "prazo oficial inválido: " + field);
        return value.longValue();
    }

    /** Bloqueia a troca sem pressupor prazo ou registrar a credencial candidata. */
    private void require(boolean valid, String cause) {
        if (!valid) throw new IllegalStateException("Meta não comprovou renovação: " + cause);
    }
}
