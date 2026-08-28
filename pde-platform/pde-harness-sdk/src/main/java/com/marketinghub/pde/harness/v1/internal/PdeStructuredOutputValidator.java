package com.marketinghub.pde.harness.v1.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Valida a resposta funcional do agente contra o schema versionado antes do callback. */
public final class PdeStructuredOutputValidator {
  private static final System.Logger LOGGER =
      System.getLogger(PdeStructuredOutputValidator.class.getName());

  private final ObjectMapper mapper;
  private final SchemaRegistry schemaRegistry;
  private final ConcurrentHashMap<String, Schema> compiledSchemas = new ConcurrentHashMap<>();

  /** Cria um validador local com dialecto 2020-12 e sem fonte externa de schemas. */
  public PdeStructuredOutputValidator(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
  }

  /** Converte a saída em JSON e exige aderência integral ao schema informado pelo worker. */
  public JsonNode validate(String output, JsonNode outputSchema) {
    JsonNode parsed = parseOutput(output);
    String schemaHash = PdeHashing.sha256(mapper, outputSchema);
    try {
      Schema compiled =
          compiledSchemas.computeIfAbsent(
              schemaHash,
              ignored -> schemaRegistry.getSchema(outputSchema.toString(), InputFormat.JSON));
      List<com.networknt.schema.Error> errors =
          compiled.validate(
              parsed.toString(),
              InputFormat.JSON,
              executionContext ->
                  executionContext.executionConfig(
                      executionConfig ->
                          executionConfig.formatAssertionsEnabled(true).failFast(true)));
      if (!errors.isEmpty()) {
        com.networknt.schema.Error first = errors.getFirst();
        throw new PdeHarnessException(
            PdeHarnessFailureCategory.EXECUTION_FAILED,
            "Saída do agente não atende ao schema em "
                + first.getInstanceLocation()
                + " ("
                + first.getKeyword()
                + ")");
      }
      return parsed.deepCopy();
    } catch (PdeHarnessException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Resposta estruturada do agente foi rejeitada; schemaSha256=" + schemaHash,
          ex);
      throw ex;
    } catch (RuntimeException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao compilar ou validar schema da resposta; schemaSha256=" + schemaHash,
          ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.CONFIGURATION,
          "Não foi possível validar a resposta estruturada do agente",
          ex);
    }
  }

  /** Faz parsing estrito sem registrar o conteúdo potencialmente sensível da resposta. */
  private JsonNode parseOutput(String output) {
    if (output == null || output.isBlank()) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.EXECUTION_FAILED,
          "Agente concluiu sem produzir a saída estruturada obrigatória");
    }
    try {
      return mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(output);
    } catch (JsonProcessingException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Agente concluiu com saída que não é um JSON único e válido",
          ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.EXECUTION_FAILED,
          "Saída estruturada do agente não é um JSON válido",
          ex);
    }
  }
}
