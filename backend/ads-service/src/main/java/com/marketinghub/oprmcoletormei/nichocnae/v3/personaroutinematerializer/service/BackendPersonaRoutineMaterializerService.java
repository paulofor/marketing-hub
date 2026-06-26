package com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.gateway.PersonaRoutineMaterializerNicheGateway;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.service.createStageExecution.PersonaRoutineMaterializerCreateResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.service.pending.PersonaRoutineMaterializerPendingResponse;
import com.marketinghub.oprmcoletormei.nichocnae.v3.shared.OprmNichoCnaeV3StageServiceSupport;
import com.marketinghub.repository.jpa.oprm.market.OprmCnpjCnaeDimRepository;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Service canônico da etapa persona-routine-materializer do pipeline NichoCNAE v3 no backend. */
@Service
public class BackendPersonaRoutineMaterializerService extends OprmNichoCnaeV3StageServiceSupport {
    private static final String STAGE_CODE = "persona-routine-materializer";
    private static final String NEXT_STAGE = "";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";
    private static final String CREATED_BY = "OPRM_NICHO_CNAE_V3";
    private static final String DEFAULT_QUALITY_STATUS = "V3_PERSONA_ROUTINE_MATERIALIZED";
    private static final BigDecimal DEFAULT_SOURCE_SCORE = BigDecimal.ZERO;
    private final PersonaRoutineMaterializerNicheGateway nicheGateway;
    private final ObjectMapper objectMapper;

    /** Inicializa o service com repository canônico de execuções v3. */
    public BackendPersonaRoutineMaterializerService(
            OprmNichoCnaeV3StageExecutionRepository repository,
            OprmCnpjCnaeDimRepository cnaeRepository,
            PersonaRoutineMaterializerNicheGateway nicheGateway,
            ObjectMapper objectMapper) {
        super(repository, cnaeRepository, STAGE_CODE);
        this.nicheGateway = nicheGateway;
        this.objectMapper = objectMapper;
    }

    /** Cria pendência inicial ou encadeada para a etapa persona-routine-materializer. */
    public PersonaRoutineMaterializerCreateResponse create(String jobId, String cnaeCode, String inputPayload, Integer attemptNumber, Integer knowledgeVersion) {
        return toCreateResponse(doCreate(jobId, cnaeCode, inputPayload, attemptNumber, knowledgeVersion));
    }

    /** Inicia pendência da etapa para o CNAE informado pela tela administrativa. */
    public PersonaRoutineMaterializerCreateResponse start(String cnaeCode) {
        markCnaePipelineStarted(cnaeCode, STATUS_STARTED);
        return create(null, cnaeCode, "{\"cnaeCode\":\"" + cnaeCode + "\"}", 1, 1);
    }

    /** Lista pendências da etapa persona-routine-materializer para o executor OPRM. */
    public List<PersonaRoutineMaterializerPendingResponse> pending() {
        return pendingExecutions().stream().map(this::toPendingResponse).toList();
    }

    /** Registra conclusão da etapa persona-routine-materializer. */
    @Transactional
    public PersonaRoutineMaterializerCreateResponse complete(Long stageExecutionId, String outputPayload, String nextStageCode) {
        OprmNichoCnaeV3StageExecution execution = doComplete(stageExecutionId, outputPayload, nextStageCode);
        materializeNicheData(execution);
        return toCreateResponse(execution);
    }

    /** Registra falha da etapa persona-routine-materializer. */
    public PersonaRoutineMaterializerCreateResponse fail(Long stageExecutionId, String errorMessage) {
        return toCreateResponse(doFail(stageExecutionId, errorMessage));
    }

