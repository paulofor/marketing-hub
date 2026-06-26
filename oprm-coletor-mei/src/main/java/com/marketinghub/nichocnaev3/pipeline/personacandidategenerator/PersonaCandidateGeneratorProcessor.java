package com.marketinghub.nichocnaev3.pipeline.personacandidategenerator;

import com.marketinghub.nichocnaev3.pipeline.StageArtifact;
import com.marketinghub.nichocnaev3.pipeline.StageContext;
import com.marketinghub.nichocnaev3.pipeline.StageProcessor;
import com.marketinghub.nichocnaev3.pipeline.StageResult;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Processa a etapa persona-candidate-generator criando personas operacionais candidatas para um CNAE. */
public final class PersonaCandidateGeneratorProcessor implements StageProcessor {
    private static final String STAGE_CODE = "persona-candidate-generator";
    private static final String STATUS = "PERSONAS_CANDIDATAS";

    /** Executa a etapa persona-candidate-generator produzindo personas candidatas estruturadas para priorização. */
    @Override
    public StageResult process(StageContext context) {
        String cnaeCode = text(context.input().get("cnaeCode"));
        String cnaeDescription = resolveCnaeDescription(cnaeCode, context.input());
        String marketLabel = marketLabel(cnaeDescription, cnaeCode);
        List<Map<String, Object>> personas = buildPersonas(cnaeCode, cnaeDescription, marketLabel);

        Map<String, Object> output = baseOutput(context);
        output.put("cnaeCode", cnaeCode);
        output.put("cnaeDescription", cnaeDescription);
        output.put("candidatePersonas", personas);
        output.put("personaCount", personas.size());
        output.put("personaSummary", buildPersonaSummary(personas));
        output.put("routineSummary", buildRoutineSummary(marketLabel));
        output.put("personaDailyTasks", List.of(
                "atender clientes e responder dúvidas recorrentes",
                "organizar estoque, pedidos e reposição",
                "registrar vendas, pagamentos e entregas",
                "divulgar produtos ou agenda em canais digitais"));
        output.put("selectionCriteria", List.of(
                "dor operacional diária",
                "frequência da tarefa",
                "capacidade de pagamento do microempreendedor",
                "potencial de venda futura sem criar oferta nesta etapa"));
        output.put("evidenceLimitations", List.of(
                "hipóteses geradas a partir do CNAE e do contexto persistido",
                "devem ser validadas nas etapas de busca, coleta e extração de sinais"));
        output.put("nextStageCode", "persona-tournament");
        return new StageResult(STATUS, output, List.of(new StageArtifact(STATUS, "inline://nichocnae-v3/persona-candidate-generator", "Geradas " + personas.size() + " personas candidatas para " + marketLabel + ".")));
    }

    /** Monta os campos técnicos comuns preservando rastreabilidade do pipeline. */
    private Map<String, Object> baseOutput(StageContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", STAGE_CODE);
        output.put("status", STATUS);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        return output;
    }

    /** Cria personas candidatas com dores, rotina, tarefas e sinais para o torneio posterior. */
    private List<Map<String, Object>> buildPersonas(String cnaeCode, String cnaeDescription, String marketLabel) {
        return List.of(
                persona("P1", "Dono(a) operador(a) de " + marketLabel, cnaeCode, cnaeDescription, 88,
                        "Pessoa que vende, atende, compra e organiza a operação praticamente sozinha.",
                        List.of("falta de tempo", "retrabalho em atendimento", "controle informal de estoque e pedidos"),
                        List.of("atender clientes", "comprar ou repor itens", "organizar pedidos", "fechar caixa"),
                        List.of("procura atalhos práticos", "decide rápido quando reduz esforço diário", "valoriza linguagem simples")),
                persona("P2", "MEI ou autônomo(a) que vende por canais digitais em " + marketLabel, cnaeCode, cnaeDescription, 82,
                        "Profissional que depende de WhatsApp, Instagram, marketplace ou indicação para vender diariamente.",
                        List.of("responder muitas mensagens repetidas", "perder oportunidades por demora", "dificuldade de organizar follow-up"),
                        List.of("postar ofertas", "responder dúvidas", "separar pedidos", "confirmar pagamento e entrega"),
                        List.of("busca modelos prontos", "aceita solução de baixo atrito", "mede valor por venda recuperada")),
                persona("P3", "Responsável por operação familiar de " + marketLabel, cnaeCode, cnaeDescription, 76,
                        "Pessoa que divide atendimento, compras e tarefas administrativas com familiares ou poucos ajudantes.",
                        List.of("processos combinados verbalmente", "erros por falta de padrão", "dificuldade de delegar"),
                        List.of("distribuir tarefas", "acompanhar entregas", "conferir estoque", "resolver reclamações"),
                        List.of("prefere orientação passo a passo", "compra quando enxerga redução de confusão", "quer previsibilidade")),
                persona("P4", "Empreendedor(a) em crescimento no segmento de " + marketLabel, cnaeCode, cnaeDescription, 71,
                        "Operador que já tem demanda, mas sente gargalo em rotina, organização e atendimento.",
                        List.of("crescimento desorganizado", "dificuldade de manter padrão", "decisão baseada em memória"),
                        List.of("planejar reposição", "acompanhar vendas", "padronizar respostas", "avaliar fornecedores"),
                        List.of("valoriza método simples", "tem urgência operacional", "quer solução aplicável sem equipe técnica")));
    }

    /** Monta uma persona candidata no contrato funcional da etapa. */
    private Map<String, Object> persona(String id, String name, String cnaeCode, String cnaeDescription, int priorityScore, String description, List<String> pains, List<String> dailyTasks, List<String> buyingSignals) {
        Map<String, Object> persona = new LinkedHashMap<>();
        persona.put("id", id);
        persona.put("name", name);
        persona.put("cnaeCode", cnaeCode);
        persona.put("cnaeDescription", cnaeDescription);
        persona.put("priorityScore", priorityScore);
        persona.put("description", description);
        persona.put("operationalPains", pains);
        persona.put("dailyTasks", dailyTasks);
        persona.put("buyingSignals", buyingSignals);
        persona.put("validationNeed", "Validar em fontes reais nas próximas etapas antes de transformar em oferta.");
        return persona;
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

    /** Cria um rótulo humano curto para inserir nas personas sem linguagem de oferta. */
    private String marketLabel(String cnaeDescription, String cnaeCode) {
        String description = cnaeDescription == null ? "" : cnaeDescription.trim();
        if (description.isBlank() || description.equalsIgnoreCase("CNAE " + cnaeCode)) {
            return "atividade CNAE " + cnaeCode;
        }
        String normalized = normalize(description);
        if (normalized.contains("vestuario") || normalized.contains("roupa")) {
            return "loja de vestuário";
        }
        return description.toLowerCase(Locale.ROOT);
    }

    /** Resume as personas candidatas para consumo rápido pela tela e pelas próximas etapas. */
    private String buildPersonaSummary(List<Map<String, Object>> personas) {
        return "Personas candidatas: " + personas.stream()
                .map(persona -> Objects.toString(persona.get("name"), ""))
                .filter(name -> !name.isBlank())
                .reduce((left, right) -> left + "; " + right)
                .orElse("sem personas candidatas");
    }

    /** Resume a rotina operacional esperada antes da validação por fontes externas. */
    private String buildRoutineSummary(String marketLabel) {
        return "Rotina preliminar de " + marketLabel + ": atendimento, organização operacional, controle de pedidos/estoque, divulgação e resolução de dúvidas recorrentes.";
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

    /** Normaliza texto para comparações simples sem acento. */
    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
