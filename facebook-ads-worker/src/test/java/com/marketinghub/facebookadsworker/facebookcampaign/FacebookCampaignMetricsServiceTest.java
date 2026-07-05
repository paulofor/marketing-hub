package com.marketinghub.facebookadsworker.facebookcampaign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAccessTokenManager;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Testa o mapeamento das métricas de campanha coletadas na Meta. */
class FacebookCampaignMetricsServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Garante que a consulta de insights pede o campo de alcance da Meta. */
    @Test
    void buildInsightsQueryIncludesReach() throws Exception {
        FacebookCampaignMetricsService service = service();

        Method method = FacebookCampaignMetricsService.class.getDeclaredMethod("buildInsightsQuery");
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> query = (Map<String, String>) method.invoke(service);

        assertThat(query.get("fields")).contains("reach");
    }

    /** Garante que o alcance retornado pela Meta entra no payload enviado ao backend. */
    @Test
    void mapToPayloadCopiesReachFromInsights() throws Exception {
        FacebookCampaignMetricsService service = service();
        JsonNode row = objectMapper.readTree("""
                {
                  "date_start": "2026-06-12",
                  "date_stop": "2026-06-12",
                  "reach": "1300",
                  "impressions": "1500",
                  "clicks": "120",
                  "spend": "25.00",
                  "actions": [{"action_type": "lead", "value": "6"}]
                }
                """);

        Method method = FacebookCampaignMetricsService.class.getDeclaredMethod("mapToPayload", JsonNode.class);
        method.setAccessible(true);
        FacebookCampaignMetricsService.CampaignMetricsUpdateRequest payload =
                (FacebookCampaignMetricsService.CampaignMetricsUpdateRequest) method.invoke(service, row);

        assertThat(payload.dateStart()).isEqualTo(LocalDate.parse("2026-06-12"));
        assertThat(payload.dateStop()).isEqualTo(LocalDate.parse("2026-06-12"));
        assertThat(payload.reach()).isEqualTo(1300L);
        assertThat(payload.impressions()).isEqualTo(1500L);
        assertThat(payload.clicks()).isEqualTo(120L);
        assertThat(payload.leads()).isEqualTo(6L);
        assertThat(payload.spend()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    /** Garante que o retrato de status da Meta inclui campanha, ad set e anúncios. */
    @Test
    void mapStatusSnapshotCopiesCampaignChildrenStatus() throws Exception {
        FacebookCampaignMetricsService service = service();
        JsonNode row = objectMapper.readTree("""
                {
                  "id": "cmp-1",
                  "status": "ACTIVE",
                  "effective_status": "ACTIVE",
                  "adsets": {
                    "data": [
                      {
                        "id": "adset-1",
                        "status": "ACTIVE",
                        "effective_status": "ACTIVE",
                        "ads": {
                          "data": [
                            {
                              "id": "ad-1",
                              "status": "ACTIVE",
                              "effective_status": "ACTIVE"
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
                """);

        Method method = FacebookCampaignMetricsService.class.getDeclaredMethod("mapStatusSnapshot", JsonNode.class);
        method.setAccessible(true);
        FacebookCampaignMetricsService.CampaignStatusSyncRequest payload =
                (FacebookCampaignMetricsService.CampaignStatusSyncRequest) method.invoke(service, row);

        assertThat(payload.status()).isEqualTo("ACTIVE");
        assertThat(payload.effectiveStatus()).isEqualTo("ACTIVE");
        assertThat(payload.adSets()).hasSize(1);
        assertThat(payload.adSets().get(0).id()).isEqualTo("adset-1");
        assertThat(payload.ads()).hasSize(1);
        assertThat(payload.ads().get(0).id()).isEqualTo("ad-1");
    }

    /** Cria o serviço com dependências simuladas para testar apenas o mapeamento local. */
    private FacebookCampaignMetricsService service() {
        return new FacebookCampaignMetricsService(
                mock(FacebookAdsService.class),
                mock(FacebookAccessTokenManager.class),
                mock(FacebookWorkerConfigurationClient.class),
                mock(FacebookCampaignStatusSnapshotClient.class),
                WebClient.builder(),
                "http://backend.test",
                "/api"
        );
    }
}
