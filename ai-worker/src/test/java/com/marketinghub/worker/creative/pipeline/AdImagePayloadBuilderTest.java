package com.marketinghub.worker.creative.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdImagePayloadBuilderTest {

    private final AdImagePayloadBuilder builder = new AdImagePayloadBuilder();

    @Test
    void buildDorFeedVariant() {
        AdImagePayloadBuilder.BuildAdImagePayloadsInput input = baseInput(List.of(
                new AdImagePayloadBuilder.VisualVariant(
                        "V1",
                        "dor",
                        "feed",
                        "dor",
                        new AdImagePayloadBuilder.Concept("Rotina corrida sem tempo para cozinhar saudável"),
                        "falta de energia no fim do dia",
                        "pessoa olhando marmita industrializada e expressão de cansaço",
                        new AdImagePayloadBuilder.OnImageCopy("Energia em 7 dias", "Sem dietas extremas", "Método", "Quero testar agora"),
                        List.of("luz natural", "close no rosto", "fundo de cozinha real"))));

        AdImagePayloadBuilder.ImageBuildPayloadsOutput output = builder.buildAdImagePayloads(input);

        assertThat(output.imageBuildPayloads()).hasSize(1);
        AdImagePayloadBuilder.ImageBuildPayload payload = output.imageBuildPayloads().getFirst();
        assertThat(payload.placement()).isEqualTo("feed");
        assertThat(payload.imageParams().size()).isEqualTo("1024x1536");
        assertThat(payload.overlayCopy().headline()).isEqualTo("Energia em 7 dias");
        assertThat(payload.consistency().ctaMatch()).isEqualTo("Quero emagrecer com saúde");
        assertThat(payload.imagePrompt()).contains("nicho", "Instagram/Meta Ads", "foco visual único");
    }

    @Test
    void buildResultadoStoriesVariant() {
        AdImagePayloadBuilder.BuildAdImagePayloadsInput input = baseInput(List.of(
                new AdImagePayloadBuilder.VisualVariant(
                        "V2",
                        "resultado",
                        "stories",
                        "resultado",
                        new AdImagePayloadBuilder.Concept("Antes e depois simbólico no espelho"),
                        "roupas voltando a servir",
                        "mesma pessoa com postura confiante em frente ao espelho",
                        new AdImagePayloadBuilder.OnImageCopy("Resultado visível", "Plano simples no celular", "Real", "Começar hoje mesmo"),
                        List.of("enquadramento vertical", "contraste quente"))));

        AdImagePayloadBuilder.ImageBuildPayload payload = builder.buildAdImagePayloads(input).imageBuildPayloads().getFirst();

        assertThat(payload.placement()).isEqualTo("stories");
        assertThat(payload.imageParams().size()).isEqualTo("1024x1792");
        assertThat(payload.overlayCopy().cta()).isEqualTo("Começar hoje mesmo");
        assertThat(payload.assetId()).isEqualTo("AD-10-V2-stories");
    }

    @Test
    void buildProvaFeedVariant() {
        AdImagePayloadBuilder.BuildAdImagePayloadsInput input = baseInput(List.of(
                new AdImagePayloadBuilder.VisualVariant(
                        "V3",
                        "prova",
                        "feed",
                        "prova",
                        new AdImagePayloadBuilder.Concept("Depoimento em ambiente doméstico"),
                        "desconfiança sobre promessas de emagrecimento",
                        "cliente real mostrando foto no celular",
                        new AdImagePayloadBuilder.OnImageCopy("Funciona na rotina real", "Sem academia", "Prova", "Ver depoimentos"),
                        List.of("pele natural", "sem elementos de dashboard"))));

        AdImagePayloadBuilder.ImageBuildPayload payload = builder.buildAdImagePayloads(input).imageBuildPayloads().getFirst();

        assertThat(payload.placement()).isEqualTo("feed");
        assertThat(payload.consistency().singleMindedPromise()).isEqualTo("Perder gordura sem passar fome");
        assertThat(payload.experimentMetadata().assetRole()).isEqualTo("ad-image-build");
    }

    @Test
    void blocksWhenVisualVariantDoesNotMatchCopyVariant() {
        AdImagePayloadBuilder.BuildAdImagePayloadsInput input = baseInput(List.of(
                new AdImagePayloadBuilder.VisualVariant(
                        "V9",
                        "inexistente",
                        "feed",
                        "erro",
                        new AdImagePayloadBuilder.Concept("Qualquer"),
                        "dor",
                        "metafora",
                        new AdImagePayloadBuilder.OnImageCopy("h", "s", "b", "c"),
                        List.of("direcao"))));

        assertThatThrownBy(() -> builder.buildAdImagePayloads(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("copy correspondente");
    }

    @Test
    void exampleUsageWithRealisticMock() {
        AdImagePayloadBuilder.BuildAdImagePayloadsInput input = baseInput(List.of(
                new AdImagePayloadBuilder.VisualVariant(
                        "V1",
                        "dor",
                        "feed",
                        "dor",
                        new AdImagePayloadBuilder.Concept("Mãe empreendedora sem tempo para refeições equilibradas"),
                        "cansaço no trabalho e culpa por não cuidar da saúde",
                        "mesa de home office com notebook aberto e prato saudável pronto",
                        new AdImagePayloadBuilder.OnImageCopy(
                                "Energia para render no trabalho",
                                "Plano alimentar de 15 min por dia",
                                "Nutrição",
                                "Quero o plano"),
                        List.of("tons quentes", "foco no prato + expressão de alívio", "fundo doméstico real"))));

        AdImagePayloadBuilder.ImageBuildPayload payload = builder.buildAdImagePayloads(input).imageBuildPayloads().getFirst();

        assertThat(payload.imagePrompt()).contains("Instagram/Meta Ads", "composição simples e forte", "Evitar aparência de dashboard");
        assertThat(payload.overlayCopy().headline()).isEqualTo("Energia para render no trabalho");
    }

    private AdImagePayloadBuilder.BuildAdImagePayloadsInput baseInput(List<AdImagePayloadBuilder.VisualVariant> variants) {
        return new AdImagePayloadBuilder.BuildAdImagePayloadsInput(
                new AdImagePayloadBuilder.ExperimentMetadata("10", "V1", "AD", "treatment", null),
                new AdImagePayloadBuilder.CampaignAngle(
                        "Perder gordura sem passar fome",
                        "Quero emagrecer com saúde",
                        "Método alinhado à landing de emagrecimento para mães empreendedoras",
                        "mães empreendedoras de 30 a 45 anos que trabalham em casa"),
                new AdImagePayloadBuilder.AdCopy(List.of(
                        new AdImagePayloadBuilder.AdCopyVariant("dor", "Cansada sem energia", "Texto dor", "Quero emagrecer com saúde"),
                        new AdImagePayloadBuilder.AdCopyVariant("resultado", "Volte a vestir 40", "Texto resultado", "Começar hoje"),
                        new AdImagePayloadBuilder.AdCopyVariant("prova", "Veja casos reais", "Texto prova", "Ver depoimentos"))),
                new AdImagePayloadBuilder.AdImageBriefing(
                        new AdImagePayloadBuilder.GlobalDesignSystem("fotografia lifestyle realista com luz natural"),
                        variants));
    }
}
