package com.marketinghub.pipeline.service;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.pipeline.Pipeline;
import com.marketinghub.pipeline.PipelineStage;
import com.marketinghub.pipeline.definition.PipelineDefinitionRegistry;
import com.marketinghub.pipeline.definition.PipelineDefinitionRegistry.PipelineDefinition;
import com.marketinghub.pipeline.definition.PipelineDefinitionRegistry.PipelineStageDefinition;
import com.marketinghub.pipeline.dto.OfficialPipelineDto;
import com.marketinghub.pipeline.dto.GeraLandingStageModelDto;
import com.marketinghub.pipeline.dto.OfficialPipelineStageDto;
import com.marketinghub.pipeline.dto.PipelineDiagnosticsDto;
import com.marketinghub.pipeline.dto.PipelineDiagnosticsIssueDto;
import com.marketinghub.pipeline.dto.PipelineFieldPolicyDto;
import com.marketinghub.pipeline.dto.PipelineMetadataDto;
import com.marketinghub.pipeline.dto.PipelineRequest;
import com.marketinghub.pipeline.dto.PipelineStageRequest;
import com.marketinghub.pipeline.dto.StageFieldPolicyDto;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineRepository;
import com.marketinghub.repository.jpa.pipeline.PipelineStageRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serviço responsável por manter pipelines e etapas usados pela operação do Marketing Hub.
 */
@Service
public class PipelineService {
    private static final Map<String, List<String>> GERA_LANDING_STAGE_ALIASES = buildGeraLandingStageAliases();
    private static final String TEXT_DEFAULT_MODEL_CODE = "gpt-5.2";
    private static final String TEXT_DEFAULT_MODEL_NAME = "GPT-5.2";
    private static final String IMAGE_DEFAULT_MODEL_CODE = "gpt-image-2";
    private static final String IMAGE_DEFAULT_MODEL_NAME = "GPT Image 2";
    private static final String PRICING_MODE_FLEX = "flex";

    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository stageRepository;
    private final OpenAiModelRepository openAiModelRepository;
    private final PipelineDefinitionRegistry definitionRegistry;

    /**
     * Inicializa o serviço com os repositórios centralizados de pipelines, etapas e modelos OpenAI.
     */
    public PipelineService(
            PipelineRepository pipelineRepository,
            PipelineStageRepository stageRepository,
            OpenAiModelRepository openAiModelRepository,
            PipelineDefinitionRegistry definitionRegistry) {
        this.pipelineRepository = pipelineRepository;
        this.stageRepository = stageRepository;
        this.openAiModelRepository = openAiModelRepository;
        this.definitionRegistry = definitionRegistry;
    }

    /**
     * Lista todos os pipelines cadastrados com etapas ordenadas para administração.
     */
    public List<Pipeline> list() {
        return pipelineRepository.findAll().stream()
                .peek(this::sortStages)
                .sorted(Comparator.comparing(Pipeline::getModule).thenComparing(Pipeline::getName))
                .toList();
    }

    /**
     * Busca um pipeline pelo identificador, garantindo erro claro quando não existe.
     */
    public Pipeline get(Long id) {
        Pipeline pipeline = findPipeline(id);
        sortStages(pipeline);
        return pipeline;
    }

    /**
     * Lista os modelos OpenAI configurados no banco para as etapas exibidas na aba Gera Landing.
     */
    @Transactional(readOnly = true)
    public List<GeraLandingStageModelDto> listGeraLandingStageModels() {
        List<PipelineStage> activeStages = pipelineRepository.findAll().stream()
                .filter(Pipeline::isActive)
                .peek(this::sortStages)
                .flatMap(pipeline -> pipeline.getStages().stream())
                .filter(PipelineStage::isActive)
                .toList();

        return GERA_LANDING_STAGE_ALIASES.keySet().stream()
                .map(stageCode -> toGeraLandingStageModelDto(stageCode, activeStages))
                .toList();
    }

    /**
     * Cria um novo pipeline operacional validando módulo e contrato conhecido.
     */
    @Transactional
    public Pipeline create(PipelineRequest request) {
        validatePipelineModule(request.getModule());
        validateOfficialPipelineCreate(request);
        Pipeline pipeline = new Pipeline();
        applyPipelineRequest(pipeline, request);
        return pipelineRepository.save(pipeline);
    }

