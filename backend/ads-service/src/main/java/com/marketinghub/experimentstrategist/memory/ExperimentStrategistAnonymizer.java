package com.marketinghub.experimentstrategist.memory;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Responsabilidade: remover identificadores pessoais de artefatos textuais antes do S3. */
@Component
public class ExperimentStrategistAnonymizer {
  private static final Pattern EMAIL =
      Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
  private static final Pattern PHONE =
      Pattern.compile(
          "(?<!\\d)(?:\\+?55\\s*)?(?:\\(?\\d{2}\\)?\\s*)?9?\\d{4}[-.\\s]?\\d{4}(?!\\d)");
  private static final Pattern IPV4 = Pattern.compile("(?<!\\d)(?:\\d{1,3}\\.){3}\\d{1,3}(?!\\d)");
  private static final Pattern CPF =
      Pattern.compile("(?<!\\d)\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}(?!\\d)");

  /** Substitui identificadores conhecidos por marcadores sem tentar inferir identidade. */
  public String anonymize(String text) {
    if (text == null) return "";
    String sanitized = EMAIL.matcher(text).replaceAll("[EMAIL_REMOVIDO]");
    sanitized = PHONE.matcher(sanitized).replaceAll("[TELEFONE_REMOVIDO]");
    sanitized = IPV4.matcher(sanitized).replaceAll("[IP_REMOVIDO]");
    return CPF.matcher(sanitized).replaceAll("[CPF_REMOVIDO]");
  }
}
