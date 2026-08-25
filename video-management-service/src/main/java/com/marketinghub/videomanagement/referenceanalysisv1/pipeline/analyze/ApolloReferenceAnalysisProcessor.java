package com.marketinghub.videomanagement.referenceanalysisv1.pipeline.analyze;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageContext;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageProcessor;
import com.marketinghub.videomanagement.referenceanalysisv1.pipeline.ReferenceAnalysisStageResult;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Transforma evidências determinísticas e leitura multimodal em uma receita executável por Apolo. */
@Component
public class ApolloReferenceAnalysisProcessor implements ReferenceAnalysisStageProcessor {
    private static final Logger log = LoggerFactory.getLogger(ApolloReferenceAnalysisProcessor.class);
    private final VideoManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final ReferenceMediaInspector inspector;
    private final ReferenceAnalysisAiClient aiClient;

    /** Inicializa a etapa concreta com inspeção local e integração multimodal isoladas. */
    public ApolloReferenceAnalysisProcessor(VideoManagementProperties properties,
                                            ObjectMapper objectMapper,
                                            ReferenceMediaInspector inspector,
                                            ReferenceAnalysisAiClient aiClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.inspector = inspector;
        this.aiClient = aiClient;
    }

    /** Executa inspeção, IA, validação determinística e consolidação do relatório. */
    @Override
    public ReferenceAnalysisStageResult process(ReferenceAnalysisStageContext context) {
        ReferenceMediaInspector.Evidence evidence = inspector.inspect(context);
        ReferenceAnalysisAiClient.AiInteraction interaction;
        try {
            interaction = aiClient.analyze(context, evidence);
        } catch (ReferenceAnalysisAiClient.AiFailure ex) {
            log.error("Falha multimodal de Apolo; executionId={} referenceId={}",
                    context.executionId(), context.referenceId(), ex);
            throw new ReferenceAnalysisFailureException(
                    "A leitura multimodal de Apolo falhou",
                    ex,
                    evidence.artifacts(),
                    ex.request(),
                    ex.response(),
                    properties.getReferenceAnalysis().getModel());
        }
        JsonNode output;
        try {
            output = extractOutput(interaction.response());
            validate(output);
        } catch (RuntimeException ex) {
            log.error("Resposta multimodal inválida de Apolo; executionId={} referenceId={}",
                    context.executionId(), context.referenceId(), ex);
            throw new ReferenceAnalysisFailureException(
                    "A resposta multimodal de Apolo não cumpriu o contrato",
                    ex,
                    evidence.artifacts(),
                    interaction.request(),
                    interaction.response(),
                    properties.getReferenceAnalysis().getModel());
        }
        JsonNode usage = interaction.response().path("usage");
        Long inputTokens = requiredUsage(usage, "input_tokens");
        Long outputTokens = requiredUsage(usage, "output_tokens");
        BigDecimal costUsd = conservativeCost(inputTokens, outputTokens);
        ObjectNode auditArtifacts = evidence.artifacts().deepCopy();
        ObjectNode costEvidence = auditArtifacts.putObject("costEstimate");
        costEvidence.put("usd", costUsd);
        costEvidence.put("method", "GPT_5_6_STANDARD_UPPER_BOUND");
        costEvidence.put("serviceTier", "flex");
        costEvidence.put("inputTokensChargedAtFullRate", inputTokens);
        costEvidence.put("outputTokens", outputTokens);
        return new ReferenceAnalysisStageResult(
                summary(context, auditArtifacts, output),
                output,
                auditArtifacts,
                interaction.request(),
                interaction.response(),
                properties.getReferenceAnalysis().getModel(),
                inputTokens,
                nullableLong(usage.path("input_tokens_details"), "cached_tokens"),
                outputTokens,
                costUsd,
                output.path("operationalDecision").asText());
    }

    /** Calcula um teto conservador sem descontar cache ou Flex, evitando subestimar o gasto. */
    private BigDecimal conservativeCost(long inputTokens, long outputTokens) {
        BigDecimal input = properties.getReferenceAnalysis().getInputPricePerMillionUsd()
                .multiply(BigDecimal.valueOf(inputTokens));
        BigDecimal output = properties.getReferenceAnalysis().getOutputPricePerMillionUsd()
                .multiply(BigDecimal.valueOf(outputTokens));
        return input.add(output).divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }

    /** Exige usage para que nenhuma análise concluída permaneça com custo desconhecido. */
    private Long requiredUsage(JsonNode usage, String field) {
        Long value = nullableLong(usage, field);
        if (value == null || value < 0) {
            throw new IllegalStateException("OpenAI não informou " + field + " para auditar o custo");
        }
        return value;
    }

