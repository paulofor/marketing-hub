package com.marketinghub.facebookadsworker.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Protege a ativação padrão da rotina de pixels nos descritores usados pelo runtime. */
class FacebookPixelRuntimeConfigurationTest {

    /** Garante que aplicação e Compose mantenham a mensuração low-ticket ativa por padrão. */
    @Test
    void keepsPixelAutomationEnabledByDefaultAcrossRuntimeDescriptors() throws IOException {
        String applicationProperties = Files.readString(Path.of("src/main/resources/application.properties"));
        String composeFile = Files.readString(Path.of("docker-compose.yml"));

        assertThat(applicationProperties).contains("facebookpixel.enabled=${FACEBOOKPIXEL_ENABLED:true}");
        assertThat(composeFile).contains("FACEBOOKPIXEL_ENABLED: ${FACEBOOKPIXEL_ENABLED:-true}");
    }
}