    /** Converte entidade persistida em resposta de criação/conclusão/falha. */
    private PersonaRoutineMaterializerCreateResponse toCreateResponse(OprmNichoCnaeV3StageExecution execution) {
        return new PersonaRoutineMaterializerCreateResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getStageCode(), execution.getStatus().name());
    }

    /** Converte entidade persistida em item pendente para executor externo. */
    private PersonaRoutineMaterializerPendingResponse toPendingResponse(OprmNichoCnaeV3StageExecution execution) {
        return new PersonaRoutineMaterializerPendingResponse(execution.getId(), execution.getJobId(), execution.getCnaeCode(), execution.getInputPayload(), execution.getAttemptNumber(), execution.getKnowledgeVersion());
    }

    /** Materializa o resultado final v3 nas estruturas reutilizáveis do nicho. */
    private void materializeNicheData(OprmNichoCnaeV3StageExecution execution) {
        JsonNode output = parseOutput(execution.getOutputPayload());
        String cnaeDescription = firstText(output, "cnaeDescription", "marketDescription", "description");
        if (!StringUtils.hasText(cnaeDescription)) {
            cnaeDescription = "CNAE " + execution.getCnaeCode();
        }
        String neutralNicheName = buildCnaeNicheName(execution.getCnaeCode(), cnaeDescription);
        String routineSummary = textOrDefault(
                firstText(output, "routineSummary", "routine", "rotina"),
                "Rotina operacional materializada pelo NichoCNAE v3.");
        String dailyTasks = textOrDefault(
                firstText(output, "personaDailyTasks", "dailyTasks", "tarefasDiarias", "tasks"),
                "Tarefas diárias materializadas pelo NichoCNAE v3.");
        Instant now = Instant.now();
        Long existingMarketNicheId = nicheGateway
                .findPersonaRoutineMaterializedNiche(
                        execution.getCnaeCode(), neutralNicheName.trim().toLowerCase(Locale.ROOT))
                .map(PersonaRoutineMaterializerNicheGateway.MarketNicheSnapshot::marketNicheId)
                .orElse(null);
        nicheGateway.materialize(
                new PersonaRoutineMaterializerNicheGateway.MarketNicheDraft(
                        existingMarketNicheId,
                        neutralNicheName,
                        buildNicheDescription(cnaeDescription, routineSummary, dailyTasks),
                        execution.getCnaeCode(),
                        cnaeDescription,
                        null,
                        routineSummary,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        dailyTasks,
                        null),
                new PersonaRoutineMaterializerNicheGateway.EnrichedNicheProfileDraft(
                        null,
                        execution.getId(),
                        execution.getId(),
                        execution.getCnaeCode(),
                        cnaeDescription,
                        DEFAULT_SOURCE_SCORE,
                        DEFAULT_QUALITY_STATUS,
                        integerOrDefault(output, "specificityScore", 0),
                        integerOrDefault(output, "confidenceScore", 0),
                        integerOrDefault(output, "duplicationScore", 0),
                        integerOrDefault(output, "routineEvidenceScore", 0),
                        integerOrDefault(output, "difficultyEvidenceScore", 0),
                        integerOrDefault(output, "sourceDiversityScore", 0),
                        integerOrDefault(output, "solutionLanguageRiskScore", 0),
                        neutralNicheName,
                        neutralNicheName,
                        "V3_PERSONA_ROUTINE",
                        routineSummary,
                        dailyTasks,
                        textOrDefault(firstText(output, "evidenceSummary", "evidences"), execution.getOutputPayload()),
                        firstText(output, "sourceDomains", "sources"),
                        firstText(output, "personaSummary", "persona"),
                        firstText(output, "languagePatterns", "language"),
                        firstText(output, "commercialTriggers", "triggers"),
                        firstText(output, "objections"),
                        execution.getOutputPayload(),
                        CREATED_BY,
                        now,
                        now));
    }

    /** Converte o JSON de saída em árvore tolerante a payloads técnicos ou funcionais. */
    private JsonNode parseOutput(String outputPayload) {
        if (!StringUtils.hasText(outputPayload)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(outputPayload);
        } catch (java.io.IOException ex) {
            return objectMapper.createObjectNode().put("raw", outputPayload);
        }
    }

    /** Retorna o primeiro campo textual disponível no payload. */
    private String firstText(JsonNode root, String... fieldNames) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = root.path(fieldName);
            if (value.isMissingNode() || value.isNull()) {
                continue;
            }
            String text = value.isTextual() ? value.asText() : value.toString();
            if (StringUtils.hasText(text)) {
                return text.trim();
            }
        }
        return null;
    }

    /** Retorna texto padrão quando o campo final ainda não veio no payload do executor. */
    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    /** Lê pontuação numérica opcional do payload da etapa final. */
    private Integer integerOrDefault(JsonNode root, String fieldName, int defaultValue) {
        JsonNode value = root == null ? null : root.path(fieldName);
        return value != null && value.isNumber() ? value.asInt() : defaultValue;
    }


    /** Monta o nome canônico do nicho usando código e descrição do CNAE. */
    private String buildCnaeNicheName(String cnaeCode, String cnaeDescription) {
        return "CNAE " + cnaeCode + " — " + cnaeDescription;
    }

    /** Monta uma descrição compacta do nicho base para outros pipelines consumirem. */
    private String buildNicheDescription(String cnaeDescription, String routineSummary, String dailyTasks) {
        return "Descrição CNAE: " + cnaeDescription
                + "\n\nRotina observada: " + routineSummary
                + "\n\nTarefas diárias: " + dailyTasks;
    }
}
