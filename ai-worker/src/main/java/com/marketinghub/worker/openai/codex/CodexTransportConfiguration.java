package com.marketinghub.worker.openai.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Instala o transporte Codex nos clientes Responses existentes sem assumir avanço de pipelines. */
@Configuration
@ConditionalOnProperty(name = "openai.text-transport", havingValue = "codex-app-server")
public class CodexTransportConfiguration {
  /** Cria um único transporte serializado e limitado para a sessão exclusiva do Worker IA. */
  @Bean
  public CodexAppServerTransport codexAppServerTransport(
      @Value("${openai.codex.node:node}") String node,
      @Value("${openai.codex.script:/app/codex/responses.mjs}") String script,
      @Value("${openai.codex.timeout:PT16M}") Duration timeout,
      ObjectMapper mapper) {
    return new CodexAppServerTransport(node, script, timeout, mapper);
  }

  /**
   * Preserva filtros dos clientes e restringe a substituição ao endpoint configurado de Responses.
   */
  @Bean
  public WebClientCustomizer codexResponsesCustomizer(
      @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
      CodexAppServerTransport transport,
      ObjectMapper mapper) {
    return builder -> builder.filter(new CodexResponsesFilter(baseUrl, transport, mapper));
  }
}
