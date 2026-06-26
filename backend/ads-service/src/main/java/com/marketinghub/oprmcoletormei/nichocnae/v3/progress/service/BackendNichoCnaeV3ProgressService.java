package com.marketinghub.oprmcoletormei.nichocnae.v3.progress.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecution;
import com.marketinghub.oprmcoletormei.nichocnae.v3.OprmNichoCnaeV3StageExecutionStatus;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.gateway.PersonaRoutineMaterializerNicheGateway;
import com.marketinghub.oprmcoletormei.nichocnae.v3.personaroutinematerializer.service.BackendPersonaRoutineMaterializerService;
import com.marketinghub.repository.jpa.oprm.nichocnae.v3.OprmNichoCnaeV3StageExecutionRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Consulta o progresso persistido do pipeline NichoCNAE v3 sem inferir execução no frontend. */
@Service
public class BackendNichoCnaeV3ProgressService {
    private static final String QUALITY_GATE_STAGE = "quality-gate";
    private static final String FINAL_STAGE = "persona-routine-materializer";
    private final OprmNichoCnaeV3StageExecutionRepository repository;
    private final PersonaRoutineMaterializerNicheGateway nicheGateway;
    private final BackendPersonaRoutineMaterializerService materializerService;
    private final ObjectMapper objectMapper;

    /** Inicializa o service com dependências canônicas de progresso, prévia e confirmação v3. */
    public BackendNichoCnaeV3ProgressService(
            OprmNichoCnaeV3StageExecutionRepository repository,
            PersonaRoutineMaterializerNicheGateway nicheGateway,
            BackendPersonaRoutineMaterializerService materializerService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.nicheGateway = nicheGateway;
        this.materializerService = materializerService;
        this.objectMapper = objectMapper;
    }

    /** Retorna o job mais recente do CNAE com suas etapas persistidas em ordem de criação. */
    public NichoCnaeV3JobProgressResponse latestByCnae(String cnaeCode) {
        return repository.findTop1ByCnaeCodeAndStageCodeOrderByCreatedAtDesc(cnaeCode, "cnae-intake")
                .map(latest -> {
                    List<OprmNichoCnaeV3StageExecution> executions = repository.findByJobIdOrderByCreatedAtAsc(latest.getJobId());
                    return new NichoCnaeV3JobProgressResponse(
                            latest.getJobId(),
                            latest.getCnaeCode(),
                            executions.stream().map(this::toStage).toList(),
                            buildFinalizationReview(latest.getJobId(), executions));
                })
                .orElseGet(() -> new NichoCnaeV3JobProgressResponse(null, cnaeCode, List.of(), null));
    }

