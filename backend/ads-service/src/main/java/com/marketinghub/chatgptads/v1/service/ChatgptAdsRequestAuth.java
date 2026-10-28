package com.marketinghub.chatgptads.v1.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Autentica requests internos assinados sem transmitir o segredo pela rede operacional. */
@Component
public class ChatgptAdsRequestAuth {
  private final String secret;
  private final ConcurrentHashMap<String, Long> nonces = new ConcurrentHashMap<>();

  /** Recebe segredo dedicado provisionado pelo workflow, fora do repositório. */
  public ChatgptAdsRequestAuth(@Value("${CHATGPT_ADS_WORKER_TOKEN:}") String secret) {
    this.secret = secret;
  }

  /** Informa configuração sem revelar nem transportar a chave. */
  public boolean configured() {
    return secret != null && secret.length() >= 32;
  }

  /** Confere corpo, rota, prazo e nonce antes de reservar ou confirmar trabalho. */
  public void verify(String path, String body, HttpHeaders headers) {
    String time = headers.getFirst("X-Mkt-Ads-Time"),
        nonce = headers.getFirst("X-Mkt-Ads-Nonce"),
        signature = headers.getFirst("X-Mkt-Ads-Signature");
    if (!configured()
        || time == null
        || !time.matches("[0-9]{10,11}")
        || nonce == null
        || !nonce.matches("[a-zA-Z0-9-]{32,64}")
        || signature == null
        || !signature.matches("[a-f0-9]{64}")) deny();
    long now = Instant.now().getEpochSecond();
    if (Math.abs(now - Long.parseLong(time)) > 300) deny();
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String digest =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(body.getBytes(StandardCharsets.UTF_8)));
      byte[] expected =
          mac.doFinal(
              ("POST\n" + path + "\n" + time + "\n" + nonce + "\n" + digest)
                  .getBytes(StandardCharsets.UTF_8));
      if (!MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature))) deny();
    } catch (java.security.GeneralSecurityException ex) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .error("ChatGPT Ads falha ao verificar assinatura rota={}", path, ex);
      deny();
    }
    nonces.entrySet().removeIf(e -> e.getValue() < now - 600);
    if (nonces.size() > 10000 || nonces.putIfAbsent(nonce, now) != null) deny();
  }

  /** Rejeita autenticação inválida sem revelar detalhe do segredo. */
  private static void deny() {
    throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED, "Assinatura do executor inválida ou repetida.");
  }

  /** Vincula a resposta ao nonce do request para impedir troca de comandos em trânsito. */
  public String signResponse(int status, String nonce, String body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String digest =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(body.getBytes(StandardCharsets.UTF_8)));
      return HexFormat.of()
          .formatHex(
              mac.doFinal(
                  ("RESPONSE\n" + status + "\n" + nonce + "\n" + digest)
                      .getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.GeneralSecurityException ex) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .error("ChatGPT Ads falha ao assinar resposta", ex);
      throw new IllegalStateException("Resposta do executor não pôde ser autenticada", ex);
    }
  }
}
