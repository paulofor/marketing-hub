package com.marketinghub.gerasalespage.v1.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationStageAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.integration.LeadPortalPublicationException;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationStageAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: criar e consultar snapshots historicos das paginas publicadas pelo GeraSalesPage v1. */
@Service
public class GeraSalesPagePublicationAuditService {
    private static final Logger log = LoggerFactory.getLogger(GeraSalesPagePublicationAuditService.class);
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final Pattern IFRAME_BLOCK_PATTERN =
            Pattern.compile("(?is)<iframe\\b[^>]*>.*?</iframe>");

    private final ExperimentRepository experimentRepository;
    private final GeraSalesPageStageExecutionRepository executionRepository;
    private final GeraSalesPagePublicationAuditRepository publicationRepository;
    private final GeraSalesPagePublicationStageAuditRepository publicationStageRepository;
    private final LeadPortalFlowPublisher leadPortalFlowPublisher;
    private final LeadPortalPublicUrlResolver leadPortalPublicUrlResolver;
    private final ObjectMapper objectMapper;

    /** Inicializa o service com repositorios e serializador usados nos snapshots. */
    public GeraSalesPagePublicationAuditService(
            ExperimentRepository experimentRepository,
            GeraSalesPageStageExecutionRepository executionRepository,
            GeraSalesPagePublicationAuditRepository publicationRepository,
            GeraSalesPagePublicationStageAuditRepository publicationStageRepository,
            LeadPortalFlowPublisher leadPortalFlowPublisher,
            LeadPortalPublicUrlResolver leadPortalPublicUrlResolver,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.executionRepository = executionRepository;
        this.publicationRepository = publicationRepository;
        this.publicationStageRepository = publicationStageRepository;
        this.leadPortalFlowPublisher = leadPortalFlowPublisher;
        this.leadPortalPublicUrlResolver = leadPortalPublicUrlResolver;
        this.objectMapper = objectMapper;
    }

