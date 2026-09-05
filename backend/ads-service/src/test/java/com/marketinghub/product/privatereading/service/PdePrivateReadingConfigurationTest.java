package com.marketinghub.product.privatereading.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Responsabilidade: validar a leitura do segredo montado pelo deploy sem incluí-lo em imagem. */
class PdePrivateReadingConfigurationTest {
  @TempDir Path directory;

  /** Comprova que o configtree preenche a propriedade usada pelo cliente autenticado. */
  @Test
  void loadsInternalTokenFromMountedConfigTree() throws Exception {
    Files.writeString(
        directory.resolve("integrations.pde-platform.internal-token"), "synthetic-local-token");
    new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withPropertyValues("spring.config.import=optional:configtree:" + directory + "/")
        .run(
            context ->
                assertThat(
                        context
                            .getEnvironment()
                            .getProperty("integrations.pde-platform.internal-token"))
                    .isEqualTo("synthetic-local-token"));
  }
}
