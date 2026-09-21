package com.marketinghub.customeragentworker;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;

/** Responsabilidade: homologar a captura real local com o mesmo gate anterior ao modelo pago. */
public class PublicationCaptureLocalCheck {
  /** Confere origem, página servida, CTA e PNGs; nunca inicia modelo ou callback produtivo. */
  public static void main(String[] args) throws Exception {
    var json = new ObjectMapper().findAndRegisterModules();
    var capture =
        json.readValue(Path.of(args[0]).toFile(), BpmVisualEvidenceRunner.CaptureOutput.class);
    var contract =
        new PdeExperienceEvidenceLoader.LiveVisualContract(
            List.of("Comprar kit por R$ 67"),
            List.of("Kit para divulgar seu trabalho"),
            PdeExperienceEvidenceLoader.RuntimeIdentity.none(),
            args[2]);
    var method =
        BpmVisualEvidenceRunner.class.getDeclaredMethod(
            "validateCapture",
            String.class,
            Path.class,
            BpmVisualEvidenceRunner.CaptureOutput.class,
            PdeExperienceEvidenceLoader.LiveVisualContract.class);
    method.setAccessible(true);
    try {
      method.invoke(
          new BpmVisualEvidenceRunner(json, "unused", "unused"),
          capture.captureSessionId(),
          Path.of(args[1]),
          capture,
          contract);
      if ("BLOCKED".equals(args[3]))
        throw new AssertionError("Página sem identidade não foi bloqueada.");
      System.out.println("PASS: identidade, CTA e arquivos capturados conferidos antes do modelo.");
    } catch (InvocationTargetException ex) {
      if (!"BLOCKED".equals(args[3])
          || !(ex.getCause() instanceof BpmVisualEvidenceRunner.PublicationIdentityException)) {
        org.slf4j.LoggerFactory.getLogger(PublicationCaptureLocalCheck.class)
            .error("Fixture: falha inesperada na captura local arquivo={}", args[0], ex);
        throw ex;
      }
      System.out.println("PASS: página antiga bloqueada antes de qualquer chamada paga.");
    }
  }
}