    /** Cria snapshot da publicacao final quando a etapa de pacote termina com sucesso. */
    @Transactional
    public void snapshotPublication(GeraSalesPageStageExecution publicationExecution) {
        if (!isCompletedPublication(publicationExecution)
                || publicationRepository.existsByPublicationJobId(publicationExecution.getIdJob())) {
            return;
        }
        Experiment experiment = experimentRepository.findById(publicationExecution.getExperimentId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Experiment not found: " + publicationExecution.getExperimentId()));
        saveAudit(experiment, publicationExecution);
    }

    /** Lista publicacoes historicas e faz backfill quando houver execucao final antiga sem snapshot. */
    @Transactional
    public List<GeraSalesPagePublicationResponse> listPublications(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experiment not found: " + experimentId));
        backfillMissingSnapshots(experiment);
        return publicationRepository.findByExperimentIdOrderByPublishedAtDesc(experimentId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Retorna o checkout auditado mais recente para rebuild sem confundir com a URL pública da página. */
    @Transactional(readOnly = true)
    public String latestCheckoutUrl(Long experimentId) {
        if (experimentId == null) {
            return null;
        }
        return publicationRepository.findTopByExperimentIdOrderByPublishedAtDesc(experimentId)
                .map(GeraSalesPagePublicationAudit::getCheckoutUrl)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    /** Cria snapshots para publicacoes antigas que ainda possuem execucoes auditaveis no banco. */
    private void backfillMissingSnapshots(Experiment experiment) {
        executionRepository.findByExperimentIdAndStageCodeAndStatusOrderByExecutionRequestedAtAsc(
                        experiment.getId(), GeraSalesPageStageCode.PUBLICATION_PACKAGE.code(), STATUS_COMPLETED)
                .stream()
                .filter(execution -> !publicationRepository.existsByPublicationJobId(execution.getIdJob()))
                .forEach(execution -> saveAudit(experiment, execution));
    }

    /** Salva uma publicação e suas etapas auditadas em tabelas normalizadas. */
    private void saveAudit(
            Experiment experiment,
            GeraSalesPageStageExecution publicationExecution) {
        List<GeraSalesPageStageExecution> stageExecutions =
                executionRepository.findByExperimentIdOrderByExecutionRequestedAtAsc(experiment.getId()).stream()
                        .filter(execution -> GeraSalesPageStageCode.contains(execution.getStageCode()))
                        .filter(execution -> STATUS_COMPLETED.equals(execution.getStatus()))
                        .filter(execution -> !execution.getExecutionRequestedAt()
                                .isAfter(publicationExecution.getExecutionRequestedAt()))
                        .toList();
        String packageJson = publicationExecution.getModelResponse();
        Map<String, Object> packagePayload = parseObject(packageJson);
        String html = stringValue(packagePayload.get("html"));
        String checkoutUrl = stringValue(packagePayload.get("checkoutUrl"));
        String salesPageUrl = firstText(
                packagePayload.get("salesPageUrl"),
                packagePayload.get("publicUrl"),
                packagePayload.get("publishedUrl"),
                packagePayload.get("pageUrl"));
        PersonalizedSamplePublication personalizedSamplePublication =
                publishPersonalizedSampleSalesPageIfNeeded(experiment, html);
        boolean personalizedSampleSalesPage = personalizedSamplePublication != null;
        if (personalizedSamplePublication != null) {
            html = personalizedSamplePublication.html();
            salesPageUrl = personalizedSamplePublication.salesPageUrl();
            checkoutUrl = null;
        }
        Instant publishedAt = publicationExecution.getCompletedAt() != null
                ? publicationExecution.getCompletedAt()
                : Instant.now();
        GeraSalesPagePublicationAudit audit = publicationRepository.save(GeraSalesPagePublicationAudit.builder()
                .experimentId(experiment.getId())
                .publicationJobId(publicationExecution.getIdJob())
                .publishedAt(publishedAt)
                .salesPageUrl(StringUtils.hasText(salesPageUrl) ? salesPageUrl : experiment.getFollowUpActionUrl())
                .checkoutUrl(resolveAuditCheckoutUrl(checkoutUrl, experiment, personalizedSampleSalesPage))
                .html(html)
                .publicationPackageJson(packageJson)
                .createdAt(Instant.now())
                .build());
        publicationStageRepository.saveAll(toStageAudits(audit.getId(), stageExecutions));
    }

    /** Define o checkout auditado sem confundir funil Produto IA com checkout direto. */
    private String resolveAuditCheckoutUrl(
            String checkoutUrl,
            Experiment experiment,
            boolean personalizedSampleSalesPage) {
        if (personalizedSampleSalesPage) {
            return StringUtils.hasText(checkoutUrl) ? checkoutUrl : null;
        }
        return StringUtils.hasText(checkoutUrl) ? checkoutUrl : experiment.getFollowUpActionUrl();
    }

    /** Publica a pagina aprovada dentro do funil canonico quando o produto exige personalizacao. */
    private PersonalizedSamplePublication publishPersonalizedSampleSalesPageIfNeeded(
            Experiment experiment,
            String html) {
        if (experiment.getProductAiSubtype() != ProductAiSubtype.AI_PERSONALIZED_SAMPLE) {
            return null;
        }
        LeadPortalFlow flow = experiment.getLeadPortalFlow();
        if (flow == null || flow.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Produto IA personalizado exige funil Lead Portal aprovado antes da publicacao da pagina.");
        }
        String htmlWithManagedForm = ensureManagedFormAnchor(html);
        htmlWithManagedForm = removeSelfReferentialLeadPortalIframes(htmlWithManagedForm, flow);
        flow.setCustomFormHtml(buildPersonalizedSampleTemplatePayload(flow, htmlWithManagedForm));
        flow.setSchemaFirst(true);
        flow.setApproved(true);
        if (flow.getApprovedAt() == null) {
            flow.setApprovedAt(Instant.now());
        }
        flow.setModel("AI_PERSONALIZED_SAMPLE_GERA_SALES_PAGE");
        if (!StringUtils.hasText(flow.getPrompt())) {
            flow.setPrompt("Pipeline: gera-sales-page-v1/publication-package -> product-ai personalized sample funnel");
        }
        try {
            leadPortalFlowPublisher.publish(flow);
        } catch (LeadPortalPublicationException ex) {
            log.error("Falha ao publicar pagina GeraSalesPage no funil Produto IA: experimentId={}, flowId={}, slug={}",
                    experiment.getId(), flow.getId(), flow.getSlug(), ex);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Falha ao publicar pagina de venda no funil Produto IA.",
                    ex);
        }
        String publicUrl = leadPortalPublicUrlResolver.resolve(flow);
        experiment.setFollowUpActionUrl(publicUrl);
        return new PersonalizedSamplePublication(publicUrl, htmlWithManagedForm);
    }

    /** Remove iframe que aponta para o proprio funil e causaria recursao visual no Lead Portal. */
    private String removeSelfReferentialLeadPortalIframes(String html, LeadPortalFlow flow) {
        if (!StringUtils.hasText(html)) {
            return html;
        }
        Matcher matcher = IFRAME_BLOCK_PATTERN.matcher(html);
        StringBuffer cleaned = new StringBuffer();
        while (matcher.find()) {
            String iframe = matcher.group();
            if (isSelfReferentialLeadPortalIframe(iframe, flow)) {
                matcher.appendReplacement(cleaned, "");
            } else {
                matcher.appendReplacement(cleaned, Matcher.quoteReplacement(iframe));
            }
        }
        matcher.appendTail(cleaned);
        return cleaned.toString();
    }

    /** Identifica iframes que tentam embutir o mesmo fluxo publicado pelo Lead Portal. */
    private boolean isSelfReferentialLeadPortalIframe(String iframe, LeadPortalFlow flow) {
        String normalized = iframe.toLowerCase(java.util.Locale.ROOT);
        String slug = flow != null && StringUtils.hasText(flow.getSlug())
                ? flow.getSlug().toLowerCase(java.util.Locale.ROOT)
                : "";
        return (StringUtils.hasText(slug) && normalized.contains(slug))
                || normalized.contains("/flows/")
                || normalized.contains("lead-portal");
    }

    /** Garante que o runtime publico tenha um form alvo para renderizar as perguntas do Lead Portal. */
    private String ensureManagedFormAnchor(String html) {
        String normalized = StringUtils.hasText(html) ? html : "";
        if (normalized.toLowerCase().contains("<form")) {
            return normalized;
        }
        String formAnchor = """
                <section id="personalized-sample-form-section">
                  <form id="lead-portal-personalized-sample-form"></form>
                </section>
                """;
        if (normalized.toLowerCase().contains("</body>")) {
            return normalized.replaceFirst("(?i)</body>", formAnchor + "\n</body>");
        }
        return normalized + "\n" + formAnchor;
    }

    /** Serializa HTML e especificacao de formulario no contrato reconhecido pelo Lead Portal. */
    private String buildPersonalizedSampleTemplatePayload(LeadPortalFlow flow, String html) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("htmlDocument", html);
        payload.put("formSpec", buildFormSpec(flow));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.error("Falha ao serializar template da pagina Produto IA: flowId={}, slug={}",
                    flow.getId(), flow.getSlug(), ex);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao preparar template da pagina Produto IA.",
                    ex);
        }
    }

