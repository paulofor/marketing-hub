package com.marketinghub.facebookadsworker.facebookcampaign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAccessTokenExpiredException;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.FacebookPermissionException;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.facebookapi.ExperimentFacebookApiLogClient;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolverProperties;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Comprova recuperação da credencial de vídeo por HTTP, sem alterar a conexão compartilhada. */
@Timeout(value = 15, unit = TimeUnit.SECONDS)
class FacebookVideoCredentialRecoveryTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private MockWebServer meta;
    private FacebookAdsService ads;
    private FacebookCampaignService campaigns;
    private ExperimentFacebookApiLogClient audit;

    /** Prepara uma conta fictícia e a API Meta local, sem credenciais ou consumo externo. */
    @BeforeEach
    void prepare() throws Exception {
        meta = new MockWebServer();
        meta.start();
        WebClient.Builder builder = WebClient.builder();
        ads = new FacebookAdsService(builder, meta.url("/").toString(), "v23.0", mapper,
            new TargetingResolverProperties());
        ads.updateAccessToken("current-user");
        audit = mock(ExperimentFacebookApiLogClient.class);
        campaigns = new FacebookCampaignService(ads, mock(FacebookAccessTokenManager.class), builder,
            mock(FacebookWorkerConfigurationClient.class), "http://backend.invalid", "/api", mapper,
            audit, new MetaVideoNormalizer(false, "ffmpeg", Duration.ofSeconds(1)));
    }

    /** Fecha somente o servidor local criado pelo teste. */
    @AfterEach
    void close() throws Exception {
        meta.shutdown();
    }

    /** Reproduz Capella e outra identidade sem exceções por produto ou conta. */
    @ParameterizedTest
    @ValueSource(strings = {"105", "214"})
    void expiredDedicatedCredentialUsesCurrentAccountOnce(String account) throws Exception {
        meta.enqueue(error(190, 463, "The token has expired"));
        officialSuccess();

        assertEquals("video-local", upload(account, "expired-dedicated"));

        assertEquals("expired-dedicated", token(take()));
        assertOfficialRequests(account, "current-user");
        assertEquals(4, meta.getRequestCount());
        assertEquals("current-user", ads.getCurrentAccessToken());
        verify(audit).logPublicationJobStep(eq("job-local"), eq(105L),
            eq("CAMPAIGN_VIDEO_CREDENTIAL_REJECTED"), argThat(info -> info.statusCode() == 400));
    }

    /** Preserva a credencial dedicada válida durante as três fases oficiais. */
    @Test
    void validDedicatedCredentialRemainsPreferred() throws Exception {
        officialSuccess();
        assertEquals("video-local", upload("71", "valid-dedicated"));
        assertOfficialRequests("71", "valid-dedicated");
        assertEquals("current-user", ads.getCurrentAccessToken());
    }

    /** Mantém o caminho antes válido que usa somente a conexão principal. */
    @Test
    void absentDedicatedCredentialUsesCurrentConnection() throws Exception {
        officialSuccess();
        assertEquals("video-local", upload("88", null));
        assertOfficialRequests("88", "current-user");
    }

    /** Impede o upload de desfazer uma renovação concorrente da conexão principal. */
    @Test
    void concurrentRefreshIsPreservedAndUploadKeepsItsOwnCredential() throws Exception {
        meta.setDispatcher(new Dispatcher() {
            /** Simula a renovação entre início e transferência do mesmo vídeo. */
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath().contains("/binary")) return json("{\"success\":true}");
                if (request.getBody().clone().readUtf8().contains("\"start\"")) {
                    ads.updateAccessToken("newer-user-session");
                    return startResponse();
                }
                return json("{\"success\":true}");
            }
        });
        assertEquals("video-local", upload("92", "valid-dedicated"));
        assertOfficialRequests("92", "valid-dedicated");
        assertEquals("newer-user-session", ads.getCurrentAccessToken());
    }

    /** Credenciais iguais não autorizam repetição nem fallback que esconderia a expiração. */
    @Test
    void expiredCurrentCredentialIsNotRetried() throws Exception {
        meta.enqueue(error(190, 463, "The token has expired"));
        assertThrows(FacebookAccessTokenExpiredException.class, () -> upload("89", "current-user"));
        assertEquals(1, meta.getRequestCount());
        assertEquals("current-user", ads.getCurrentAccessToken());
    }

    /** Reprovação de permissão continua bloqueando sem tentar outra identidade. */
    @Test
    void permissionFailureDoesNotSwitchCredentials() throws Exception {
        meta.enqueue(error(200, 1815066, "Permissions error"));
        assertThrows(FacebookPermissionException.class, () -> upload("90", "valid-dedicated"));
        assertEquals(1, meta.getRequestCount());
        assertEquals("current-user", ads.getCurrentAccessToken());
    }

    /** Preserva advideos como alternativa de protocolo, com a mesma credencial por tentativa. */
    @Test
    void unsupportedOfficialFlowKeepsLegacyUploadWorking() throws Exception {
        meta.enqueue(error(100, 0, "Unsupported request"));
        meta.enqueue(json("{\"id\":\"legacy-video\"}"));
        assertEquals("legacy-video", upload("93", "valid-dedicated"));
        assertEquals("valid-dedicated", token(take()));
        RecordedRequest legacy = take();
        assertEquals("/v23.0/act_93/advideos", legacy.getPath());
        assertTrue(legacy.getBody().readUtf8().contains("valid-dedicated"));
        assertEquals("current-user", ads.getCurrentAccessToken());
    }

    /** Duas credenciais expiradas encerram a tentativa sem ciclo de repetição. */
    @Test
    void bothExpiredCredentialsStopAfterOneRecovery() {
        meta.enqueue(error(190, 463, "The token has expired"));
        meta.enqueue(error(190, 463, "The token has expired"));
        assertThrows(FacebookAccessTokenExpiredException.class, () -> upload("94", "expired-dedicated"));
        assertEquals(2, meta.getRequestCount());
    }

    /** Executa a mesma operação chamada pela publicação de vídeos, sem criar campanha real. */
    private String upload(String account, String dedicated) {
        return ReflectionTestUtils.invokeMethod(campaigns, "uploadVideoWithOfficialFlowOrLegacyFallback",
            "job-local", 105L, account, dedicated, new byte[] {1, 2, 3}, "creative.mp4", "video/mp4");
    }

    /** Programa início, transferência e conclusão com retorno oficial válido. */
    private void officialSuccess() {
        meta.enqueue(startResponse());
        meta.enqueue(json("{\"success\":true}"));
        meta.enqueue(json("{\"success\":true}"));
    }

    /** Retorna a URL local do binário como a Meta faria na primeira fase. */
    private MockResponse startResponse() {
        return json("{\"video_id\":\"video-local\",\"upload_url\":\"" + meta.url("/binary") + "\"}");
    }

    /** Confere identidade e credencial estável nas três chamadas do protocolo. */
    private void assertOfficialRequests(String account, String credential) throws Exception {
        RecordedRequest start = take();
        assertEquals("/v23.0/act_" + account + "/video_ads", start.getPath());
        assertEquals(credential, token(start));
        assertEquals("OAuth " + credential, take().getHeader("Authorization"));
        assertEquals(credential, token(take()));
    }

    /** Extrai apenas a credencial fictícia do contrato JSON enviado ao stub. */
    private String token(RecordedRequest request) throws Exception {
        return mapper.readTree(request.getBody().readUtf8()).path("access_token").asText();
    }

    /** Obtém a próxima chamada com limite curto para evitar testes pendurados. */
    private RecordedRequest take() throws Exception {
        RecordedRequest request = meta.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        return request;
    }

    /** Constrói erro oficial determinístico com código e subcódigo. */
    private MockResponse error(int code, int subcode, String message) {
        return json("{\"error\":{\"code\":" + code + ",\"error_subcode\":" + subcode
            + ",\"message\":\"" + message + "\"}}").setResponseCode(400);
    }

    /** Retorna uma resposta JSON do provedor local. */
    private MockResponse json(String value) {
        return new MockResponse().setBody(value).addHeader("Content-Type", "application/json");
    }
}
