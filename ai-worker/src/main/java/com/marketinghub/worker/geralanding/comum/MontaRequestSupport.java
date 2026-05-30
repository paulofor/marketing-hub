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

    /** Resolve placeholders simples e reprocessa o texto até não haver referências encadeadas. */
    private static String resolverPlaceholders(String template, Map<String, Object> dadosPayload, ObjectMapper objectMapper)
            throws IOException {
        return resolverPlaceholders(template, dadosPayload, objectMapper, new LinkedHashSet<>());
    }

    /** Resolve placeholders preservando a pilha de prompts para bloquear referências circulares. */
    private static String resolverPlaceholders(String template,
                                               Map<String, Object> dadosPayload,
                                               ObjectMapper objectMapper,
                                               Set<String> stack) throws IOException {
        String resolved = template;
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
                String replacement = resolveTypedPlaceholder(tipo, nome, dadosPayload, objectMapper, stack);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(buffer);
            resolved = resolverMustachePlaceholders(buffer.toString(), dadosPayload, objectMapper, stack);
            if (found) {
                pending.push(resolved);
            }
        }
        return resolved;
    }

    /** Resolve placeholders no formato mustache, incluindo aliases {{prompt-*}} e {{dados-*}}. */
    private static String resolverMustachePlaceholders(String template,
                                                       Map<String, Object> dadosPayload,
                                                       ObjectMapper objectMapper,
                                                       Set<String> stack)
            throws IOException {
        Matcher matcher = MUSTACHE_PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String nome = matcher.group(1);
            String replacement = resolveMustachePlaceholder(nome, dadosPayload, objectMapper, stack);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /** Resolve um placeholder mustache como dado direto ou como alias prefixado por prompt-/dados-. */
    private static String resolveMustachePlaceholder(String nome,
                                                     Map<String, Object> dadosPayload,
                                                     ObjectMapper objectMapper,
                                                     Set<String> stack) throws IOException {
        if (nome != null && nome.startsWith("prompt-")) {
            return resolveTypedPlaceholder("prompt", nome.substring("prompt-".length()), dadosPayload, objectMapper, stack);
        }
        if (nome != null && nome.startsWith("dados-")) {
            return resolveTypedPlaceholder("dados", nome.substring("dados-".length()), dadosPayload, objectMapper, stack);
        }
        return renderPlaceholderValue(dadosPayload.get(nome), objectMapper);
    }

    /** Resolve o valor de um placeholder tipado, carregando prompts recursivos ou dados do payload. */
    private static String resolveTypedPlaceholder(String tipo,
                                                  String nome,
                                                  Map<String, Object> dadosPayload,
                                                  ObjectMapper objectMapper,
                                                  Set<String> stack) throws IOException {
        if ("prompt".equals(tipo)) {
            String token = tipo + ":" + nome;
            if (!stack.add(token)) {
                throw new IllegalStateException("Referência circular de prompts detectada: " + token);
            }
            try {
                return resolverPlaceholders(carregarPromptBase(nome + ".md"), dadosPayload, objectMapper, stack);
            } finally {
                stack.remove(token);
            }
        }
        return renderPlaceholderValue(dadosPayload.get(nome), objectMapper);
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
