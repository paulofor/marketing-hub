package com.marketinghub.worker.geralanding.comum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;

/**
 * Responsabilidade: concentrar utilitários de resolução de prompt e schema usados pelos montadores de request do GeraLanding.
 */
public final class MontaRequestSupport {

    private static final String GERALANDING_PROMPT_BASE_PATH = "prompts/geralanding/";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(prompt|dados)-([a-zA-Z0-9_-]+)}");
    private static final Pattern MUSTACHE_PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([A-Za-z0-9_\\-.]+)}}");

    private MontaRequestSupport() {
    }

    /** Carrega o markdown bruto do prompt da etapa diretamente do classpath. */
    public static String carregarPromptMarkdownCru(String promptFileName) throws IOException {
        return carregarPromptBase(promptFileName);
    }

    /** Resolve placeholders de prompt e dados para montar o texto final enviado ao modelo. */
    public static String montarPrompt(String stageCode, String promptFileName, Map<String, Object> dadosPayload, ObjectMapper objectMapper)
            throws IOException {
        String template = carregarPromptBase(promptFileName);
        String resolvedPrompt = resolverPlaceholders(template, dadosPayload, objectMapper);
        String etapaNormalizada = stageCode == null ? "desconhecida" : stageCode.trim();
        return """
                # Tarefa
                Você deve executar a etapa `%s` do pipeline de landing page e responder estritamente no formato solicitado.

                # Instruções do usuário
                %s
                """.formatted(etapaNormalizada, resolvedPrompt == null ? "" : resolvedPrompt.trim());
    }

    /** Carrega e converte para mapa o schema JSON da etapa. */
    public static Map<String, Object> carregarSchema(ObjectMapper objectMapper, String schemaPath) throws JsonProcessingException {
        try {
            return objectMapper.readValue(new ClassPathResource(schemaPath).getInputStream(), Map.class);
        } catch (IOException ex) {
            throw new JsonProcessingException("Falha ao carregar schema: " + schemaPath) {
            };
        }
    }

    private static String resolverPlaceholders(String template, Map<String, Object> dadosPayload, ObjectMapper objectMapper) throws IOException {
        String resolved = template;
        Set<String> stack = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.push(template);
        while (!pending.isEmpty()) {
            String current = pending.pop();
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(current);
            StringBuffer buffer = new StringBuffer();
            boolean found = false;
            while (matcher.find()) {
                found = true;
                String tipo = matcher.group(1);
                String nome = matcher.group(2);
                String replacement;
                if ("prompt".equals(tipo)) {
                    String token = tipo + ":" + nome;
                    if (!stack.add(token)) {
                        throw new IllegalStateException("Referência circular de prompts detectada: " + token);
                    }
                    replacement = resolverPlaceholders(carregarPromptBase(nome + ".md"), dadosPayload, objectMapper);
                    stack.remove(token);
                } else {
                    replacement = renderPlaceholderValue(dadosPayload.get(nome), objectMapper);
                }
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(buffer);
            resolved = resolverMustachePlaceholders(buffer.toString(), dadosPayload, objectMapper);
            if (found) {
                pending.push(resolved);
            }
        }
        return resolved;
    }

    private static String resolverMustachePlaceholders(String template, Map<String, Object> dadosPayload, ObjectMapper objectMapper)
            throws IOException {
        Matcher matcher = MUSTACHE_PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String nome = matcher.group(1);
            String replacement = renderPlaceholderValue(dadosPayload.get(nome), objectMapper);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String renderPlaceholderValue(Object dado, ObjectMapper objectMapper) throws JsonProcessingException {
        if (dado == null) {
            return "";
        }
        if (dado instanceof String valor) {
            return valor;
        }
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dado);
    }

    private static String carregarPromptBase(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(GERALANDING_PROMPT_BASE_PATH + fileName);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Prompt não encontrado em geralanding: " + fileName);
        }
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
