package com.marketinghub.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenAiApiKeyResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldPreferDirectApiKeyOverFile() throws Exception {
        Path tokenFile = tempDir.resolve("openai_api_key");
        Files.writeString(tokenFile, "file-token\n");
        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("direct-token");
        properties.setApiKeyFile(tokenFile.toString());

        String token = new OpenAiApiKeyResolver().resolve(properties);

        assertThat(token).isEqualTo("direct-token");
    }

    @Test
    void shouldReadApiKeyFromConfiguredFileWhenDirectKeyIsBlank() throws Exception {
        Path tokenFile = tempDir.resolve("openai_api_key");
        Files.writeString(tokenFile, "file-token\n");
        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("");
        properties.setApiKeyFile(tokenFile.toString());

        String token = new OpenAiApiKeyResolver().resolve(properties);

        assertThat(token).isEqualTo("file-token");
    }
}
