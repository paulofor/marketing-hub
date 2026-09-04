package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger payload, custo e referências da receita Product UGC. */
class RunwayProductUgcRequestFactoryTest {
    private final RunwayProductUgcRequestFactory factory = new RunwayProductUgcRequestFactory();

    /** Monta a versão premium de quinze segundos com teto oficial de 648 créditos. */
    @Test
    void shouldBuildPinnedPremiumRequestWithoutNativeAudio() {
        ProviderPreflightJob job = job(true);

        Map<String, Object> request = factory.build(job).getFirst();
        JsonNode json = new ObjectMapper().valueToTree(request);

        assertThat(factory.supports(job)).isTrue();
        assertThat(factory.estimatedCredits(job)).isEqualByComparingTo("648");
        assertThat(factory.resolution(job)).isEqualTo("1080p");
        assertThat(json.path("version").asText()).isEqualTo("2026-06");
        assertThat(json.path("duration").asInt()).isEqualTo(15);
        assertThat(json.path("ratio").asText()).isEqualTo("1080:1920");
        assertThat(json.path("audio").asBoolean()).isFalse();
        assertThat(json.path("characterImage").path("uri").asText())
                .isEqualTo("https://assets.example/apresentadora.png");
        assertThat(json.path("productImage").path("uri").asText())
                .isEqualTo("https://assets.example/musa-pde.png");
        assertThat(json.path("productInfo").asText())
                .contains("AI-powered digital experience")
                .contains("not a physical object");
        assertThat(json.path("userConcept").asText())
                .contains("locked-tripod", "must not speak", "Never use a mirror");
        assertThat(factory.contractAudit().toString())
                .contains(
                        "APOLLO_RUNWAY_PRODUCT_UGC_V1",
                        "product-info.md",
                        "user-concept.md",
                        "response-schema.json")
                .matches(".*sha256=[0-9a-f]{64}.*");
    }

    /** Aceita somente id inicial e saída HTTPS concluída previstos no schema versionado. */
    @Test
    void shouldValidateAcceptedAndCompletedTaskContracts() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        assertThat(factory.requireAcceptedTaskId(mapper.readTree("{\"id\":\"task-91\"}")))
                .isEqualTo("task-91");
        assertThat(
                        factory.requireCompletedVideoUrl(
                                mapper.readTree(
                                        "{\"id\":\"task-91\",\"status\":\"SUCCEEDED\","
                                                + "\"output\":[\"https://cdn.example/vega.mp4\"]}")))
                .isEqualTo("https://cdn.example/vega.mp4");
        assertThatThrownBy(
                        () ->
                                factory.requireCompletedVideoUrl(
                                        mapper.readTree(
                                                "{\"id\":\"task-91\",\"status\":\"SUCCEEDED\","
                                                        + "\"output\":[\"http://local/vega.mp4\"]}")))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue("code", "PROVIDER_RENDER_FAILED");
    }

    /** Bloqueia a receita antes do preflight quando faltam direitos das referências. */
    @Test
    void shouldRejectMissingRightsBeforeAnyExternalCall() {
        assertThatThrownBy(() -> factory.build(job(false)))
                .isInstanceOf(VideoProviderException.class)
                .hasFieldOrPropertyWithValue("code", "PROVIDER_INPUT_INVALID")
                .hasMessageContaining("direitos");
    }

    /** Cria um job reutilizável sem depender do identificador do experimento Vega. */
    private ProviderPreflightJob job(boolean withRights) {
        return new ProviderPreflightJob(
                31L,
                11L,
                "Runway",
                "RUNWAY_PRIMARY",
                "FINAL_CAMPAIGN",
                "Provider escolhido no Estudio: Runway Product UGC Premium (RUNWAY_PRODUCT_UGC).",
                new BigDecimal("1000"),
                15,
                15,
                1,
                "1080:1920",
                "1080p",
                false,
                "Vega",
                "Converter interesse em diagnóstico",
                "Você se arruma, mas falta presença?",
                "MUSA usa IA para criar um plano simples.",
                "Dúvida, mecanismo, alívio e ação",
                "Mulher brasileira adulta",
                "Ambiente claro sem espelho",
                "Tela real do diagnóstico MUSA",
                "UGC premium e natural",
                "Uma única tomada estável",
                "Você se arruma, mas ainda sente que falta presença? | Faça o diagnóstico gratuito.",
                "Faça o diagnóstico gratuito",
                "image",
                "https://assets.example/apresentadora.png",
                "https://assets.example/musa-pde.png",
                "consentimento-91",
                withRights ? "direitos-91" : "",
                "Sem texto gerado",
                "Sem tremor e com texto sincronizado",
                "Validar criativo premium",
                "Reter e gerar CTA");
    }
}
