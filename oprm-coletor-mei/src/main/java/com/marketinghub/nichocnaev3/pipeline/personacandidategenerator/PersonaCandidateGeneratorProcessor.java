package com.marketinghub.nichocnaev3.pipeline.personacandidategenerator;

import com.marketinghub.nichocnaev3.pipeline.StageArtifact;
import com.marketinghub.nichocnaev3.pipeline.StageContext;
import com.marketinghub.nichocnaev3.pipeline.StageProcessor;
import com.marketinghub.nichocnaev3.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa persona-candidate-generator criando personas operacionais candidatas com apoio da OpenAI. */
public final class PersonaCandidateGeneratorProcessor implements StageProcessor {
    private static final String STAGE_CODE = "persona-candidate-generator";
    private static final String STATUS = "PERSONAS_CANDIDATAS";
    private final PersonaCandidateGenerationClient generationClient;

    /** Inicializa o processor com o cliente responsável por acessar a OpenAI. */
    public PersonaCandidateGeneratorProcessor(PersonaCandidateGenerationClient generationClient) {
        this.generationClient = generationClient;
    }

    /** Executa a etapa persona-candidate-generator produzindo personas candidatas estruturadas para priorização. */
    @Override
    public StageResult process(StageContext context) {
        String cnaeCode = text(context.input().get("cnaeCode"));
        String cnaeDescription = resolveCnaeDescription(cnaeCode, context.input());
        PersonaCandidateGenerationRequest request = new PersonaCandidateGenerationRequest(
                context.jobId(),
                context.stageExecutionId(),
                cnaeCode,
                cnaeDescription,
                context.input());
        Map<String, Object> output = new LinkedHashMap<>(generationClient.generate(request));
        completeTechnicalFields(context, output, cnaeCode, cnaeDescription);
        validateFunctionalOutput(output);
        return new StageResult(STATUS, output, List.of(new StageArtifact(STATUS, "inline://nichocnae-v3/persona-candidate-generator", "Personas candidatas geradas pela OpenAI para CNAE " + cnaeCode + ".")));
    }

    /** Completa campos técnicos controlados pelo executor para preservar rastreabilidade e avanço canônico. */
    private void completeTechnicalFields(StageContext context, Map<String, Object> output, String cnaeCode, String cnaeDescription) {
        output.put("stage", STAGE_CODE);
        output.put("status", STATUS);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("cnaeCode", cnaeCode);
        output.put("cnaeDescription", cnaeDescription);
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "persona-tournament");
    }

    /** Bloqueia resposta que ainda pareça técnica ou sem personas reais. */
    private void validateFunctionalOutput(Map<String, Object> output) {
        Object personas = output.get("candidatePersonas");
        if (!(personas instanceof List<?> list) || list.size() < 3) {
            throw new IllegalStateException("OpenAI não retornou personas candidatas suficientes para persona-candidate-generator.");
        }
        output.put("personaCount", list.size());
    }

    /** Resolve a descrição do CNAE usando contexto persistido ou fallback seguro por código. */
    private String resolveCnaeDescription(String cnaeCode, Map<String, Object> input) {
        String explicit = firstText(input, "cnaeDescription", "marketDescription", "description");
        if (!explicit.isBlank()) {
            return explicit;
        }
        if ("4781400".equals(onlyDigits(cnaeCode))) {
            return "Comércio varejista de artigos do vestuário";
        }
        return cnaeCode.isBlank() ? "atividade CNAE informada" : "CNAE " + cnaeCode;
    }

    /** Busca o primeiro campo textual relevante no mapa de entrada. */
    private String firstText(Map<String, Object> input, String... keys) {
        for (String key : keys) {
            String value = text(input.get(key));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    /** Converte valores opcionais em texto seguro. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** Mantém apenas dígitos para comparar códigos CNAE formatados ou não. */
    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
