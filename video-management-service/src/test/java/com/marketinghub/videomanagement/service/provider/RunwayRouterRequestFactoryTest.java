package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger os dois perfis reutilizáveis de roteamento audiovisual. */
class RunwayRouterRequestFactoryTest {

    /** Divide o lote em clipes e usa a configuração econômica de rascunho. */
    @Test
    void shouldBuildDraftInstagramBatchWithStableVisualRules() {
        VideoManagementProperties properties = new VideoManagementProperties();
        RunwayRouterRequestFactory factory = new RunwayRouterRequestFactory(properties);

        List<Map<String, Object>> result = factory.build(job("DRAFT_INSTAGRAM", 24, 10, 3));

        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(request -> {
            assertThat(request.get("configId"))
                    .isEqualTo("marketing-hub-instagram-draft-v1");
            assertThat(request).doesNotContainKey("dryRun");
            assertThat(((Map<?, ?>) request.get("input")).get("negativePrompt").toString())
                    .contains("flicker", "camera shake", "embedded text");
        });
        assertThat(((Map<?, ?>) result.get(0).get("input")).get("duration")).isEqualTo(10);
        assertThat(((Map<?, ?>) result.get(1).get("input")).get("duration")).isEqualTo(10);
        assertThat(((Map<?, ?>) result.get(2).get("input")).get("duration")).isEqualTo(4);
    }

    /** Usa a configuração de qualidade final sem acoplar o fluxo ao experimento Vega. */
    @Test
    void shouldUseFinalCampaignProfileForAnyVideoProject() {
        VideoManagementProperties properties = new VideoManagementProperties();

        List<Map<String, Object>> result = new RunwayRouterRequestFactory(properties)
                .build(job("FINAL_CAMPAIGN", 10, 10, 1));

        assertThat(result).singleElement().satisfies(request ->
                assertThat(request.get("configId"))
                        .isEqualTo("marketing-hub-campaign-final-v1"));
    }

    /** Cria contexto mínimo e independente de produto para o factory. */
    private ProviderPreflightJob job(String profile, int duration, int clipDuration, int clips) {
        return new ProviderPreflightJob(
                31L, 11L, "Runway", "RUNWAY_PRIMARY", profile, new BigDecimal("500"),
                duration, clipDuration, clips, "9:16", "720p", false,
                "Projeto reutilizável", "Validar comunicação", "Gancho", "Roteiro",
                "Dor\nMecanismo\nResultado", "Personagem", "Ambiente", "Estilo", "Continuidade",
                "Aprender", "Melhorar retenção");
    }
}