    /** Extrai o texto estruturado da Responses API e rejeita resposta incompleta. */
    private JsonNode extractOutput(JsonNode response) {
        if (response != null) {
            for (JsonNode output : response.path("output")) {
                for (JsonNode content : output.path("content")) {
                    if ("output_text".equals(content.path("type").asText())) {
                        try {
                            return objectMapper.readTree(content.path("text").asText());
                        } catch (Exception ex) {
                            log.error("Não foi possível interpretar a resposta estruturada da análise de referência", ex);
                            throw new IllegalStateException("A resposta estruturada de Apolo é inválida", ex);
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("Apolo não retornou análise estruturada");
    }

    /** Reforça gates que não podem depender apenas do schema ou da interpretação do modelo. */
    private void validate(JsonNode output) {
        if (!StringUtils.hasText(output.path("commercialDiagnosis").asText())
                || output.path("sequence").size() < 4
                || output.path("reusableLearnings").size() < 3
                || output.path("productionBlueprint").path("scenePlan").size() < 4) {
            throw new IllegalStateException("Análise não contém evidência e receita suficientes para produção");
        }
        String capability = output.path("productionBlueprint").path("apolloCapability").asText();
        if (!("CURRENT".equals(capability) || "EXTEND_APOLLO".equals(capability))) {
            throw new IllegalStateException("A referência não justificou um papel diferente da direção criativa de Apolo");
        }
        if (output.path("rightsRisks").isMissingNode()) {
            throw new IllegalStateException("Análise não avaliou riscos de imagem, marca, voz, música e conteúdo");
        }
    }

    /** Monta Markdown compatível com a visão histórica e enriquecido pela receita estruturada. */
    private String summary(ReferenceAnalysisStageContext context, JsonNode artifacts, JsonNode output) {
        return """
                **Evidências usadas**
                - Arquivo SHA-256: %s
                - Duração: %.2f segundos; resolução: %sx%s; codec: %s; áudio: %s.
                - %s mudanças visuais no limiar %s; loudness integrado: %s LUFS; true peak: %s dBFS.
                - Dois contact sheets, 24 frames-chave e leitura multimodal auditável.

                **Diagnóstico comercial**
                %s

                **Análise por sequência**
                %s

                **O que o sistema deve aprender desse vídeo**
                %s

                **Melhorias acionáveis para usar em vendas**
                Campanha: %s
                Produto: %s
                Orgânico: %s

                **Riscos e limites de direitos**
                %s

                **Decisão operacional**
                %s

                **Capacidade de Apolo**
                %s. Lacunas condicionais: %s

                Analisado por: Apolo / reference-analysis-v1; execução #%s.
                """.formatted(
                artifacts.path("sha256").asText(),
                artifacts.path("durationSeconds").asDouble(),
                artifacts.path("width").asText(),
                artifacts.path("height").asText(),
                artifacts.path("videoCodec").asText(),
                artifacts.path("audioCodec").asText(),
                artifacts.path("sceneChangeCount").asText(),
                artifacts.path("sceneChangeThreshold").asText(),
                artifacts.path("integratedLoudnessLufs").asText(),
                artifacts.path("truePeakDbfs").asText(),
                output.path("commercialDiagnosis").asText(),
                sequenceText(output.path("sequence")),
                lines(output.path("reusableLearnings")),
                output.path("salesApplications").path("campaign").asText(),
                output.path("salesApplications").path("product").asText(),
                output.path("salesApplications").path("organic").asText(),
                lines(output.path("rightsRisks")),
                output.path("operationalDecision").asText(),
                output.path("productionBlueprint").path("apolloCapability").asText(),
                lines(output.path("productionBlueprint").path("capabilityGaps")),
                context.executionId());
    }

    /** Converte a sequência temporal estruturada em linhas legíveis na visão histórica. */
    private String sequenceText(JsonNode sequence) {
        List<String> lines = new ArrayList<>();
        sequence.forEach(item -> lines.add("- %.1fs–%.1fs | %s | %s | %s".formatted(
                item.path("startSeconds").asDouble(), item.path("endSeconds").asDouble(),
                item.path("role").asText(), item.path("visualAction").asText(),
                item.path("editing").asText())));
        return String.join("\n", lines);
    }

    /** Converte uma lista JSON em linhas Markdown sem serialização aninhada. */
    private String lines(JsonNode values) {
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add("- " + value.asText()));
        return String.join("\n", result);
    }

    /** Preserva ausência de contagem quando o provedor não reportar usage. */
    private Long nullableLong(JsonNode node, String field) {
        return node.has(field) && node.path(field).canConvertToLong() ? node.path(field).asLong() : null;
    }
}
