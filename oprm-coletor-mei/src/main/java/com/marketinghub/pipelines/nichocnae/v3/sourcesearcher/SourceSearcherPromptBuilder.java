package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Carrega o prompt versionado e injeta tentativas auditáveis da busca de fontes. */
@Component
public class SourceSearcherPromptBuilder {
    private static final String PROMPT_PATH = "prompts/nichocnaev3/source-searcher.md";

    /** Monta o prompt final com contexto persistido, queries planejadas e candidatos encontrados. */
    public String build(StageContext context, List<Map<String, Object>> plannedQueries, List<Map<String, Object>> searchAttempts) {
        return loadPrompt()
                + "\n\nContexto persistido:\n"
                + "jobId: " + context.jobId() + "\n"
                + "stageExecutionId: " + context.stageExecutionId() + "\n"
                + "cnaeCode: " + context.input().getOrDefault("cnaeCode", "") + "\n"
                + "inputKeys: " + context.input().keySet() + "\n"
                + "\nQueries planejadas:\n" + plannedQueries + "\n"
                + "\nTentativas e fontes candidatas/rejeitadas:\n" + searchAttempts + "\n"
                + "\nRetorne somente JSON aderente ao schema. Não invente URL, título, evidência ou fonte nova.";
    }

    /** Lê o prompt operacional do recurso versionado. */
    private String loadPrompt() {
        try {
            return new String(new ClassPathResource(PROMPT_PATH).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao carregar prompt da etapa source-searcher.", ex);
        }
    }
}