    /** Converte as perguntas canonicas do fluxo em campos gerenciados pelo runtime publico. */
    private Map<String, Object> buildFormSpec(LeadPortalFlow flow) {
        Map<String, Object> formSpec = new LinkedHashMap<>();
        formSpec.put("formId", "lead-portal-personalized-sample-form");
        formSpec.put("title", "Receba sua amostra personalizada");
        formSpec.put("submitLabel", "Gerar minha amostra personalizada");
        formSpec.put("fields", flow.getQuestions().stream()
                .map(this::toManagedFormField)
                .toList());
        formSpec.put("successState", Map.of(
                "title", "Dados recebidos",
                "message", "Vamos preparar sua amostra personalizada com base nas suas respostas."));
        formSpec.put("consent", Map.of(
                "enabled", true,
                "required", true,
                "label", "Autorizo o uso das minhas respostas para gerar minha amostra personalizada."));
        return formSpec;
    }

    /** Converte uma pergunta do Lead Portal em campo aceito pelo template gerenciado. */
    private Map<String, Object> toManagedFormField(LeadPortalFlowQuestion question) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", question.getDataKey());
        field.put("type", toManagedFieldType(question.getType()));
        field.put("label", question.getTitle());
        field.put("required", question.isRequired());
        field.put("placeholder", question.getPlaceholder());
        return field;
    }

    /** Mapeia tipos do Lead Portal para os tipos simples suportados pelo runtime customizado. */
    private String toManagedFieldType(LeadPortalQuestionType type) {
        if (type == LeadPortalQuestionType.EMAIL) {
            return "email";
        }
        if (type == LeadPortalQuestionType.PHONE) {
            return "tel";
        }
        return "text";
    }

    /** Retorna o primeiro texto preenchido entre os campos candidatos. */
    private String firstText(Object... values) {
        for (Object value : values) {
            String text = stringValue(value);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    /** Converte execuções concluídas em linhas auditáveis da publicação. */
    private List<GeraSalesPagePublicationStageAudit> toStageAudits(
            Long publicationAuditId,
            List<GeraSalesPageStageExecution> executions) {
        return java.util.stream.IntStream.range(0, executions.size())
                .mapToObj(index -> toStageAudit(publicationAuditId, executions.get(index), index + 1))
                .toList();
    }

    /** Converte uma execução de etapa em snapshot normalizado. */
    private GeraSalesPagePublicationStageAudit toStageAudit(
            Long publicationAuditId,
            GeraSalesPageStageExecution execution,
            int stageOrder) {
        return GeraSalesPagePublicationStageAudit.builder()
                .publicationAuditId(publicationAuditId)
                .stageOrder(stageOrder)
                .idJob(execution.getIdJob())
                .stageCode(execution.getStageCode())
                .status(execution.getStatus())
                .completedAt(execution.getCompletedAt())
                .promptTemplateKey(execution.getPromptTemplateKey())
                .prompt(execution.getPrompt())
                .promptMarkdownContent(execution.getPromptMarkdownContent())
                .schemaJson(execution.getSchemaJson())
                .openAiModel(execution.getOpenAiModel())
                .openAiRequestBody(execution.getOpenAiRequestBody())
                .modelResponse(execution.getModelResponse())
                .rawResponse(execution.getRawResponse())
                .inputTokens(execution.getInputTokens())
                .outputTokens(execution.getOutputTokens())
                .costUsd(execution.getCostUsd())
                .build();
    }

    /** Converte entidade persistida em resposta usada pelo frontend. */
    private GeraSalesPagePublicationResponse toResponse(GeraSalesPagePublicationAudit audit) {
        return new GeraSalesPagePublicationResponse(
                audit.getId(),
                audit.getExperimentId(),
                audit.getPublicationJobId(),
                audit.getPublishedAt(),
                audit.getSalesPageUrl(),
                audit.getCheckoutUrl(),
                audit.getHtml(),
                audit.getPublicationPackageJson(),
                publicationStageRepository.findByPublicationAuditIdOrderByStageOrderAsc(audit.getId()).stream()
                        .map(this::toStageResponse)
                        .toList());
    }

    /** Converte snapshot normalizado de etapa em contrato de leitura. */
    private GeraSalesPagePublicationStageResponse toStageResponse(GeraSalesPagePublicationStageAudit stage) {
        return new GeraSalesPagePublicationStageResponse(
                stage.getIdJob(),
                stage.getStageCode(),
                stage.getStatus(),
                stage.getCompletedAt(),
                stage.getPromptTemplateKey(),
                stage.getPrompt(),
                stage.getPromptMarkdownContent(),
                stage.getSchemaJson(),
                stage.getOpenAiModel(),
                stage.getOpenAiRequestBody(),
                stage.getModelResponse(),
                stage.getRawResponse(),
                stage.getInputTokens(),
                stage.getOutputTokens(),
                stage.getCostUsd());
    }

    /** Indica se a execução é a etapa final concluída. */
    private boolean isCompletedPublication(GeraSalesPageStageExecution execution) {
        return execution != null
                && GeraSalesPageStageCode.PUBLICATION_PACKAGE.code().equals(execution.getStageCode())
                && STATUS_COMPLETED.equals(execution.getStatus());
    }

    /** Converte JSON textual em mapa quando possível. */
    private Map<String, Object> parseObject(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            log.debug("Pacote de publicacao do GeraSalesPage v1 nao estava em JSON.", ex);
            return Map.of();
        }
    }

    /** Extrai string de campo opcional do pacote final. */
    private String stringValue(Object value) {
        return value instanceof String text ? text : null;
    }

    /** Resultado interno da publicacao no funil Produto IA personalizado. */
    private record PersonalizedSamplePublication(String salesPageUrl, String html) {
    }
}
