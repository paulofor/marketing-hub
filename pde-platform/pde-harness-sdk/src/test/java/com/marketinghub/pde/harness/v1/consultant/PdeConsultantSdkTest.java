package com.marketinghub.pde.harness.v1.consultant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marketinghub.pde.harness.v1.PdeHarnessConfiguration;
import com.marketinghub.pde.harness.v1.PdeLocalImageInput;
import com.marketinghub.pde.harness.v1.PdeRunStatus;
import com.marketinghub.pde.harness.v1.internal.PdeHashing;
import com.marketinghub.pde.harness.v1.support.PdeHarnessTestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Homologa o perfil comum de consultores sobre o núcleo seguro do PDE Harness SDK. */
class PdeConsultantSdkTest {
  @TempDir Path temporaryDirectory;

  /** Executa Turmalina com imagem e preserva as partes auditáveis do prompt. */
  @Test
  void executesPwaConsultantWithImageAndPromptParts() throws Exception {
    byte[] png = syntheticPng();
    Path image = temporaryDirectory.resolve("look.png");
    Files.write(image, png);
    PdeConsultantPromptParts promptParts =
        new PdeConsultantPromptParts(
            "Você é Amora, consultora acolhedora e objetiva, sem julgar o corpo da cliente.",
            "amora-agent-v1",
            "Analise a adequação do look à ocasião e proponha somente um ajuste prioritário.",
            "look-review-v1",
            "Tenho uma reunião hoje. Este look funciona?");
    PdeConsultantTurnRequest request =
        new PdeConsultantTurnRequest(
            PdeConsultantChannel.PWA,
            PdeHarnessTestSupport.context("cliente-a", "conversa-a", "look-review", "interacao-1"),
            PdeHarnessTestSupport.emptyMemory("cliente-a"),
            "gpt-test",
            promptParts,
            PdeConsultantOutputSchemas.defaultV1(),
            PdeConsultantOutputSchemas.DEFAULT_V1_VERSION,
            List.of(
                new PdeLocalImageInput("look-atual", image, "image/png", PdeHashing.sha256(png))),
            null,
            false);

    try (PdeConsultantSdk sdk = new PdeConsultantSdk(configuration("consultant-aware"))) {
      PdeConsultantRunResult result = sdk.execute(request);

      assertEquals(PdeRunStatus.COMPLETED, result.agentRun().status());
      assertEquals("AI_PWA_CONSULTANT_PRODUCT", result.productTypeCode());
      assertEquals(promptParts.agentPart(), result.agentPrompt());
      assertEquals(promptParts.activityPart(), result.activityPrompt());
      assertTrue(result.completePrompt().contains("# Parte do agente"));
      assertTrue(result.completePrompt().contains("# Parte da atividade"));
      assertTrue(result.completePromptVersion().contains("amora-agent-v1"));
      assertEquals(
          "Imagem privada recebida.",
          result.agentRun().structuredOutput().path("recommendation").asText());
      assertEquals(
          "Prompt dividido e auditável.",
          result.agentRun().structuredOutput().path("why").asText());
    }
  }

  /** Mapeia Fluorita ao código histórico sem exigir uma camada React no canal. */
  @Test
  void preservesWhatsappCanonicalIdentity() {
    assertEquals(
        "AI_SANDBOX_CONVERSATIONAL_PRODUCT", PdeConsultantChannel.WHATSAPP.productTypeCode());
    assertFalse(PdeConsultantChannel.WHATSAPP.reactExperienceRequired());
    assertTrue(PdeConsultantChannel.PWA.reactExperienceRequired());
  }

  /** Bloqueia um turno sem parte do agente antes de construir qualquer execução. */
  @Test
  void rejectsConsultantWithoutAgentPrompt() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PdeConsultantPromptParts(
                " ", "agent-v1", "Atividade suficientemente descrita.", "activity-v1", "Olá"));
  }

  /** Cria o transporte falso segregado usado somente pela homologação local. */
  private PdeHarnessConfiguration configuration(String mode) {
    return PdeHarnessTestSupport.configuration(
        temporaryDirectory.resolve("codex-home"),
        temporaryDirectory.resolve("workspaces"),
        mode,
        Duration.ofSeconds(2));
  }

  /** Gera bytes mínimos com assinatura PNG sem incorporar foto de uma pessoa real. */
  private byte[] syntheticPng() {
    return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x11, 0x12, 0x13};
  }
}
