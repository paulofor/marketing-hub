package com.marketinghub.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: proteger a integridade e a auditoria da importação de pacotes criativos. */
class ApprovedCreativePackageArchiveTest {
  private static final byte[] PROOF = "prova-real".getBytes(StandardCharsets.UTF_8);
  private static final byte[] ASSET = "peca-final".getBytes(StandardCharsets.UTF_8);
  private static final byte[] FRAME = "frame-revisao".getBytes(StandardCharsets.UTF_8);
  private static final byte[] PREVIEW = "preview-canal".getBytes(StandardCharsets.UTF_8);
  private static final byte[] AUDIT = "auditoria-bruta".getBytes(StandardCharsets.UTF_8);
  private static final byte[] AGENT_PROMPT = "Núcleo do agente.".getBytes(StandardCharsets.UTF_8);
  private static final byte[] ACTIVITY_PROMPT =
      "Atividade do pacote.".getBytes(StandardCharsets.UTF_8);
  private static final byte[] REQUEST =
      "Núcleo do agente.\n\nAtividade do pacote.".getBytes(StandardCharsets.UTF_8);

  /** Aceita o mesmo contrato, manifesto, mídia e auditoria vinculados por hashes. */
  @Test
  void acceptsCompleteAuditablePackage() throws Exception {
    var validated =
        new ApprovedCreativePackageArchive(new ObjectMapper()).validate(validPackageBytes(), 4L);

    assertThat(validated.assets()).hasSize(2);
    assertThat(validated.assets())
        .extracting(item -> item.mediaType())
        .containsExactly("IMAGE", "IMAGE");
    assertThat(validated.psiqueExecution().executionId()).isEqualTo("psique-execution");
    assertThat(validated.temisExecution().executionId()).isEqualTo("temis-execution");
  }

