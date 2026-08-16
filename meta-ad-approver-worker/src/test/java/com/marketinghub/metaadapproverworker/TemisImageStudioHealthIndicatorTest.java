package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Status;

/** Responsabilidade: proteger o health real da capacidade visual de Têmis. */
class TemisImageStudioHealthIndicatorTest {
  @TempDir Path temporaryDirectory;

  /** Confirma que um arquivo regular e preenchido mantém a capacidade visual saudável. */
  @Test
  void reportsUpForReadableCredentialFile() throws Exception {
    Path keyFile = Files.writeString(temporaryDirectory.resolve("openai-key"), "test-key");
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    properties.setOpenAiApiKeyFile(keyFile.toString());

    assertThat(new TemisImageStudioHealthIndicator(properties).health().getStatus())
        .isEqualTo(Status.UP);
  }

  /** Confirma que a chave direta configurada mantém a capacidade visual saudável. */
  @Test
  void reportsUpForDirectCredential() {
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    properties.setOpenAiApiKey("test-key");

    assertThat(new TemisImageStudioHealthIndicator(properties).health().getStatus())
        .isEqualTo(Status.UP);
  }

  /** Confirma que um diretório não pode ser aceito como arquivo de segredo. */
  @Test
  void reportsDownWhenCredentialPathIsDirectory() {
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    properties.setOpenAiApiKeyFile(temporaryDirectory.toString());

    assertThat(new TemisImageStudioHealthIndicator(properties).health().getStatus())
        .isEqualTo(Status.DOWN);
  }

  /** Confirma que um modelo visual inferior ou desconhecido derruba o health. */
  @Test
  void reportsDownWhenImageModelIsNotCanonical() throws Exception {
    Path keyFile = Files.writeString(temporaryDirectory.resolve("openai-key"), "test-key");
    MetaAdApproverProperties properties = new MetaAdApproverProperties();
    properties.setOpenAiApiKeyFile(keyFile.toString());
    properties.setImageModel("gpt-image-1");

    assertThat(new TemisImageStudioHealthIndicator(properties).health().getStatus())
        .isEqualTo(Status.DOWN);
  }
}
