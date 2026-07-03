package com.marketinghub.productai.delivery;

import com.marketinghub.aiprompt.AiPromptSchemaTemplate;
import com.marketinghub.cost.CostAttributionService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.aiprompt.AiPromptSchemaTemplateRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.productai.ProductAiPaidDeliveryStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: controlar fila e auditoria da entrega paga do Produto IA personalizado. */
@Service
public class ProductAiPaidDeliveryService {
    static final String PIPELINE_CODE = "personalizedsample.v1";
    static final String STAGE_CODE = "paid-delivery";
    private static final String STATUS_STARTED = "INICIADO";
    private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_OPENAI";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final String STATUS_FAILED = "FALHA";

    private final ProductAiPaidDeliveryStageExecutionRepository repository;
    private final ExperimentRepository experimentRepository;
    private final AiPromptSchemaTemplateRepository templateRepository;
    private final OpenAiPricingService pricingService;
    private final CostAttributionService costAttributionService;
    private final JdbcTemplate jdbcTemplate;

    /** Inicializa dependências de persistência, custo e consulta operacional. */
    public ProductAiPaidDeliveryService(
            ProductAiPaidDeliveryStageExecutionRepository repository,
            ExperimentRepository experimentRepository,
            AiPromptSchemaTemplateRepository templateRepository,
            OpenAiPricingService pricingService,
            CostAttributionService costAttributionService,
            JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
        this.templateRepository = templateRepository;
        this.pricingService = pricingService;
        this.costAttributionService = costAttributionService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Enfileira entrega paga após confirmação de compra aprovada. */
    @Transactional
    public ProductAiPaidDeliveryDtos.EnqueueResponse enqueueApprovedPurchase(
            ProductAiPaidDeliveryDtos.PurchaseApprovedRequest request) {
        PurchaseContext context = loadPurchaseContext(request.purchaseId(), request.packageId());
        Experiment experiment = experimentRepository.findById(context.experimentId())
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + context.experimentId()));
        if (experiment.getProductAiSubtype() != ProductAiSubtype.AI_PERSONALIZED_SAMPLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Compra não pertence a AI_PERSONALIZED_SAMPLE.");
        }
        ProductAiPaidDeliveryStageExecution execution = repository
                .findTopByPurchaseIdOrderByExecutionRequestedAtDesc(context.purchaseId())
                .filter(existing -> !STATUS_FAILED.equals(existing.getStatus()))
                .orElseGet(() -> createExecution(context, experiment));
        return new ProductAiPaidDeliveryDtos.EnqueueResponse(execution.getIdJob(), execution.getStatus());
    }

    /** Lista entregas pendentes para consumo exclusivo do product-ai-worker. */
    @Transactional(readOnly = true)
    public List<ProductAiPaidDeliveryDtos.PendingResponse> pending() {
        AiPromptSchemaTemplate template = loadTemplate();
        return repository.findTop20ByPipelineCodeAndStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                        PIPELINE_CODE, STAGE_CODE, STATUS_STARTED)
                .stream()
                .map(execution -> toPending(execution, template, loadPurchaseContext(execution.getPurchaseId(), execution.getPackageId())))
                .toList();
    }

    /** Registra o request bruto antes da chamada OpenAI feita pelo worker. */
    @Transactional
    public void receiveRequest(String idJob, ProductAiPaidDeliveryDtos.ReceiveRequestRequest request) {
        ProductAiPaidDeliveryStageExecution execution = findExecution(idJob);
        execution.setStatus(STATUS_WAITING);
        execution.setPrompt(request.prompt());
        execution.setSchemaJson(request.schemaJson());
        execution.setOpenAiRequestBody(request.requestBodyJson());
        execution.setOpenAiModel(request.openAiModel());
        execution.setServiceTier(request.serviceTier());
        execution.setProcessingStartedAt(Instant.now());
        repository.save(execution);
    }

    /** Registra o resultado funcional, calcula custo autoritativo e marca compra como entregue. */
    @Transactional
    public void receiveResponse(String idJob, ProductAiPaidDeliveryDtos.ReceiveResponseRequest request) {
        ProductAiPaidDeliveryStageExecution execution = findExecution(idJob);
        if (StringUtils.hasText(request.errorMessage())) {
            execution.setStatus(STATUS_FAILED);
            execution.setErrorMessage(request.errorMessage());
            repository.save(execution);
            return;
        }
        BigDecimal costUsd = calculateCost(request);
        execution.setStatus(STATUS_COMPLETED);
        execution.setOpenAiResponseBody(request.responseBodyJson());
        execution.setFunctionalOutput(request.functionalOutputJson());
        execution.setArtifactUrl(request.artifactUrl());
        execution.setOpenAiModel(request.openAiModel());
        execution.setServiceTier(request.serviceTier());
        execution.setInputTokens(request.inputTokens());
        execution.setOutputTokens(request.outputTokens());
        execution.setCostUsd(costUsd);
        execution.setCompletedAt(Instant.now());
        repository.save(execution);
        experimentRepository.findById(execution.getExperimentId())
                .ifPresent(experiment -> costAttributionService.addUsdCostToExperimentHierarchy(experiment, costUsd));
        markPurchaseDelivered(execution.getPurchaseId(), request.artifactUrl());
    }

    /** Cria execução inicial associada ao template ativo do pipeline. */
    private ProductAiPaidDeliveryStageExecution createExecution(PurchaseContext context, Experiment experiment) {
        AiPromptSchemaTemplate template = loadTemplate();
        ProductAiPaidDeliveryStageExecution execution = ProductAiPaidDeliveryStageExecution.builder()
                .idJob(UUID.randomUUID().toString())
                .purchaseId(context.purchaseId())
                .packageId(context.packageId())
                .experimentId(experiment.getId())
                .pipelineCode(PIPELINE_CODE)
                .stageCode(STAGE_CODE)
                .status(STATUS_STARTED)
                .executionRequestedAt(Instant.now())
                .promptTemplateKey(template.getTemplateKey())
                .promptTemplateVersion(template.getVersion())
                .schemaName(template.getSchemaName())
                .build();
        return repository.save(execution);
    }

    /** Monta o contrato de pending com dados funcionais e template do backend. */
    private ProductAiPaidDeliveryDtos.PendingResponse toPending(
            ProductAiPaidDeliveryStageExecution execution,
            AiPromptSchemaTemplate template,
            PurchaseContext context) {
        return new ProductAiPaidDeliveryDtos.PendingResponse(
                execution.getIdJob(),
                execution.getPurchaseId(),
                execution.getPackageId(),
                execution.getExperimentId(),
                execution.getPipelineCode(),
                execution.getStageCode(),
                execution.getExecutionRequestedAt(),
                Map.of("name", nullToEmpty(context.buyerName()), "email", nullToEmpty(context.buyerEmail())),
                context.personalizationInput(),
                context.experimentPayload(),
                templatePayload(template));
    }

    /** Carrega o template ativo persistido no banco. */
    private AiPromptSchemaTemplate loadTemplate() {
        return templateRepository.findFirstByPipelineCodeAndStageCodeAndActiveTrueOrderByVersionDesc(
                        PIPELINE_CODE, STAGE_CODE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Template ativo de prompt/schema não encontrado para " + PIPELINE_CODE + "/" + STAGE_CODE));
    }

    /** Busca a execução pelo idJob do contrato interno. */
    private ProductAiPaidDeliveryStageExecution findExecution(String idJob) {
        return repository.findTopByIdJobOrderByExecutionRequestedAtDesc(idJob)
                .orElseThrow(() -> new EntityNotFoundException("Product AI paid delivery job not found: " + idJob));
    }

    /** Calcula custo canônico conforme o service tier usado na tentativa. */
    private BigDecimal calculateCost(ProductAiPaidDeliveryDtos.ReceiveResponseRequest request) {
        OpenAiResponse.OpenAiUsage usage =
                new OpenAiResponse.OpenAiUsage(request.inputTokens(), request.outputTokens(), null, null, null);
        if ("flex".equalsIgnoreCase(request.serviceTier())) {
            return pricingService.estimateFlexCost(request.openAiModel(), request.inputTokens(), request.outputTokens());
        }
        return pricingService.estimateStandardCost(request.openAiModel(), usage);
    }

    /** Marca a compra como entregue quando o produto personalizado foi gerado. */
    private void markPurchaseDelivered(Long purchaseId, String artifactUrl) {
        jdbcTemplate.update("""
                UPDATE lead_portal_purchase
                   SET status = 'DELIVERED',
                       delivered_at = UTC_TIMESTAMP(),
                       zip_object_key = COALESCE(?, zip_object_key),
                       updated_at = UTC_TIMESTAMP()
                 WHERE id = ?
                """, artifactUrl, purchaseId);
    }

    /** Lê compra, pacote, experimento e respostas do formulário para formar contexto funcional. */
    private PurchaseContext loadPurchaseContext(Long purchaseId, Long packageId) {
        return jdbcTemplate.queryForObject("""
                SELECT p.id AS purchase_id,
                       p.package_id,
                       p.buyer_name,
                       p.buyer_email,
                       pack.submission_id,
                       sub.name AS submission_name,
                       sub.email AS submission_email,
                       sub.answers AS submission_answers,
                       flow.id AS flow_id,
                       COALESCE(exp.id, flow_exp.id) AS experiment_id,
                       COALESCE(exp.name, flow_exp.name) AS experiment_name,
                       COALESCE(exp.hypothesis, flow_exp.hypothesis) AS hypothesis,
                       COALESCE(exp.single_pain, flow_exp.single_pain) AS single_pain,
                       COALESCE(exp.funnel_promise, flow_exp.funnel_promise) AS funnel_promise,
                       COALESCE(exp.primary_cta, flow_exp.primary_cta) AS primary_cta,
                       COALESCE(exp.unit_price_brl, flow_exp.unit_price_brl) AS unit_price_brl
                  FROM lead_portal_purchase p
                  JOIN flow_submission_image_package pack ON pack.id = p.package_id
                  JOIN flow_submissions sub ON sub.id = pack.submission_id
                  LEFT JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
                  LEFT JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
                  LEFT JOIN experiment flow_exp ON flow.experiment_id = flow_exp.id
                 WHERE p.id = ? AND p.package_id = ?
                """, (rs, rowNum) -> {
                    Long experimentId = rs.getLong("experiment_id");
                    Map<String, Object> experiment = new LinkedHashMap<>();
                    experiment.put("id", experimentId);
                    experiment.put("name", rs.getString("experiment_name"));
                    experiment.put("hypothesis", rs.getString("hypothesis"));
                    experiment.put("singlePain", rs.getString("single_pain"));
                    experiment.put("funnelPromise", rs.getString("funnel_promise"));
                    experiment.put("primaryCta", rs.getString("primary_cta"));
                    experiment.put("unitPriceBrl", rs.getBigDecimal("unit_price_brl"));
                    Map<String, Object> personalization = new LinkedHashMap<>();
                    personalization.put("submissionId", rs.getString("submission_id"));
                    personalization.put("name", rs.getString("submission_name"));
                    personalization.put("email", rs.getString("submission_email"));
                    personalization.put("answers", rs.getString("submission_answers"));
                    return new PurchaseContext(
                            rs.getLong("purchase_id"),
                            rs.getLong("package_id"),
                            experimentId,
                            rs.getString("buyer_name"),
                            rs.getString("buyer_email"),
                            personalization,
                            experiment);
                }, purchaseId, packageId);
    }

    /** Monta payload do template versionado entregue ao worker. */
    private Map<String, Object> templatePayload(AiPromptSchemaTemplate template) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("templateKey", template.getTemplateKey());
        payload.put("version", template.getVersion());
        payload.put("model", template.getOpenAiModel());
        payload.put("schemaName", template.getSchemaName());
        payload.put("promptMarkdownContent", template.getPromptMarkdownContent());
        payload.put("schemaJson", template.getSchemaJson());
        return payload;
    }

    /** Normaliza texto nulo para contratos JSON. */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record PurchaseContext(
            Long purchaseId,
            Long packageId,
            Long experimentId,
            String buyerName,
            String buyerEmail,
            Map<String, Object> personalizationInput,
            Map<String, Object> experimentPayload) {}
}
