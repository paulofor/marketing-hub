package com.marketinghub.metaadapproverworker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: impedir que o recurso visual de Dédalo pareça saudável sem configuração. */
@Component("temisImageStudio")
@ConditionalOnProperty(name = "meta-ad-approver.execution-role", havingValue = "image-studio")
public class TemisImageStudioHealthIndicator implements HealthIndicator {
  private final MetaAdApproverProperties properties;

  /** Configura a verificação com o mesmo contrato operacional usado pelo cliente de imagens. */
  public TemisImageStudioHealthIndicator(MetaAdApproverProperties properties) {
    this.properties = properties;
  }

  /** Verifica modelo GPT Image 2 e disponibilidade da chave sem expor seu conteúdo. */
  @Override
  public Health health() {
    if (!"gpt-image-2".equals(normalized(properties.getImageModel()))) {
      return Health.down().withDetail("reason", "modelo_visual_invalido").build();
    }
    if (StringUtils.hasText(properties.getOpenAiApiKey())) {
      return Health.up().withDetail("credential", "environment").build();
    }
    String configuredFile = properties.getOpenAiApiKeyFile();
    if (!StringUtils.hasText(configuredFile)) {
      return Health.down().withDetail("reason", "credencial_visual_ausente").build();
    }
    try {
      Path keyFile = Path.of(configuredFile.trim());
      if (!Files.isRegularFile(keyFile) || !Files.isReadable(keyFile) || Files.size(keyFile) == 0) {
        return Health.down().withDetail("reason", "arquivo_de_credencial_invalido").build();
      }
      return Health.up().withDetail("credential", "file").build();
    } catch (IOException | InvalidPathException | SecurityException ex) {
      return Health.down(ex).withDetail("reason", "credencial_visual_indisponivel").build();
    }
  }

  /** Normaliza uma propriedade textual antes da validação de contrato. */
  private String normalized(String value) {
    return StringUtils.hasText(value) ? value.trim() : "";
  }
}
