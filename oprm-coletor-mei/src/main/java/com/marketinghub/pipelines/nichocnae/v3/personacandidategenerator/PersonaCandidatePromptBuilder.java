package com.marketinghub.pipelines.nichocnae.v3.personacandidategenerator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Carrega o prompt versionado e injeta o contexto persistido da etapa de personas. */
@Component
public class PersonaCandidatePromptBuilder {
    private static final String PROMPT_PATH = "prompts/nichocnaev3/persona-candidate-generator.md";

    /** Monta o prompt final a partir do arquivo versionado no classpath. */
    public String build(PersonaCandidateGenerationRequest request) {
        return loadPrompt()
                + "\n\nContexto persistido:\n"
                + "jobId: " + request.jobId() + "\n"
                + "stageExecutionId: " + request.stageExecutionId() + "\n"
                + "cnaeCode: " + request.cnaeCode() + "\n"
                + "cnaeDescription: " + request.cnaeDescription() + "\n"
                + "inputKeys: " + request.input().keySet() + "\n"
                + "\nRetorne somente JSON aderente ao schema. Gere 4 personas candidatas operacionais, descrevendo contexto de atuação, fluxo do dia, tarefas recorrentes, interações, ferramentas, registros, decisões pequenas e variações de rotina, sem criar dor, oferta, campanha, promessa, preço, checkout, landing page, produto ou solução.";
    }

    /** Lê o prompt operacional do recurso versionado. */
    private String loadPrompt() {
        try {
            return new String(new ClassPathResource(PROMPT_PATH).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Falha ao carregar prompt da etapa persona-candidate-generator.", ex);
        }
    }
}
