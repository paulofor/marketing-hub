package com.marketinghub.videomanagement.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.VideoProviderException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar a candidata Codex de Apolo em sandbox sombra sem autoridade externa. */
@Component
public class ApolloCodexShadowClient {
    private static final String PROMPT_PATH = "prompts/apollo/v2/codex-shadow-storyboard.md";
    private static final String SCHEMA_PATH = "prompts/apollo/v2/storyboard-planner-schema.json";
    private final Logger log = LoggerFactory.getLogger(ApolloCodexShadowClient.class);
    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;

    /** Recebe somente configuração e serializador necessários ao processo isolado. */
    public ApolloCodexShadowClient(VideoManagementProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Produz um storyboard candidato sem provider, publicação, MCP ou autorização financeira. */
    public CodexShadowResult plan(Long jobId, JsonNode frozenMetadata, JsonNode apiBaseline) {
        VideoManagementProperties.CodexShadow config = properties.getApolloPlanner().getCodexShadow();
        if (!config.isEnabled()) {
            throw blocked("Replay Codex de Apolo está desabilitado; nenhum provider foi chamado.");
        }
        Path output = null;
        Path processLog = null;
        Path schema = null;
        try {
            output = Files.createTempFile("apollo-codex-shadow-", ".json");
            processLog = Files.createTempFile("apollo-codex-shadow-process-", ".log");
            schema = materializeSchema();
            String request = buildPrompt(frozenMetadata, apiBaseline);
            Process process = new ProcessBuilder(command(output, schema))
                    .redirectErrorStream(true)
                    .redirectOutput(processLog.toFile())
                    .start();
            process.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();
            boolean finished = process.waitFor(config.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw blocked("Replay Codex excedeu o timeout; nenhum provider foi chamado.");
            }
            if (process.exitValue() != 0) {
                log.error("Replay Codex de Apolo falhou; jobId={} exitCode={} log={}",
                        jobId, process.exitValue(), safeLog(processLog));
                throw blocked("Replay Codex falhou; nenhum provider foi chamado.");
            }
            String raw = Files.readString(output, StandardCharsets.UTF_8);
            JsonNode plan = objectMapper.readTree(raw);
            log.info("Replay Codex de Apolo concluído; jobId={} model={} shadow=true",
                    jobId, config.getModel());
            return new CodexShadowResult(plan, request, raw, config.getModel(), true, false, false);
        } catch (VideoProviderException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("Falha ao executar replay Codex de Apolo; jobId={}", jobId, ex);
            throw blocked("Replay Codex indisponível; nenhum provider foi chamado.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Replay Codex de Apolo interrompido; jobId={}", jobId, ex);
            throw blocked("Replay Codex interrompido; nenhum provider foi chamado.", ex);
        } finally {
            delete(output);
            delete(processLog);
            delete(schema);
        }
    }

    /** Monta o comando Codex sem pesquisa, MCP, escrita ou aprovação interativa. */
    List<String> command(Path output, Path schema) {
        VideoManagementProperties.CodexShadow config = properties.getApolloPlanner().getCodexShadow();
        List<String> command = new ArrayList<>(List.of(
                config.getCommand(), "exec", "-", "--skip-git-repo-check", "--sandbox", "read-only",
                "--cd", config.getWorkingDirectory(), "--output-schema", schema.toString(),
                "--output-last-message", output.toString(), "--color", "never",
                "--config", "approval_policy=\"never\"", "--model", config.getModel()));
        if (config.getReasoningEffort() != null && !config.getReasoningEffort().isBlank()) {
            command.addAll(List.of("--config",
                    "model_reasoning_effort=\"" + config.getReasoningEffort() + "\""));
        }
        return command;
    }

    /** Resolve o prompt versionado exclusivamente com o snapshot congelado do replay. */
    private String buildPrompt(JsonNode metadata, JsonNode baseline) throws IOException {
        return resource(PROMPT_PATH)
                .replace("{{FROZEN_METADATA}}", objectMapper.writeValueAsString(metadata))
                .replace("{{API_BASELINE}}", objectMapper.writeValueAsString(baseline));
    }

    /** Materializa o schema versionado em arquivo temporário exigido pelo Codex. */
    private Path materializeSchema() throws IOException {
        Path path = Files.createTempFile("apollo-storyboard-schema-", ".json");
        Files.writeString(path, resource(SCHEMA_PATH), StandardCharsets.UTF_8);
        return path;
    }

    /** Lê integralmente um recurso obrigatório do classpath. */
    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Recurso ausente: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Limita o log operacional para evitar payload ilimitado em falhas do processo. */
    private String safeLog(Path path) {
        try {
            String value = Files.readString(path, StandardCharsets.UTF_8);
            return value.substring(0, Math.min(value.length(), 4000));
        } catch (IOException ex) {
            log.error("Falha ao ler log do replay Codex de Apolo; arquivo={}", path, ex);
            return "log indisponível";
        }
    }

    /** Remove artefatos temporários sem mascarar o resultado principal. */
    private void delete(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Falha ao remover artefato temporário do replay Codex; arquivo={}", path, ex);
        }
    }

    /** Cria bloqueio funcional que jamais deve ser convertido em geração de vídeo. */
    private VideoProviderException blocked(String message) {
        return new VideoProviderException("APOLLO_CODEX_SHADOW_BLOCKED", message);
    }

    /** Preserva a causa completa da falha local do runner. */
    private VideoProviderException blocked(String message, Throwable cause) {
        return new VideoProviderException("APOLLO_CODEX_SHADOW_BLOCKED", message, cause);
    }

    /** Preserva plano e auditoria sem alegar gasto, provider ou publicação. */
    public record CodexShadowResult(JsonNode plan, String request, String rawResponse, String model,
                                    boolean shadowMode, boolean providerCalled, boolean spendingAuthorized) {}
}