    /** Confirma manualmente a materialização e libera a etapa final do job. */
    public void confirmFinalization(String cnaeCode) {
        OprmNichoCnaeV3StageExecution latest = repository.findTop1ByCnaeCodeAndStageCodeOrderByCreatedAtDesc(cnaeCode, "cnae-intake")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job v3 não encontrado para este CNAE."));
        OprmNichoCnaeV3StageExecution qualityGate = repository.findByJobIdAndStageCode(latest.getJobId(), QUALITY_GATE_STAGE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Quality gate ainda não foi concluído."));
        if (qualityGate.getStatus() != OprmNichoCnaeV3StageExecutionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quality gate ainda não foi concluído.");
        }
        if (!repository.existsByJobIdAndStageCode(latest.getJobId(), FINAL_STAGE)) {
            materializerService.create(latest.getJobId(), latest.getCnaeCode(), qualityGate.getOutputPayload(), qualityGate.getAttemptNumber(), qualityGate.getKnowledgeVersion());
        }
    }

    /** Monta a revisão final quando a etapa 9 terminou e a etapa 10 ainda aguarda confirmação. */
    private NichoCnaeV3FinalizationReviewResponse buildFinalizationReview(String jobId, List<OprmNichoCnaeV3StageExecution> executions) {
        OprmNichoCnaeV3StageExecution qualityGate = executions.stream().filter(e -> QUALITY_GATE_STAGE.equals(e.getStageCode())).findFirst().orElse(null);
        boolean hasFinalStage = executions.stream().anyMatch(e -> FINAL_STAGE.equals(e.getStageCode()));
        if (qualityGate == null || qualityGate.getStatus() != OprmNichoCnaeV3StageExecutionStatus.COMPLETED || hasFinalStage) {
            return null;
        }
        JsonNode output = parseOutput(qualityGate.getOutputPayload());
        String cnaeDescription = textOrDefault(firstText(output, "cnaeDescription", "marketDescription", "description"), "CNAE " + qualityGate.getCnaeCode());
        String targetName = "CNAE " + qualityGate.getCnaeCode() + " — " + cnaeDescription;
        Long existingId = nicheGateway.findPersonaRoutineMaterializedNiche(qualityGate.getCnaeCode(), targetName.trim().toLowerCase(Locale.ROOT))
                .map(PersonaRoutineMaterializerNicheGateway.MarketNicheSnapshot::marketNicheId).orElse(null);
        return new NichoCnaeV3FinalizationReviewResponse(
                true,
                qualityGate.getId(),
                existingId == null ? "CREATE_NEW" : "REUSE_EXISTING",
                existingId,
                targetName,
                buildNicheInformation(output, cnaeDescription),
                buildEnrichedInformation(output, qualityGate.getOutputPayload()));
    }

    /** Converte a entidade persistida em contrato de progresso para a UI. */
    private NichoCnaeV3StageProgressResponse toStage(OprmNichoCnaeV3StageExecution execution) {
        return new NichoCnaeV3StageProgressResponse(
                execution.getId(),
                execution.getStageCode(),
                execution.getStatus().name(),
                execution.getCreatedAt(),
                execution.getUpdatedAt(),
                execution.getErrorMessage(),
                execution.getInputPayload(),
                execution.getOutputPayload());
    }

    /** Converte JSON funcional ou payload livre para leitura da prévia. */
    private JsonNode parseOutput(String outputPayload) {
        if (!StringUtils.hasText(outputPayload)) return objectMapper.createObjectNode();
        try { return objectMapper.readTree(outputPayload); } catch (java.io.IOException ex) { return objectMapper.createObjectNode().put("raw", outputPayload); }
    }

    /** Retorna o primeiro campo textual disponível no payload. */
    private String firstText(JsonNode root, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = root.path(fieldName);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.isTextual() ? value.asText() : value.toString();
                if (StringUtils.hasText(text)) return text.trim();
            }
        }
        return null;
    }

    /** Retorna texto padrão quando o campo não está disponível. */
    private String textOrDefault(String value, String defaultValue) { return StringUtils.hasText(value) ? value.trim() : defaultValue; }

    /** Monta resumo do nicho base encontrado antes da materialização. */
    private String buildNicheInformation(JsonNode output, String cnaeDescription) {
        return "Descrição CNAE: " + cnaeDescription + "\n\nRotina observada: " + textOrDefault(firstText(output, "routineSummary", "routine", "rotina"), "Não informada.") + "\n\nTarefas diárias: " + textOrDefault(firstText(output, "personaDailyTasks", "dailyTasks", "tarefasDiarias", "tasks"), "Não informadas.");
    }

    /** Monta resumo do perfil enriquecido encontrado antes da materialização. */
    private String buildEnrichedInformation(JsonNode output, String fallback) {
        return "Persona: " + textOrDefault(firstText(output, "personaSummary", "persona"), "Não informada.") + "\n\nEvidências: " + textOrDefault(firstText(output, "evidenceSummary", "evidences"), fallback) + "\n\nGatilhos comerciais: " + textOrDefault(firstText(output, "commercialTriggers", "triggers"), "Não informados.") + "\n\nObjeções: " + textOrDefault(firstText(output, "objections"), "Não informadas.");
    }
}
