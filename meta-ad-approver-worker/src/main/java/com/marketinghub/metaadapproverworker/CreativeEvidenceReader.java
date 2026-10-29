package com.marketinghub.metaadapproverworker;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Responsabilidade: recuperar leituras transitórias de provas antes da inferência de Têmis. */
final class CreativeEvidenceReader {
  private static final Logger log = LoggerFactory.getLogger(CreativeEvidenceReader.class);
  private static final int MAX_ATTEMPTS = 3;

  /** Impede instanciação do leitor sem estado compartilhado. */
  private CreativeEvidenceReader() {}

  /** Repete somente GET transitório, com limite e pausa, preservando contrato e cancelamento. */
  static <T> T read(RestClient backend, String endpoint, long taskId, Class<T> type)
      throws IOException {
    for (int attempt = 1; ; attempt++) {
      if (Thread.currentThread().isInterrupted())
        throw new IOException("Leitura das provas interrompida. taskId=" + taskId);
      try {
        return backend.get().uri(endpoint).retrieve().body(type);
      } catch (RestClientException ex) {
        boolean retry = attempt < MAX_ATTEMPTS && transientRead(ex);
        log.warn(
            "Falha na leitura de prova de Têmis. taskId={} endpoint={} attempt={} maxAttempts={} retry={}",
            taskId,
            endpoint,
            attempt,
            MAX_ATTEMPTS,
            retry,
            ex);
        if (!retry) throw ex;
        try {
          Thread.sleep(250L * attempt);
        } catch (InterruptedException interrupted) {
          log.warn(
              "Recuperação da leitura de prova interrompida. taskId={} endpoint={}",
              taskId,
              endpoint,
              interrupted);
          Thread.currentThread().interrupt();
          throw new IOException("Recuperação da leitura de prova interrompida.", interrupted);
        }
      }
    }
  }

  /** Distingue transporte temporário de autorização, segurança, cancelamento e erro funcional. */
  private static boolean transientRead(RestClientException ex) {
    if (Thread.currentThread().isInterrupted()) return false;
    for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
      if (cause instanceof InterruptedException
          || cause instanceof javax.net.ssl.SSLException
          || cause instanceof java.net.UnknownHostException) return false;
    }
    if (ex instanceof ResourceAccessException) return true;
    if (ex instanceof RestClientResponseException response) {
      int status = response.getStatusCode().value();
      return status == 502 || status == 503 || status == 504;
    }
    return false;
  }
}
