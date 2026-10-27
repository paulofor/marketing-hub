package com.marketinghub.pde.kit.privateprototype.v1.service;

import java.util.List;

/** Responsabilidade: declarar especialidades implementadas sem inferir capacidade pelo produto. */
public final class KitSpecialties {
  public static final String BEAUTY = "beauty-v1";
  private static final List<Specialty> BEAUTY_PROFILES =
      List.of(
          new Specialty("nails-v1", "Unhas", "Manicure e nail design"),
          new Specialty("barber-v1", "Barbearia", "Corte e barba"),
          new Specialty("hair-v1", "Cabelo", "Corte e finalização"),
          new Specialty("makeup-v1", "Maquiagem", "Maquiagem para ocasiões"),
          new Specialty("brows-v1", "Sobrancelhas e cílios", "Design de sobrancelhas"),
          new Specialty(
              "aesthetics-v1", "Estética não clínica", "Cuidados estéticos não clínicos"));

  /** Impede instanciação do catálogo imutável. */
  private KitSpecialties() {}

  /** Expõe somente opções do contrato executável, sem ampliar perfis históricos. */
  public static List<Specialty> profiles(String code) {
    if (BEAUTY.equals(code)) return BEAUTY_PROFILES;
    return BEAUTY_PROFILES.stream().filter(p -> p.code().equals(code)).toList();
  }

  /** Resolve códigos explícitos e aliases documentados sem deduzir profissão de nomes ou IDs. */
  public static java.util.Optional<String> declaredProfile(
      com.fasterxml.jackson.databind.JsonNode definition) {
    String explicit = definition.path("kitProfileCode").asText("");
    String reference = definition.path("strategyReference").asText("");
    String format = definition.path("format").asText("");
    var matches =
        java.util.stream.Stream.concat(
                BEAUTY_PROFILES.stream().map(Specialty::code), java.util.stream.Stream.of(BEAUTY))
            .filter(
                code ->
                    code.equals(explicit)
                        || explicit.isBlank()
                            && (reference.matches(
                                    "(?s).*\\b" + java.util.regex.Pattern.quote(code) + "\\b.*")
                                || format.matches(
                                    "(?s).*\\b" + java.util.regex.Pattern.quote(code) + "\\b.*")))
            .toList();
    return matches.size() == 1
        ? java.util.Optional.of(matches.getFirst())
        : java.util.Optional.empty();
  }

  /**
   * Confere a autorização do adaptador privado sem efeitos externos antes de oferecer a vinculação.
   */
  public static boolean privateAdapterSupported(
      com.fasterxml.jackson.databind.JsonNode definition) {
    var fixtures = definition.path("staticResultFixtures");
    return "PDE_STATIC_RESULT_FIXTURES_V1".equals(fixtures.path("contractVersion").asText())
        && "DETERMINISTIC_HOMOLOGATION_ONLY".equals(fixtures.path("mode").asText())
        && fixtures.path("providerCallsAuthorized").isIntegralNumber()
        && fixtures.path("providerCallsAuthorized").asInt(-1) == 0
        && !fixtures.path("externalSideEffects").asBoolean(true)
        && !fixtures.path("commercialEvidenceEligible").asBoolean(true);
  }

  /** Identifica famílias instaladas para a prova privada, sem autorização comercial. */
  public static boolean supported(String code) {
    return !profiles(code).isEmpty();
  }

  /** Responsabilidade: fornecer rótulo e exemplo próprios de cada especialidade na tela. */
  public record Specialty(
      String code,
      String label,
      String serviceExample,
      String sampleHeadline,
      String sampleCaption) {
    /** Mantém uma amostra contextualizada e identificada sem representar trabalho de cliente. */
    public Specialty(String code, String label, String serviceExample) {
      this(
          code,
          label,
          serviceExample,
          "Seu momento de " + label.toLowerCase(java.util.Locale.ROOT),
          serviceExample
              + " em Cidade Exemplo. Quer conhecer as opções e os horários? Converse com Studio Exemplo pelo WhatsApp. Demonstração ilustrativa; não representa serviço realizado.");
    }
  }
}
