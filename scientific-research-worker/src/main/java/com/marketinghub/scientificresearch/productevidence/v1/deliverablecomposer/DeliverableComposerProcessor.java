package com.marketinghub.scientificresearch.productevidence.v1.deliverablecomposer;

import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageArtifact;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageCode;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageContext;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageProcessor;
import com.marketinghub.scientificresearch.productevidence.v1.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Compõe entregáveis finais explicáveis a partir da síntese científica.
 */
@Component
public class DeliverableComposerProcessor implements StageProcessor {

    /**
     * Retorna o código canônico da etapa de composição de entregáveis.
     */
    @Override
    public StageCode stageCode() {
        return StageCode.DELIVERABLE_COMPOSER;
    }

    /**
     * Gera um pacote textual pronto para exibição ou ZIP de entregáveis.
     */
    @Override
    public StageResult process(StageContext context) {
        Object synthesis = context.input().get("synthesis");
        String markdown = buildMarkdown(context, synthesis);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("deliverableTitle", "Base científica da proposta");
        output.put("markdown", markdown);
        output.put("recommendedUse", "Usar como explicação educacional e base de transparência do produto.");

        List<StageArtifact> artifacts = List.of(
                new StageArtifact("base-cientifica.md", "text/markdown", markdown),
                new StageArtifact("base-cientifica-deliverable.json", "application/json", output));
        return StageResult.completed(output, artifacts, null);
    }

    /**
     * Monta o markdown final do entregável científico.
     */
    private String buildMarkdown(StageContext context, Object synthesis) {
        return """
                # Base científica da proposta

                **Experimento:** %s

                **Ideia de produto:** %s

                ## O que a ciência permite afirmar

                %s

                ## Como usar isso no produto

                A promessa deve ser apresentada como orientação educacional baseada em evidências, sem prometer resultado garantido, diagnóstico, tratamento ou efeito universal.

                ## Regra de segurança comercial

                Sempre mostrar fontes e limites junto da explicação. Se uma afirmação não estiver sustentada pela síntese científica, ela não deve entrar em página de venda, anúncio ou entregável.
                """.formatted(
                nullToText(context.experimentCode()),
                nullToText(context.productIdea()),
                nullToText(String.valueOf(synthesis)));
    }

    /**
     * Normaliza textos vazios no entregável.
     */
    private String nullToText(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return "Não informado";
        }
        return value;
    }
}
