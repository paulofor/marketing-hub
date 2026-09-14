package com.marketinghub.videomanagement.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: validar os padrões operacionais da configuração de vídeo. */
class VideoManagementPropertiesTest {

    /** Garante que uma configuração visual antiga não seja usada em novas cenas. */
    @Test
    void normalizesRetiredImageToolModelToSunburst() {
        VideoManagementProperties.Luma luma = new VideoManagementProperties.Luma();

        luma.setOpenAiImageToolModel("gpt-image-2");

        assertThat(luma.getOpenAiImageToolModel()).isEqualTo("gpt-image-2.5-sunburst");
    }
}
