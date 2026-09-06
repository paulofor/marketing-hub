package com.marketinghub.metaadapproverworker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir que produção visual e revisão comercial voltem ao mesmo container. */
class TemisContainerIsolationContractTest {

  /** Garante serviços, credenciais e recursos segregados no Compose versionado. */
  @Test
  void segregatesRuntimeCapabilitiesAndSecrets() throws Exception {
    String compose = Files.readString(Path.of("docker-compose.yml"));
    String reviewer =
        compose.substring(
            compose.indexOf("  meta-ad-approver-worker:"), compose.indexOf("  iris-image-studio:"));
    String studio = compose.substring(compose.indexOf("  iris-image-studio:"));

    assertThat(reviewer)
        .contains("TEMIS_EXECUTION_ROLE: review", "META_AD_APPROVER_CODEX_HOME")
        .doesNotContain("OPENAI_API_KEY", "/run/secrets/openai_api_key");
    assertThat(studio)
        .contains(
            "IRIS_EXECUTION_ROLE: image-studio",
            "Dockerfile.image-studio",
            "OPENAI_API_KEY_FILE",
            "IRIS_IMAGE_BACKEND_READ_TIMEOUT",
            "IRIS_IMAGE_STUDIO_PENDING_LIMIT: ${IRIS_IMAGE_STUDIO_PENDING_LIMIT:-1}",
            "mem_limit: 2g",
            "cpus: 2.0")
        .doesNotContain("CODEX_HOME", "MARKETING_HUB_REPOSITORY");
  }

  /** Garante que a imagem produtora não instale ferramentas exclusivas do revisor. */
  @Test
  void keepsImageStudioRuntimeSlimAndUnprivileged() throws Exception {
    String dockerfile = Files.readString(Path.of("Dockerfile.image-studio"));

    assertThat(dockerfile)
        .contains("USER image-studio", "ENTRYPOINT [\"java\"")
        .doesNotContain("nodejs", "npm", "codex", "playwright", "agent-health-report");
  }

  /** Garante Java, navegador e decoder pinados sem depender do espelho APT. */
  @Test
  void usesBundledHeadlessShellAndStaticVideoDecoder() throws Exception {
    String dockerfile = Files.readString(Path.of("Dockerfile"));
    String mcp = Files.readString(Path.of("src/main/resources/mcp/meta-ad-approver.mjs"));
    String extractor =
        Files.readString(Path.of("src/main/resources/mcp/video-frame-extractor.mjs"));

    assertThat(dockerfile)
        .contains(
            "FROM eclipse-temurin:21-jre-noble AS java-runtime",
            "FROM mwader/static-ffmpeg:7.1.1@sha256:6769881cc02c80d33e387750a8e144d162adfab2775e934dd97899261dda3a0c AS ffmpeg-runtime",
            "FROM mcr.microsoft.com/playwright:v1.49.0-noble",
            "COPY --from=java-runtime /opt/java/openjdk /opt/java/openjdk",
            "COPY --from=ffmpeg-runtime /ffmpeg /usr/local/bin/ffmpeg",
            "COPY --from=ffmpeg-runtime /ffprobe /usr/local/bin/ffprobe",
            "node --version | grep -Eq '^v2[0-9]\\.'",
            "COPY browser-runtime-check.mjs /app/browser-runtime-check.mjs",
            "COPY src/main/resources/mcp/video-frame-extractor.mjs /app/video-frame-extractor.mjs")
        .doesNotContain(
            "apt-get",
            "playwright-core install --with-deps",
            "chmod -R a+rX /ms-playwright",
            "chrome-linux/chrome",
            "/usr/bin/chromium");
    assertThat(mcp)
        .contains("await chromium.launch", "extractRemoteVideoFrames(url, {")
        .doesNotContain("executablePath", "PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH");
    assertThat(extractor)
        .contains(
            "const MAX_VIDEO_BYTES = 64 * 1024 * 1024",
            "const PROCESS_TIMEOUT_MS = 120000",
            "'/usr/local/bin/ffmpeg'",
            "'/usr/local/bin/ffprobe'",
            "return await extractVideoFrames(bytes)",
            "invalidVideoBlocked = true",
            "timeout: PROCESS_TIMEOUT_MS",
            "killSignal: 'SIGKILL'",
            "await rm(directory, { recursive: true, force: true })");
  }

  /** Garante build, health e prova de ausência do segredo no revisor durante o deploy. */
  @Test
  void validatesBothContainersBeforeDeploymentCompletes() throws Exception {
    String workflow =
        Files.readString(Path.of("../.github/workflows/meta-ad-approver-worker-ci.yml"));

    assertThat(workflow)
        .contains(
            "Dockerfile.image-studio",
            "Validate reviewer visual runtime in confinement",
            "/app/browser-runtime-check.mjs",
            "http://127.0.0.1:8097/ops-meta-ad-approver-observability-v1/health",
            "http://127.0.0.1:8098/ops-meta-ad-approver-observability-v1/health",
            "docker compose exec -T iris-image-studio",
            "docker compose exec -T meta-ad-approver-worker node /app/browser-runtime-check.mjs",
            "test ! -e /run/secrets/openai_api_key");
  }

  /** Garante que o contrato operacional do modelo permaneça em recurso versionado. */
  @Test
  void keepsProductionPromptVersionedOutsideJava() throws Exception {
    String client =
        Files.readString(
            Path.of(
                "src/main/java/com/marketinghub/metaadapproverworker/TemisImageStudioOpenAiClient.java"));
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/image-studio/v1/production.md"));

    assertThat(client).contains("prompts/image-studio/v1/production.md");
    assertThat(prompt)
        .contains(
            "{{JOB_PROMPT}}", "{{PURPOSES}}", "{{ASSET_ROLE_CONTRACT}}", "{{EDIT_CONSTRAINT}}")
        .contains("não redesenhe o produto");
  }

  /** Impede que o gate confunda a comunicação de Íris com um entregável de Dédalo. */
  @Test
  void reviewsIrisCommercialAssetsAgainstRealProductProof() throws Exception {
    String prompt =
        Files.readString(Path.of("src/main/resources/prompts/image-studio/v1/review.md"));

    assertThat(prompt)
        .contains(
            "saída deve declarar somente `LANDING`, `ADS` ou `SOCIAL`",
            "referência aprovada `PRODUCT_PROOF` ou `DELIVERY` de Dédalo",
            "`PRODUCT_PROOF` é captura",
            "peça de Íris nunca se torna parte da entrega",
            "homologação sintética",
            "proporção nativa `9:16`",
            "campo `issues` deve ser obrigatoriamente um array vazio",
            "nunca como depoimento ou cliente real");
  }
}
