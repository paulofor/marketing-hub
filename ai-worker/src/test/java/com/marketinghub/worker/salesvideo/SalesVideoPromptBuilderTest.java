package com.marketinghub.worker.salesvideo;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.product.dto.ProductDto;
import com.marketinghub.salesvideo.SalesVideoKind;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import org.junit.jupiter.api.Test;

class SalesVideoPromptBuilderTest {

    private final SalesVideoPromptBuilder builder = new SalesVideoPromptBuilder();

    @Test
    void shouldIncludeKeyFieldsInPrompt() {
        SalesVideoProfileDto profile = new SalesVideoProfileDto();
        profile.setId(42L);
        profile.setTitle("Oferta Irresistível");
        profile.setPersonaName("Paula Visionária");
        profile.setPersonaStyle("didática e confiante");
        profile.setVoiceStyle("vibrante");
        profile.setLanguage("pt-BR");
        profile.setVideoKind(SalesVideoKind.HERO);
        profile.setTargetDurationSeconds(45);

        ProductDto product = new ProductDto();
        product.setName("Growth Sprint");
        product.setNiche("Agências de marketing B2B");
        product.setTargetAudience("Donos de agência que dependem de indicação");
        product.setPrimaryHypothesis("Agências pequenas compram quando enxergam um plano simples de prospecção diária");
        product.setPromise("Ensinar a triplicar o faturamento em 90 dias");
        product.setExplicitPain("Agências travadas em prospecção");
        product.setUniqueMechanism("Diagnóstico de gargalo comercial que transforma rotina em plano diário");
        product.setRiskReversal("Garantia total de 30 dias");
        product.setPrimaryCta("Fazer o diagnóstico comercial");

        String prompt = builder.buildPrompt(profile, product);

        assertThat(prompt).contains("Oferta Irresistível");
        assertThat(prompt).contains("Paula Visionária");
        assertThat(prompt).contains("Agências de marketing B2B");
        assertThat(prompt).contains("Donos de agência que dependem de indicação");
        assertThat(prompt).contains("plano simples de prospecção diária");
        assertThat(prompt).contains("Diagnóstico de gargalo comercial");
        assertThat(prompt).contains("Fazer o diagnóstico comercial");
        assertThat(prompt).contains("triplicar o faturamento");
        assertThat(prompt).contains("Garantia total de 30 dias");
        assertThat(prompt).contains("storyboard");
        assertThat(prompt).contains("conversa natural com o consumidor");
        assertThat(prompt).contains("Nao use exemplos de moda");
        assertThat(prompt).contains("qualquer produto digital");
        assertThat(prompt).doesNotContain("parar de sentir que falta algo no look");
        assertThat(prompt).contains("JSON valido");
    }
}
