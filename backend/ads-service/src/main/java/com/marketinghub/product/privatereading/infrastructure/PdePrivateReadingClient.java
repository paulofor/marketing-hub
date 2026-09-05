package com.marketinghub.product.privatereading.infrastructure;

import com.marketinghub.product.privatereading.service.evidence.PrivateReadingEvidence;
import java.net.http.HttpClient;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Responsabilidade: ler a prova privada pelo contrato autenticado do backend PDE configurado. */
@Component
@Slf4j
public class PdePrivateReadingClient {
  private final RestClient client;
  private final String internalToken;

  /**
   * Configura uma origem fixa e timeouts; nunca envia segredos para a URL fornecida pelo produto.
   */
  public PdePrivateReadingClient(
      @Value("${integrations.pde-platform.base-url:https://v5.clubemusa.com.br}") String baseUrl,
      @Value("${integrations.pde-platform.internal-token:}") String internalToken) {
    this.internalToken = internalToken == null ? "" : internalToken;
    var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    var factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofSeconds(15));
    client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
  }

  /** Obtém a leitura sem fallback para QA, métricas agregadas ou declaração manual. */
  public PrivateReadingEvidence fetch(int readingNumber) {
    if (internalToken.isBlank()) {
      throw new IllegalStateException("A integração privada com o PDE precisa ser configurada.");
    }
    try {
      return client
          .get()
          .uri("/api/pde/mira/private/v1/internal/readings/{number}", readingNumber)
          .header("X-PDE-Internal-Token", internalToken)
          .retrieve()
          .body(PrivateReadingEvidence.class);
    } catch (RuntimeException ex) {
      log.error("Falha na consulta de leitura privada do PDE; readingNumber={}", readingNumber, ex);
      throw new IllegalStateException(
          "Não foi possível consultar a leitura no protótipo. Tente atualizar novamente; nenhum sinal foi presumido.",
          ex);
    }
  }
}
