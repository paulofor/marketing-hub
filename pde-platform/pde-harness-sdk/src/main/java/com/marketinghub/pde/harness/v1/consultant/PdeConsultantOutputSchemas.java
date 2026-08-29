package com.marketinghub.pde.harness.v1.consultant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import java.io.IOException;
import java.io.InputStream;

/** Disponibiliza o contrato estruturado inicial sem hardcode de schema na classe Java. */
public final class PdeConsultantOutputSchemas {
  public static final String DEFAULT_V1_VERSION = "consultant-response-v1";
  private static final String DEFAULT_V1_RESOURCE =
      "/prompts/consultant/v1/consultant-response-schema.json";
  private static final System.Logger LOGGER =
      System.getLogger(PdeConsultantOutputSchemas.class.getName());

  /** Impede instanciação do catálogo de schemas versionados. */
  private PdeConsultantOutputSchemas() {}

  /** Carrega uma cópia do schema inicial para o worker especializar quando necessário. */
  public static JsonNode defaultV1() {
    try (InputStream input =
        PdeConsultantOutputSchemas.class.getResourceAsStream(DEFAULT_V1_RESOURCE)) {
      if (input == null) {
        throw new IOException("Recurso não encontrado: " + DEFAULT_V1_RESOURCE);
      }
      return new ObjectMapper().readTree(input);
    } catch (IOException ex) {
      LOGGER.log(System.Logger.Level.ERROR, "Falha ao carregar schema do consultor", ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.CONFIGURATION,
          "Schema versionado do consultor não está disponível",
          ex);
    }
  }
}