    /**
     * Atualiza os dados básicos de um pipeline existente sem permitir mudanças estruturais oficiais.
     */
    @Transactional
    public Pipeline update(Long id, PipelineRequest request) {
        Pipeline pipeline = findPipeline(id);
        validatePipelineModule(request.getModule());
        validateOfficialPipelineUpdate(pipeline, request);
        applyPipelineRequest(pipeline, request);
        return pipelineRepository.save(pipeline);
    }

    /**
     * Remove um pipeline configurável, bloqueando exclusão de contratos oficiais.
     */
    @Transactional
    public void delete(Long id) {
        Pipeline pipeline = findPipeline(id);
        if (definitionRegistry.isOfficialPipelineCode(pipeline.getCode())) {
            throw badRequest("Pipeline oficial não pode ser excluído: " + pipeline.getCode());
        }
        pipelineRepository.delete(pipeline);
    }

    /**
     * Cria uma etapa dentro de um pipeline existente validando contrato oficial quando aplicável.
     */
    @Transactional
    public PipelineStage createStage(Long pipelineId, PipelineStageRequest request) {
        Pipeline pipeline = findPipeline(pipelineId);
        validateStageRequest(pipeline, null, request);
        PipelineStage stage = new PipelineStage();
        stage.setPipeline(pipeline);
        applyStageRequest(stage, request);
        return stageRepository.save(stage);
    }

    /**
     * Atualiza uma etapa, preservando o vínculo e bloqueando alterações estruturais oficiais.
     */
    @Transactional
    public PipelineStage updateStage(Long pipelineId, Long stageId, PipelineStageRequest request) {
        PipelineStage stage = findStageInPipeline(pipelineId, stageId);
        validateStageRequest(stage.getPipeline(), stage, request);
        applyStageRequest(stage, request);
        return stageRepository.save(stage);
    }

    /**
     * Remove uma etapa específica, bloqueando remoção de etapa obrigatória oficial.
     */
    @Transactional
    public void deleteStage(Long pipelineId, Long stageId) {
        PipelineStage stage = findStageInPipeline(pipelineId, stageId);
        PipelineDefinition definition = definitionRegistry.findByPipelineCode(stage.getPipeline().getCode()).orElse(null);
        if (definition != null && definitionRegistry.findStage(definition, stage.getCode()).filter(PipelineStageDefinition::required).isPresent()) {
            throw badRequest("Etapa obrigatória oficial não pode ser excluída: " + stage.getCode());
        }
        stageRepository.delete(stage);
    }


    /**
     * Retorna os metadados oficiais que orientam os campos editáveis e protegidos na tela.
     */
    public PipelineMetadataDto metadata() {
        return PipelineMetadataDto.builder()
                .validModules(definitionRegistry.validModules().stream().sorted().toList())
                .officialPipelines(definitionRegistry.officialPipelines().stream().map(this::toOfficialDto).toList())
                .build();
    }

    /**
     * Compara as etapas salvas no banco com a definição oficial conhecida pelo backend.
     */
    public PipelineDiagnosticsDto diagnostics(Long id) {
        Pipeline pipeline = get(id);
        PipelineDefinition definition = definitionRegistry.findByPipelineCode(pipeline.getCode()).orElse(null);
        if (definition == null) {
            return PipelineDiagnosticsDto.builder()
                    .pipelineId(pipeline.getId())
                    .pipelineCode(pipeline.getCode())
                    .canonicalPipelineCode(null)
                    .status("ATENÇÃO")
                    .expectedStages(0)
                    .configuredStages(pipeline.getStages().size())
                    .issues(List.of(issue("WARN", null, null, "Pipeline não possui definição oficial no backend.",
                            "Pipeline foi criado como configuração livre ou ainda não foi canonizado.",
                            "Criar definição oficial no backend antes de usar como pipeline estrutural.")))
                    .build();
        }

        List<PipelineDiagnosticsIssueDto> issues = new ArrayList<>();
        Set<String> configuredCodes = new HashSet<>();
        Set<Integer> configuredPositions = new HashSet<>();
        for (PipelineStage stage : pipeline.getStages()) {
            validateStageAgainstDefinition(definition, stage, configuredCodes, configuredPositions, issues);
        }
        for (PipelineStageDefinition expected : definition.stages()) {
            boolean present = pipeline.getStages().stream()
                    .anyMatch(stage -> definitionRegistry.findStage(definition, stage.getCode())
                            .map(expected::equals)
                            .orElse(false));
            if (!present) {
                issues.add(issue("ERROR", expected.operationalCode(), expected.canonicalCode(),
                        "Etapa obrigatória está ausente no banco.",
                        "Banco foi alterado manualmente ou o pipeline foi criado sem sincronização oficial.",
                        "Cadastrar a etapa conforme definição oficial antes de executar o pipeline."));
            }
        }
        String status = resolveDiagnosticsStatus(issues);
        return PipelineDiagnosticsDto.builder()
                .pipelineId(pipeline.getId())
                .pipelineCode(pipeline.getCode())
                .canonicalPipelineCode(definition.code())
                .status(status)
                .expectedStages(definition.stages().size())
                .configuredStages(pipeline.getStages().size())
                .issues(issues)
                .build();
    }

