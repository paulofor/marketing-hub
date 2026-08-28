package com.marketinghub.pde.harness.v1.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.harness.v1.PdeHarnessConfiguration;
import com.marketinghub.pde.harness.v1.PdeHarnessException;
import com.marketinghub.pde.harness.v1.PdeHarnessFailureCategory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Verifica que o SDK e o bundle oficial pertencem à mesma versão do Codex. */
public final class PdeProtocolContract {
  public static final String MANIFEST_RESOURCE = "/codex-app-server/0.149.0/manifest.json";
  private static final System.Logger LOGGER = System.getLogger(PdeProtocolContract.class.getName());

  private final PdeProtocolManifest manifest;

  /** Carrega e valida imediatamente o manifesto e o bundle embarcado. */
  public PdeProtocolContract(ObjectMapper mapper) {
    this.manifest = readManifest(Objects.requireNonNull(mapper, "mapper"));
    verifyBundle();
  }

  /** Confirma que a configuração operacional usa a mesma versão fixada no bundle. */
  public void verifyConfiguration(PdeHarnessConfiguration configuration) {
    if (!manifest.codexVersion().equals(configuration.expectedCodexVersion())) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
          "Versão configurada do Codex "
              + configuration.expectedCodexVersion()
              + " diverge do contrato "
              + manifest.codexVersion());
    }
    if (!"stdio-jsonl".equals(manifest.transport())) {
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
          "Transporte do contrato não é stdio-jsonl");
    }
  }

  /** Retorna a versão Codex validada para auditoria do resultado. */
  public String codexVersion() {
    return manifest.codexVersion();
  }

  /** Retorna o manifesto imutável para testes de contrato e diagnóstico. */
  public PdeProtocolManifest manifest() {
    return manifest;
  }

  /** Lê o manifesto JSON presente no classpath do SDK. */
  private PdeProtocolManifest readManifest(ObjectMapper mapper) {
    try (InputStream input = getClass().getResourceAsStream(MANIFEST_RESOURCE)) {
      if (input == null) {
        throw new PdeHarnessException(
            PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
            "Manifesto do protocolo não encontrado em " + MANIFEST_RESOURCE);
      }
      return mapper.readValue(input, PdeProtocolManifest.class);
    } catch (IOException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR, "Falha ao ler o manifesto do protocolo Codex App Server", ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
          "Manifesto do protocolo Codex App Server inválido",
          ex);
    }
  }

  /** Calcula novamente o hash do bundle para detectar drift ou arquivo parcial. */
  private void verifyBundle() {
    try (InputStream input = getClass().getResourceAsStream(manifest.schemaResource())) {
      if (input == null) {
        throw new PdeHarnessException(
            PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
            "Bundle do protocolo não encontrado em " + manifest.schemaResource());
      }
      String actualSha256 = PdeHashing.sha256(input.readAllBytes());
      if (!manifest.schemaSha256().equals(actualSha256)) {
        throw new PdeHarnessException(
            PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
            "SHA-256 do bundle Codex App Server diverge do manifesto");
      }
    } catch (IOException ex) {
      LOGGER.log(
          System.Logger.Level.ERROR,
          "Falha ao verificar o bundle do protocolo Codex App Server",
          ex);
      throw new PdeHarnessException(
          PdeHarnessFailureCategory.PROTOCOL_INCOMPATIBLE,
          "Não foi possível verificar o bundle Codex App Server",
          ex);
    }
  }
}
