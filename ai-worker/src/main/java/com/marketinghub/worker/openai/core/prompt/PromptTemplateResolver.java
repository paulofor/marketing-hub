package com.marketinghub.worker.openai.core.prompt;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Responsabilidade: resolver placeholders reutilizáveis em templates de prompt do core OpenAI. */
public class PromptTemplateResolver {

    private static final Pattern PREFIXED_PLACEHOLDER_PATTERN = Pattern.compile(
            "\\{\\{(prompt|dados)-([A-Za-z0-9_-]+)}}|\\{(prompt|dados)-([A-Za-z0-9_-]+)}"
    );
    private static final Pattern MUSTACHE_PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([A-Za-z0-9_\\-.]+)}}");

    private final Function<String, String> promptLoader;
    private final Function<Object, String> valueRenderer;

    /** Inicializa o resolvedor com funções externas para carregar prompts e renderizar valores. */
    public PromptTemplateResolver(Function<String, String> promptLoader, Function<Object, String> valueRenderer) {
        this.promptLoader = Objects.requireNonNull(promptLoader, "promptLoader must not be null");
        this.valueRenderer = Objects.requireNonNull(valueRenderer, "valueRenderer must not be null");
    }

    /** Resolve o template informado usando dados do job e o caminho do recurso atual como base. */
    public String resolve(String template, Map<String, Object> data, String currentResourcePath) {
        Map<String, Object> safeData = data == null ? Map.of() : data;
        String result = resolvePrefixedPlaceholders(template, safeData, currentResourcePath);

        for (Map.Entry<String, Object> entry : safeData.entrySet()) {
            String key = entry.getKey();
            String value = valueRenderer.apply(entry.getValue());

            result = result.replace("{{" + key + "}}", value);
            result = result.replace("${" + key + "}", value);
        }

        return resolveMustachePlaceholders(result, safeData);
    }

    /** Resolve placeholders prefixados `{dados-*}`, `{{dados-*}}`, `{prompt-*}` e `{{prompt-*}}`. */
    private String resolvePrefixedPlaceholders(String template, Map<String, Object> data, String currentResourcePath) {
        String result = template == null ? "" : template;
        boolean found;
        do {
            Matcher matcher = PREFIXED_PLACEHOLDER_PATTERN.matcher(result);
            StringBuffer buffer = new StringBuffer();
            found = false;
            while (matcher.find()) {
                found = true;
                String type = firstNonNull(matcher.group(1), matcher.group(3));
                String name = firstNonNull(matcher.group(2), matcher.group(4));
                String replacement = resolvePrefixedValue(type, name, data, currentResourcePath);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(buffer);
            result = buffer.toString();
        } while (found);
        return result;
    }

    /** Resolve o valor prefixado como dado do job ou como inclusão de outro arquivo markdown. */
    private String resolvePrefixedValue(String type, String name, Map<String, Object> data, String currentResourcePath) {
        if ("prompt".equals(type)) {
            String includedResourcePath = siblingResourcePath(currentResourcePath, name + ".md");
            return resolve(promptLoader.apply(includedResourcePath), data, includedResourcePath);
        }
        return valueRenderer.apply(data.get(name));
    }

    /** Resolve placeholders mustache diretos que ainda restarem no template. */
    private String resolveMustachePlaceholders(String template, Map<String, Object> data) {
        Matcher matcher = MUSTACHE_PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = data.get(key);
            if (value == null) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(valueRenderer.apply(value)));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /** Retorna o primeiro texto não nulo entre grupos alternativos de regex. */
    private String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    /** Monta o caminho de um recurso incluído no mesmo diretório do prompt atual. */
    private String siblingResourcePath(String currentResourcePath, String includedFileName) {
        if (currentResourcePath == null) {
            return includedFileName;
        }
        int lastSlash = currentResourcePath.lastIndexOf('/');
        if (lastSlash < 0) {
            return includedFileName;
        }
        return currentResourcePath.substring(0, lastSlash + 1) + includedFileName;
    }
}