    /**
     * Localiza um pipeline pelo identificador interno.
     */
    private Pipeline findPipeline(Long id) {
        return pipelineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pipeline não encontrado: " + id));
    }

    /**
     * Localiza uma etapa validando que ela pertence ao pipeline da rota.
     */
    private PipelineStage findStageInPipeline(Long pipelineId, Long stageId) {
        PipelineStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new EntityNotFoundException("Etapa de pipeline não encontrada: " + stageId));
        if (!stage.getPipeline().getId().equals(pipelineId)) {
            throw new EntityNotFoundException("Etapa não pertence ao pipeline informado: " + pipelineId);
        }
        return stage;
    }


    /**
     * Garante que o módulo informado pertence aos módulos oficiais conhecidos.
     */
    private void validatePipelineModule(String module) {
        if (!definitionRegistry.validModules().contains(module)) {
            throw badRequest("Módulo de pipeline desconhecido: " + module);
        }
    }

    /**
     * Bloqueia criação de pipeline oficial com módulo divergente do contrato canônico.
     */
    private void validateOfficialPipelineCreate(PipelineRequest request) {
        PipelineDefinition definition = definitionRegistry.findByPipelineCode(request.getCode()).orElse(null);
        if (definition != null && !definition.module().equals(request.getModule())) {
            throw badRequest("Pipeline oficial deve usar o módulo canônico "
                    + definition.module()
                    + ": "
                    + request.getCode());
        }
    }

    /**
     * Bloqueia alteração de código ou módulo estrutural em pipeline oficial já existente.
     */
    private void validateOfficialPipelineUpdate(Pipeline pipeline, PipelineRequest request) {
        if (!definitionRegistry.isOfficialPipelineCode(pipeline.getCode())) {
            return;
        }
        PipelineDefinition definition = definitionRegistry.findByPipelineCode(pipeline.getCode()).orElseThrow();
        if (!definitionRegistry.normalize(pipeline.getCode()).equals(definitionRegistry.normalize(request.getCode()))) {
            throw badRequest("Código de pipeline oficial não pode ser alterado: " + pipeline.getCode());
        }
        if (!definition.module().equals(request.getModule())) {
            throw badRequest("Módulo de pipeline oficial não pode ser alterado: " + pipeline.getCode());
        }
    }

    /**
     * Valida duplicidade operacional e aderência da etapa ao contrato oficial quando existir.
     */
    private void validateStageRequest(Pipeline pipeline, PipelineStage currentStage, PipelineStageRequest request) {
        ensureUniqueStagePositionAndCode(pipeline, currentStage, request);
        PipelineDefinition definition = definitionRegistry.findByPipelineCode(pipeline.getCode()).orElse(null);
        if (definition == null) {
            return;
        }
        PipelineStageDefinition requestedDefinition = definitionRegistry.findStage(definition, request.getCode())
                .orElseThrow(() -> badRequest("Etapa não mapeia para definição canônica oficial: " + request.getCode()));
        if (currentStage != null) {
            PipelineStageDefinition currentDefinition = definitionRegistry.findStage(definition, currentStage.getCode())
                    .orElseThrow(() -> badRequest("Etapa atual não mapeia para definição canônica oficial: " + currentStage.getCode()));
            if (!currentDefinition.equals(requestedDefinition)) {
                throw badRequest("Código estrutural de etapa oficial não pode ser alterado: " + currentStage.getCode());
            }
        }
        if (requestedDefinition.required() && !request.isRequired()) {
            throw badRequest("Etapa obrigatória oficial não pode deixar de ser obrigatória: " + request.getCode());
        }
        if (requestedDefinition.required() && !request.isActive()) {
            throw badRequest("Etapa obrigatória oficial não pode ser desativada sem regra explícita: " + request.getCode());
        }
        if (!java.util.Objects.equals(requestedDefinition.executionModule(), normalizeOptionalText(request.getExecutionModule()))) {
            throw badRequest("Módulo executor de etapa oficial não pode divergir do contrato: " + request.getCode());
        }
        if (!java.util.Objects.equals(requestedDefinition.rootPackage(), normalizeOptionalText(request.getRootPackage()))) {
            throw badRequest("Pacote raiz de etapa oficial não pode divergir do contrato: " + request.getCode());
        }
    }

    /**
     * Impede duplicidade de posição ou código operacional antes da constraint do banco.
     */
    private void ensureUniqueStagePositionAndCode(Pipeline pipeline, PipelineStage currentStage, PipelineStageRequest request) {
        for (PipelineStage existing : pipeline.getStages()) {
            if (currentStage != null && existing.getId().equals(currentStage.getId())) {
                continue;
            }
            if (existing.getPosition().equals(request.getPosition())) {
                throw badRequest("Já existe etapa com a posição operacional " + request.getPosition());
            }
            if (definitionRegistry.normalize(existing.getCode()).equals(definitionRegistry.normalize(request.getCode()))) {
                throw badRequest("Já existe etapa com o código operacional " + request.getCode());
            }
        }
    }

    /**
     * Adiciona divergências específicas de uma etapa comparando banco e definição oficial.
     */
    private void validateStageAgainstDefinition(
            PipelineDefinition definition,
            PipelineStage stage,
            Set<String> configuredCodes,
            Set<Integer> configuredPositions,
            List<PipelineDiagnosticsIssueDto> issues) {
        PipelineStageDefinition stageDefinition = definitionRegistry.findStage(definition, stage.getCode()).orElse(null);
        String normalizedCode = definitionRegistry.normalize(stage.getCode());
        if (!configuredCodes.add(normalizedCode)) {
            issues.add(issue("ERROR", stage.getCode(), stageDefinition == null ? null : stageDefinition.canonicalCode(),
                    "Código operacional duplicado no banco.",
                    "Banco recebeu alteração duplicada antes da validação estrutural.",
                    "Manter apenas uma etapa por código operacional."));
        }
        if (!configuredPositions.add(stage.getPosition())) {
            issues.add(issue("ERROR", stage.getCode(), stageDefinition == null ? null : stageDefinition.canonicalCode(),
                    "Posição operacional duplicada no banco.",
                    "Banco recebeu alteração duplicada antes da validação estrutural.",
                    "Manter apenas uma etapa por posição operacional."));
        }
        if (stageDefinition == null) {
            issues.add(issue("ERROR", stage.getCode(), null,
                    "Etapa extra não possui mapeamento canônico conhecido.",
                    "Banco foi alterado manualmente ou a tela salvou código fora do contrato oficial.",
                    "Remover a etapa extra ou criar alias oficial no backend."));
            return;
        }
        if (!stage.getPosition().equals(stageDefinition.position())) {
            issues.add(issue("ERROR", stage.getCode(), stageDefinition.canonicalCode(),
                    "Etapa obrigatória está fora da posição canônica.",
                    "Banco foi alterado manualmente ou por tela sem validação estrutural.",
                    "Corrigir posição conforme definição oficial."));
        }
        if (stageDefinition.required() && !stage.isRequired()) {
            issues.add(issue("ERROR", stage.getCode(), stageDefinition.canonicalCode(),
                    "Etapa obrigatória está marcada como opcional no banco.",
                    "Configuração operacional removeu obrigação estrutural do pipeline oficial.",
                    "Marcar a etapa como obrigatória."));
        }
        if (stageDefinition.required() && !stage.isActive()) {
            issues.add(issue("ERROR", stage.getCode(), stageDefinition.canonicalCode(),
                    "Etapa obrigatória está inativa no banco.",
                    "Configuração operacional desativou etapa necessária para execução do contrato.",
                    "Reativar a etapa obrigatória ou formalizar regra explícita."));
        }
    }

    /**
     * Resolve o status agregado do diagnóstico a partir da maior severidade encontrada.
     */
    private String resolveDiagnosticsStatus(List<PipelineDiagnosticsIssueDto> issues) {
        if (issues.stream().anyMatch(issue -> "ERROR".equals(issue.severity()))) {
            return "BLOQUEADO";
        }
        if (issues.stream().anyMatch(issue -> "WARN".equals(issue.severity()))) {
            return "ATENÇÃO";
        }
        return "OK";
    }

    /**
     * Cria uma divergência padronizada com causa-raiz e ação recomendada para a tela.
     */
    private PipelineDiagnosticsIssueDto issue(
            String severity,
            String stageCode,
            String canonicalCode,
            String message,
            String rootCause,
            String recommendedAction) {
        return PipelineDiagnosticsIssueDto.builder()
                .severity(severity)
                .stageCode(stageCode)
                .canonicalCode(canonicalCode)
                .message(message)
                .rootCause(rootCause)
                .recommendedAction(recommendedAction)
                .build();
    }

    /**
     * Converte definição oficial interna para DTO de metadados consumido pela tela.
     */
    private OfficialPipelineDto toOfficialDto(PipelineDefinition definition) {
        return OfficialPipelineDto.builder()
                .module(definition.module())
                .code(definition.code())
                .name(definition.name())
                .canonicalVersion(definition.canonicalVersion())
                .official(definition.official())
                .aliases(definition.aliases().stream().sorted().toList())
                .implementationModules(implementationModules(definition))
                .backendPackages(backendPackages(definition))
                .modulePackages(modulePackages(definition))
                .fieldPolicy(toPipelineFieldPolicyDto(definition))
                .stages(definition.stages().stream().map(stage -> toOfficialStageDto(definition, stage)).toList())
                .build();
    }

    /**
     * Converte definição oficial de etapa para DTO de metadados consumido pela tela.
     */
    private OfficialPipelineStageDto toOfficialStageDto(
            PipelineDefinition pipelineDefinition, PipelineStageDefinition definition) {
        return OfficialPipelineStageDto.builder()
                .canonicalCode(definition.canonicalCode())
                .operationalCode(definition.operationalCode())
                .name(definition.name())
                .position(definition.position())
                .required(definition.required())
                .requiresOpenAiModel(definition.requiresOpenAiModel())
                .configurable(definition.configurable())
                .executionModule(definition.executionModule())
                .rootPackage(definition.rootPackage())
                .modulePackage(definition.modulePackage())
                .fieldPolicy(toStageFieldPolicyDto(pipelineDefinition))
                .aliases(definition.aliases().stream().sorted().toList())
                .build();
    }

    /**
     * Lista os módulos executores declarados pelas etapas oficiais do pipeline.
     */
    private List<String> implementationModules(PipelineDefinition definition) {
        List<String> modules = definition.stages().stream()
                .map(PipelineStageDefinition::executionModule)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
        if (!modules.isEmpty()) {
            return modules;
        }
        return modulePackages(definition).isEmpty() ? List.of() : List.of("ai-worker");
    }

    /**
     * Lista os pacotes do backend declarados pelas etapas oficiais do pipeline.
     */
    private List<String> backendPackages(PipelineDefinition definition) {
        return definition.stages().stream()
                .map(PipelineStageDefinition::rootPackage)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Lista os pacotes do módulo executor declarados pelas etapas oficiais do pipeline.
     */
    private List<String> modulePackages(PipelineDefinition definition) {
        return definition.stages().stream()
                .map(PipelineStageDefinition::modulePackage)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Converte política oficial de pipeline para DTO de metadados consumido pela tela.
     */
    private PipelineFieldPolicyDto toPipelineFieldPolicyDto(PipelineDefinition definition) {
        return PipelineFieldPolicyDto.builder()
                .codeStructural(definition.pipelineFieldPolicy().codeStructural())
                .moduleStructural(definition.pipelineFieldPolicy().moduleStructural())
                .nameStructural(definition.pipelineFieldPolicy().nameStructural())
                .descriptionOperational(definition.pipelineFieldPolicy().descriptionOperational())
                .activeOperational(definition.pipelineFieldPolicy().activeOperational())
                .build();
    }

    /**
     * Converte política oficial de etapa para DTO de metadados consumido pela tela.
     */
    private StageFieldPolicyDto toStageFieldPolicyDto(PipelineDefinition definition) {
        return StageFieldPolicyDto.builder()
                .codeStructural(definition.stageFieldPolicy().codeStructural())
                .positionStructural(definition.stageFieldPolicy().positionStructural())
                .nameStructural(definition.stageFieldPolicy().nameStructural())
                .requiredStructural(definition.stageFieldPolicy().requiredStructural())
                .descriptionOperational(definition.stageFieldPolicy().descriptionOperational())
                .activeOperational(definition.stageFieldPolicy().activeOperational())
                .openAiModelOperational(definition.stageFieldPolicy().openAiModelOperational())
                .build();
    }

    /**
     * Converte a etapa configurada no banco para DTO específico da tela Gera Landing.
     */
    private GeraLandingStageModelDto toGeraLandingStageModelDto(String stageCode, List<PipelineStage> activeStages) {
        return activeStages.stream()
                .filter(stage -> matchesGeraLandingStage(stage, stageCode))
                .filter(stage -> stage.getOpenAiModel() != null)
                .findFirst()
                .map(stage -> buildGeraLandingStageModelDto(stageCode, stage, stage.getOpenAiModel(), false))
                .orElseGet(() -> buildDefaultGeraLandingStageModelDto(stageCode));
    }

    /** Monta o DTO de modelo da etapa preenchendo os custos do modo flex e o tipo de artefato gerado. */
    private GeraLandingStageModelDto buildGeraLandingStageModelDto(
            String stageCode, PipelineStage stage, OpenAiModel openAiModel, boolean defaultModelApplied) {
        GeraLandingStageModelDto.GeraLandingStageModelDtoBuilder builder = GeraLandingStageModelDto.builder()
                .stageCode(stageCode)
                .generatedAssetType(resolveGeneratedAssetType(stageCode))
                .pricingMode(PRICING_MODE_FLEX)
                .defaultModelApplied(defaultModelApplied)
                .openAiModelId(openAiModel.getId())
                .openAiModelName(openAiModel.getName())
                .openAiModelCode(openAiModel.getCode())
                .priceInputFlex(openAiModel.getPriceInputBatch())
                .priceInputCachedFlex(openAiModel.getPriceInputCachedBatch())
                .priceOutputFlex(openAiModel.getPriceOutputBatch());

        if (stage != null) {
            builder.pipelineId(stage.getPipeline().getId())
                    .pipelineCode(stage.getPipeline().getCode())
                    .pipelineStageId(stage.getId())
                    .pipelineStageCode(stage.getCode());
        }

        return builder.build();
    }

    /** Monta o DTO usando o modelo default quando a etapa não possui modelo associado no pipeline. */
    private GeraLandingStageModelDto buildDefaultGeraLandingStageModelDto(String stageCode) {
        String defaultCode = resolveDefaultModelCode(stageCode);
        OpenAiModel fallbackModel = openAiModelRepository.findByCode(defaultCode)
                .orElseGet(() -> OpenAiModel.builder()
                        .name(resolveDefaultModelName(stageCode))
                        .code(defaultCode)
                        .priceInputBatch(BigDecimal.ZERO)
                        .priceInputCachedBatch(BigDecimal.ZERO)
                        .priceOutputBatch(BigDecimal.ZERO)
                        .build());
        return buildGeraLandingStageModelDto(stageCode, null, fallbackModel, true);
    }

    /** Resolve o modelo default conforme o tipo de geração da etapa. */
    private String resolveDefaultModelCode(String stageCode) {
        return "landing-page-image-generation".equals(stageCode) ? IMAGE_DEFAULT_MODEL_CODE : TEXT_DEFAULT_MODEL_CODE;
    }

    /** Resolve o nome legível do modelo default quando o catálogo ainda não possui o registro. */
    private String resolveDefaultModelName(String stageCode) {
        return "landing-page-image-generation".equals(stageCode) ? IMAGE_DEFAULT_MODEL_NAME : TEXT_DEFAULT_MODEL_NAME;
    }

    /** Classifica o tipo de artefato gerado por cada etapa com uso de IA. */
    private String resolveGeneratedAssetType(String stageCode) {
        return "landing-page-image-generation".equals(stageCode) ? "imagem" : "texto";
    }

    /**
     * Verifica se a etapa persistida corresponde ao código canônico usado pela tela Gera Landing.
     */
    private boolean matchesGeraLandingStage(PipelineStage stage, String stageCode) {
        String normalizedStageCode = normalizePipelineCode(stage.getCode());
        return GERA_LANDING_STAGE_ALIASES.getOrDefault(stageCode, List.of(stageCode)).stream()
                .map(this::normalizePipelineCode)
                .anyMatch(normalizedStageCode::equals);
    }

    /**
     * Normaliza códigos operacionais de pipeline para comparar aliases históricos e canônicos.
     */
    private String normalizePipelineCode(String code) {
        return StringUtils.hasText(code) ? code.trim().toLowerCase(Locale.ROOT).replace('_', '-') : "";
    }

    /**
     * Monta os aliases de etapas do Gera Landing preservando a ordem visual da aba do experimento.
     */
    private static Map<String, List<String>> buildGeraLandingStageAliases() {
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        aliases.put("landing-page-wireframe", List.of(
                "landing-page-wireframe",
                "LANDING_PAGE_WIREFRAME",
                "landing-wireframe"));
        aliases.put("landing-page-copy", List.of(
                "landing-page-copy",
                "LANDING_PAGE_COPY",
                "landing-copy"));
        aliases.put("landing-page-image-planning", List.of(
                "landing-page-image-planning",
                "LANDING_PAGE_IMAGE_PLANNING",
                "image-planning",
                "landing-image-planning"));
        aliases.put("landing-page-image-generation", List.of(
                "landing-page-image-generation",
                "LANDING_PAGE_IMAGE_GENERATION",
                "image-generation",
                "landing-image-generation",
                "framework-image-generation"));
        aliases.put("landing-page-design-preset", List.of(
                "landing-page-design-preset",
                "LANDING_PAGE_DESIGN_PRESET",
                "preset-design",
                "design-preset"));
        aliases.put("landing-page-quality-review", List.of(
                "landing-page-quality-review",
                "LANDING_PAGE_QUALITY_REVIEW",
                "quality-review",
                "landing-quality-review"));
        aliases.put("landing-page-deliverables", List.of(
                "landing-page-deliverables",
                "LANDING_PAGE_DELIVERABLES",
                "deliverables",
                "landing-deliverables",
                "LANDING_PAGE_HTML",
                "landing-page-html",
                "landing-html",
                "geralanding-html"));
        return aliases;
    }

    /**
     * Cria erro HTTP 400 para violações do contrato operacional editável.
     */
    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Aplica o payload validado nos campos básicos de pipeline.
     */
    private void applyPipelineRequest(Pipeline pipeline, PipelineRequest request) {
        pipeline.setName(request.getName());
        pipeline.setCode(request.getCode());
        pipeline.setModule(request.getModule());
        pipeline.setDescription(request.getDescription());
        pipeline.setActive(request.isActive());
    }

    /**
     * Aplica o payload validado nos campos de configuração da etapa.
     */
    private void applyStageRequest(PipelineStage stage, PipelineStageRequest request) {
        stage.setPosition(request.getPosition());
        stage.setName(request.getName());
        stage.setCode(request.getCode());
        stage.setDescription(request.getDescription());
        stage.setExecutionModule(normalizeOptionalText(request.getExecutionModule()));
        stage.setRootPackage(normalizeOptionalText(request.getRootPackage()));
        stage.setRequired(request.isRequired());
        stage.setActive(request.isActive());
        stage.setOpenAiModel(resolveOpenAiModel(request.getOpenAiModelId()));
    }

    /**
     * Normaliza texto opcional para não persistir strings vazias em campos de localização técnica.
     */
    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * Resolve o modelo OpenAI escolhido para a etapa ou remove a escolha quando não informado.
     */
    private OpenAiModel resolveOpenAiModel(Long openAiModelId) {
        if (openAiModelId == null) {
            return null;
        }
        return openAiModelRepository.findById(openAiModelId)
                .orElseThrow(() -> badRequest("Modelo OpenAI não encontrado: " + openAiModelId));
    }

    /**
     * Ordena as etapas carregadas para manter consistência visual e operacional.
     */
    private void sortStages(Pipeline pipeline) {
        pipeline.getStages().sort(Comparator.comparing(PipelineStage::getPosition).thenComparing(PipelineStage::getId));
    }
}
