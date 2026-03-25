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
        product.setPromise("Ensinar a triplicar o faturamento em 90 dias");
        product.setExplicitPain("Agências travadas em prospecção");
        product.setRiskReversal("Garantia total de 30 dias");

        String prompt = builder.buildPrompt(profile, product);

        assertThat(prompt).contains("Oferta Irresistível");
        assertThat(prompt).contains("Paula Visionária");
        assertThat(prompt).contains("triplicar o faturamento");
        assertThat(prompt).contains("Garantia total de 30 dias");
        assertThat(prompt).contains("storyboard");
        assertThat(prompt).contains("JSON válido");
    }
}