  /** Rejeita troca do conteúdo mesmo quando o nome do arquivo permanece idêntico. */
  @Test
  void rejectsTamperedAsset() throws Exception {
    Map<String, byte[]> entries = entries();
    entries.put("assets/final.png", "conteudo-adulterado".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(
            () -> new ApprovedCreativePackageArchive(new ObjectMapper()).validate(zip(entries), 4L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("SHA-256 divergente");
  }

  /** Rejeita arquivo de outro plano antes de qualquer persistência ou upload. */
  @Test
  void rejectsPackageFromAnotherPlan() throws Exception {
    assertThatThrownBy(
            () ->
                new ApprovedCreativePackageArchive(new ObjectMapper())
                    .validate(validPackageBytes(), 5L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("outro plano");
  }

  /** Rejeita pacote novo que ainda atribua a materialização criativa a Dédalo. */
  @Test
  void rejectsLegacyDedaloDirectionAsCurrentMaterialization() throws Exception {
    Map<String, byte[]> entries = entries();
    entries.put("metadata/dedalo-direction.json", entries.remove("metadata/iris-direction.json"));

    assertThatThrownBy(
            () -> new ApprovedCreativePackageArchive(new ObjectMapper()).validate(zip(entries), 4L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("metadados obrigatórios");
  }

  /** Valida o ZIP completo produzido pela matriz quando seu caminho é informado. */
  @Test
  void acceptsGeneratedPackageWhenProvided() throws Exception {
    String archivePath = System.getProperty("approved.package.path");
    Assumptions.assumeTrue(archivePath != null && !archivePath.isBlank());

    var validated =
        new ApprovedCreativePackageArchive(new ObjectMapper())
            .validate(Files.readAllBytes(Path.of(archivePath)), 4L);

    assertThat(validated.assets()).hasSize(11);
    assertThat(validated.directionExecution().executionId()).isNotBlank();
    assertThat(validated.apolloExecution().executionId()).isNotBlank();
    assertThat(validated.psiqueExecution().executionId()).isNotBlank();
    assertThat(validated.temisExecution().executionId()).isNotBlank();
  }

  /** Monta o arquivo ZIP mínimo que cumpre o contrato de produção e revisão. */
  static byte[] validPackageBytes() throws Exception {
    return zip(entries());
  }

  /** Prepara todos os arquivos e metadados referenciados pelo pacote de teste. */
  private static Map<String, byte[]> entries() throws Exception {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    entries.put("proof/proof.png", PROOF);
    entries.put("assets/final.png", ASSET);
    entries.put("review-frames/frame.png", FRAME);
    entries.put("channel-previews/preview.png", PREVIEW);
    for (String execution :
        new String[] {
          "direction-execution", "apollo-execution", "psique-execution", "temis-execution"
        }) {
      entries.put("audit/" + execution + "/request.md", REQUEST);
      entries.put("audit/" + execution + "/agent-prompt.md", AGENT_PROMPT);
      entries.put("audit/" + execution + "/activity-prompt.md", ACTIVITY_PROMPT);
      entries.put("audit/" + execution + "/response.json", AUDIT);
      entries.put("audit/" + execution + "/process.jsonl", AUDIT);
    }
    entries.put(
        "metadata/contract.json",
        bytes(
            """
            {"contractVersion":"v1","product":{"commercialPlanId":4,"experimentId":89},
             "routeDecision":{"selected":"STATIC"},
             "sourceProofs":[{"file":"proof.png"}],
             "formats":[{"files":["final.png"]}]}
            """));
    entries.put(
        "metadata/manifest.json",
        bytes(
            """
            {"contractVersion":"v1","producerExecutionId":"producer-execution",
             "externalMediaProviderCalled":false,"externalMediaCostUsd":0,"published":false,
             "sourceProofs":[{"file":"proof.png","purpose":"PRODUCT_PROOF","sha256":"%s"}],
             "assets":[{"file":"final.png","mediaType":"IMAGE","purposes":["ADS"],"sha256":"%s"}],
             "reviewFrames":[{"file":"frame.png","sha256":"%s"}],
             "channelPreviews":[{"file":"preview.png","sha256":"%s"}],
             "destinationEvidence":{"screenshots":[]}}
            """
                .formatted(sha256(PROOF), sha256(ASSET), sha256(FRAME), sha256(PREVIEW))));
    entries.put(
        "metadata/iris-direction.json",
        bytes("{\"decision\":\"SELECTED\",\"chosenRoute\":\"STATIC\"}"));
    entries.put("metadata/apollo-storyboard.json", bytes("{\"cuts\":[{}]}"));
    entries.put(
        "metadata/psique-review.json", bytes("{\"decision\":\"APPROVED\",\"requiredChanges\":[]}"));
    entries.put(
        "metadata/temis-review.json", bytes("{\"decision\":\"APPROVED\",\"requiredChanges\":[]}"));
    entries.put("metadata/agent-executions.json", bytes(executionsJson()));
    return entries;
  }

  /** Cria o ledger com execuções distintas e requests, responses e logs íntegros. */
  private static String executionsJson() throws Exception {
    String template =
        """
        {"executionId":"%s","agent":"%s","exitCode":0,"agentModelCalled":true,
         "model":"gpt-5.6-sol","reasoningEffort":"high","usage":{"input_tokens":100,"cached_input_tokens":20,"output_tokens":10},
         "requestFile":"audit/%s/request.md","requestFileSha256":"%s",
         "agentPromptFile":"audit/%s/agent-prompt.md","agentPromptFileSha256":"%s",
         "activityPromptFile":"audit/%s/activity-prompt.md","activityPromptFileSha256":"%s",
         "responseFile":"audit/%s/response.json","responseFileSha256":"%s",
         "logFile":"audit/%s/process.jsonl","logFileSha256":"%s"}
        """;
    String requestHash = sha256(REQUEST);
    String agentPromptHash = sha256(AGENT_PROMPT);
    String activityPromptHash = sha256(ACTIVITY_PROMPT);
    String auditHash = sha256(AUDIT);
    return "["
        + executionJson(
            template,
            "direction-execution",
            "IRIS",
            requestHash,
            agentPromptHash,
            activityPromptHash,
            auditHash)
        + ","
        + executionJson(
            template,
            "apollo-execution",
            "APOLLO",
            requestHash,
            agentPromptHash,
            activityPromptHash,
            auditHash)
        + ","
        + executionJson(
            template,
            "psique-execution",
            "PSIQUE",
            requestHash,
            agentPromptHash,
            activityPromptHash,
            auditHash)
        + ","
        + executionJson(
            template,
            "temis-execution",
            "TEMIS_INDEPENDENT",
            requestHash,
            agentPromptHash,
            activityPromptHash,
            auditHash)
        + "]";
  }

  /** Preenche uma execução do ledger sem ocultar os prompts e artefatos brutos. */
  private static String executionJson(
      String template,
      String executionId,
      String agent,
      String requestHash,
      String agentPromptHash,
      String activityPromptHash,
      String auditHash) {
    return template.formatted(
        executionId,
        agent,
        executionId,
        requestHash,
        executionId,
        agentPromptHash,
        executionId,
        activityPromptHash,
        executionId,
        auditHash,
        executionId,
        auditHash);
  }

  /** Compacta as entradas em memória para exercitar a mesma leitura usada pelo endpoint. */
  private static byte[] zip(Map<String, byte[]> entries) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        zip.putNextEntry(new ZipEntry(entry.getKey()));
        zip.write(entry.getValue());
        zip.closeEntry();
      }
    }
    return output.toByteArray();
  }

  /** Codifica um documento textual em UTF-8. */
  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  /** Calcula o hash canônico usado pelo manifesto de teste. */
  private static String sha256(byte[] value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
  }
}
