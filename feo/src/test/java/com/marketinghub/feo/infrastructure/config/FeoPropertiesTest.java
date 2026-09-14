package com.marketinghub.feo.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a política de configuração visual da FEO. */
class FeoPropertiesTest {

    /** Promove modelo e qualidade antigos aos padrões homologados para novas execuções. */
    @Test
    void normalizesRetiredVisualConfiguration() {
        FeoProperties properties = new FeoProperties(
                "worker",
                "http://backend",
                1,
                "target",
                "https://api.openai.com",
                "",
                "",
                "gpt-image-2",
                "hd",
                true);

        assertThat(properties.imageModel()).isEqualTo("gpt-image-2.5-sunburst");
        assertThat(properties.imageQuality()).isEqualTo("high");
    }
}
