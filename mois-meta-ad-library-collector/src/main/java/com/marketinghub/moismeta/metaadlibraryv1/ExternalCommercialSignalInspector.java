package com.marketinghub.moismeta.metaadlibraryv1;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Confirma disponibilidade e sinal comercial somente em destinos HTTP fornecidos pela origem. */
@Component
@Slf4j
public class ExternalCommercialSignalInspector {

  private static final Pattern COMMERCIAL_PATTERN =
      Pattern.compile("(?i)(R\\$\\s*\\d|checkout|comprar agora|adicione ao carrinho|garantia)");
  private final HttpClient httpClient =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(10))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  /** Verifica uma página pública sem transformar falha ou texto ausente em evidência. */
  public Result inspect(String destinationUrl, long investigationId) {
    if (destinationUrl == null
        || (!destinationUrl.startsWith("https://") && !destinationUrl.startsWith("http://"))) {
      return new Result(false, false);
    }
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(destinationUrl))
              .timeout(Duration.ofSeconds(20))
              .header("User-Agent", "MarketingHub-MOIS/1.0")
              .GET()
              .build();
      log.info(
          "MOIS Meta sinal externo request investigationId={} url={}",
          investigationId,
          destinationUrl);
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      log.info(
          "MOIS Meta sinal externo response investigationId={} url={} status={}",
          investigationId,
          destinationUrl,
          response.statusCode());
      boolean active = response.statusCode() >= 200 && response.statusCode() < 400;
      return new Result(
          active, active && COMMERCIAL_PATTERN.matcher(response.body()).find());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.error(
          "Inspeção externa interrompida investigationId={} url={}",
          investigationId,
          destinationUrl,
          ex);
      return new Result(false, false);
    } catch (Exception ex) {
      log.error(
          "Falha na inspeção externa investigationId={} url={}",
          investigationId,
          destinationUrl,
          ex);
      return new Result(false, false);
    }
  }

  /** Resume os dois sinais externos exigidos pelo gate. */
  public record Result(boolean pageActive, boolean commercialSignal) {}
}
