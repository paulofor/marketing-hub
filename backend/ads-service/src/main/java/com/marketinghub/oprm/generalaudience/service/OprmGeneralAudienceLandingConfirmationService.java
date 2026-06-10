package com.marketinghub.oprm.generalaudience.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.leadportal.LeadPortalQuestionType;
import com.marketinghub.leadportal.dto.CreateLeadPortalFlowRequest;
import com.marketinghub.leadportal.dto.LeadPortalFlowQuestionRequest;
import com.marketinghub.leadportal.service.LeadPortalFlowService;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngle;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubniche;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import com.marketinghub.oprm.generalaudience.service.landingConfirmation.CreateGeneralAudienceLandingConfirmationRequest;
import com.marketinghub.oprm.generalaudience.service.landingConfirmation.GeneralAudienceLandingConfirmationQuestionResponse;
import com.marketinghub.oprm.generalaudience.service.landingConfirmation.GeneralAudienceLandingConfirmationResponse;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudiencePainAngleRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsável por materializar landing/formulário de confirmação para públicos gerais OPRM. */
@Service
public class OprmGeneralAudienceLandingConfirmationService {

    private final OprmGeneralAudiencePainAngleRepository painAngleRepository;
    private final LeadPortalFlowService leadPortalFlowService;
    private final ExperimentRepository experimentRepository;

    /** Inicializa o serviço com repositórios OPRM e serviço canônico do Lead Portal. */
    public OprmGeneralAudienceLandingConfirmationService(
            OprmGeneralAudiencePainAngleRepository painAngleRepository,
            LeadPortalFlowService leadPortalFlowService,
            ExperimentRepository experimentRepository) {
        this.painAngleRepository = painAngleRepository;
        this.leadPortalFlowService = leadPortalFlowService;
        this.experimentRepository = experimentRepository;
    }

    /** Cria um formulário de confirmação sem publicar landing ou campanha automaticamente. */
    @Transactional
    public GeneralAudienceLandingConfirmationResponse createConfirmationFlow(
            Long angleId,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload da landing/formulário é obrigatório");
        }
        OprmGeneralAudiencePainAngle angle = findPainAngle(angleId);
        validateAngleCanCreateConfirmationFlow(angle, request);
        OprmGeneralAudienceSubniche subniche = angle.getSubniche();
        String audienceQuestion = resolveAudienceQuestion(subniche, angle, request);
        List<String> qualificationOptions = normalizeQualificationOptions(request.qualificationOptions());
        String deliveryDescription = resolveDeliveryDescription(angle, request);
        String whyItMakesSense = resolveWhyItMakesSense(angle, request);
        String nextStep = resolveNextStep(request);
        Experiment experiment = findExperimentForSubniche(request.experimentId(), subniche.getMarketNicheId());
        LeadPortalFlow flow = leadPortalFlowService.create(buildFlowRequest(
                subniche,
                angle,
                request,
                audienceQuestion,
                qualificationOptions,
                deliveryDescription,
                whyItMakesSense,
                nextStep));
        attachFlowToExperiment(experiment, flow);
        return toResponse(
                angle,
                subniche,
                flow,
                request.experimentId(),
                deliveryDescription,
                whyItMakesSense,
                nextStep);
    }

    /** Busca e valida o experimento antes de criar o formulário para evitar fluxo órfão. */
    private Experiment findExperimentForSubniche(Long experimentId, Long marketNicheId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Experimento não encontrado: " + experimentId));
        if (experiment.getNiche() == null || !marketNicheId.equals(experiment.getNiche().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Experimento não pertence ao MarketNiche do subnicho geral");
        }
        return experiment;
    }

    /** Vincula o fluxo criado ao experimento para o próximo passo operacional usar a landing correta. */
    private void attachFlowToExperiment(Experiment experiment, LeadPortalFlow flow) {
        experiment.setLeadPortalFlow(flow);
        experiment.setSchemaFirstLeadPortalEnabled(true);
        experimentRepository.save(experiment);
    }

    /** Busca o ângulo ou devolve erro HTTP de recurso inexistente. */
    private OprmGeneralAudiencePainAngle findPainAngle(Long angleId) {
        return painAngleRepository.findById(angleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ângulo de público geral não encontrado: " + angleId));
    }

    /** Valida pré-condições para a landing confirmar público real e não virar oferta final. */
    private void validateAngleCanCreateConfirmationFlow(
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (request.experimentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "experimentId é obrigatório");
        }
        if (angle.getStatus() != OprmGeneralAudiencePainAngleStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ângulo precisa estar aprovado antes da landing/formulário");
        }
        OprmGeneralAudienceSubniche subniche = angle.getSubniche();
        if (subniche.getStatus() != OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE
                || subniche.getMarketNicheId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subnicho precisa estar convertido em MarketNiche antes da landing/formulário");
        }
        requiredText(subniche.getName(), "subniche.name");
        requiredText(angle.getPain(), "pain");
        requiredText(resolveLeadMagnet(angle), "proofOrLeadMagnet");
        requiredText(resolveAudienceQuestion(subniche, angle, request), "qualificationQuestion");
        normalizeQualificationOptions(request.qualificationOptions());
    }

    /** Monta o contrato canônico do Lead Portal com as perguntas obrigatórias de confirmação. */
    private CreateLeadPortalFlowRequest buildFlowRequest(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request,
            String audienceQuestion,
            List<String> qualificationOptions,
            String deliveryDescription,
            String whyItMakesSense,
            String nextStep) {
        CreateLeadPortalFlowRequest flowRequest = new CreateLeadPortalFlowRequest();
        flowRequest.setName(resolveFlowName(subniche, angle, request));
        flowRequest.setSlug(buildSlug(subniche, angle, request.experimentId()));
        flowRequest.setDescription(buildFlowDescription(subniche, angle, deliveryDescription, whyItMakesSense, nextStep));
        flowRequest.setSchemaFirst(true);
        flowRequest.setMarketNicheId(subniche.getMarketNicheId());
        flowRequest.setExperimentId(request.experimentId());
        flowRequest.setQuestions(buildQuestions(angle, request, audienceQuestion, qualificationOptions));
        return flowRequest;
    }

    /** Monta as perguntas obrigatórias para confirmar pertencimento e dor do lead. */
    private List<LeadPortalFlowQuestionRequest> buildQuestions(
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request,
            String audienceQuestion,
            List<String> qualificationOptions) {
        List<LeadPortalFlowQuestionRequest> questions = new ArrayList<>();
        questions.add(singleChoiceQuestion(
                audienceQuestion,
                "audience_confirmation",
                "Confirma se o lead pertence ao público antes de entregar a isca.",
                qualificationOptions));
        questions.add(textQuestion(
                resolvePainQuestion(angle, request),
                "pain_confirmation",
                "Confirma a dor principal com linguagem real do lead."));
        return questions;
    }

    /** Cria pergunta de escolha única obrigatória para triagem de público. */
    private LeadPortalFlowQuestionRequest singleChoiceQuestion(
            String title,
            String dataKey,
            String description,
            List<String> options) {
        LeadPortalFlowQuestionRequest question = new LeadPortalFlowQuestionRequest();
        question.setTitle(title);
        question.setDataKey(dataKey);
        question.setType(LeadPortalQuestionType.SINGLE_CHOICE);
        question.setRequired(true);
        question.setDescription(description);
        question.setOptions(options);
        return question;
    }

    /** Cria pergunta textual obrigatória para capturar sinal de dor real. */
    private LeadPortalFlowQuestionRequest textQuestion(
            String title,
            String dataKey,
            String description) {
        LeadPortalFlowQuestionRequest question = new LeadPortalFlowQuestionRequest();
        question.setTitle(title);
        question.setDataKey(dataKey);
        question.setType(LeadPortalQuestionType.TEXT);
        question.setRequired(true);
        question.setDescription(description);
        return question;
    }

    /** Monta descrição funcional da landing com todos os pontos exigidos pela etapa 9. */
    private String buildFlowDescription(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            String deliveryDescription,
            String whyItMakesSense,
            String nextStep) {
        return String.join("\n\n",
                "Para quem é: " + subniche.getName(),
                "Qual dor resolve: " + angle.getPain(),
                "O que a pessoa recebe: " + deliveryDescription,
                "Por que faz sentido: " + whyItMakesSense,
                "Próximo passo: " + nextStep,
                "Confirmação obrigatória: o formulário deve verificar se o lead pertence ao público antes de avançar.");
    }

    /** Converte o fluxo criado para resposta específica do OPRM Público Geral. */
    private GeneralAudienceLandingConfirmationResponse toResponse(
            OprmGeneralAudiencePainAngle angle,
            OprmGeneralAudienceSubniche subniche,
            LeadPortalFlow flow,
            Long experimentId,
            String deliveryDescription,
            String whyItMakesSense,
            String nextStep) {
        return new GeneralAudienceLandingConfirmationResponse(
                angle.getId(),
                subniche.getId(),
                subniche.getMarketNicheId(),
                experimentId,
                flow.getId(),
                flow.getSlug(),
                flow.getName(),
                subniche.getName(),
                angle.getPain(),
                deliveryDescription,
                whyItMakesSense,
                nextStep,
                flow.getQuestions().stream().map(this::toQuestionResponse).toList());
    }

    /** Converte pergunta do Lead Portal para resumo auditável no contrato OPRM. */
    private GeneralAudienceLandingConfirmationQuestionResponse toQuestionResponse(LeadPortalFlowQuestion question) {
        return new GeneralAudienceLandingConfirmationQuestionResponse(
                question.getTitle(),
                question.getDataKey(),
                question.getType().name(),
                question.isRequired(),
                List.copyOf(question.getOptions()));
    }

    /** Resolve a pergunta qualificadora priorizando a pergunta específica da landing. */
    private String resolveAudienceQuestion(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (StringUtils.hasText(request.audienceConfirmationQuestion())) {
            return request.audienceConfirmationQuestion().trim();
        }
        if (StringUtils.hasText(angle.getLandingConfirmationQuestion())) {
            return angle.getLandingConfirmationQuestion().trim();
        }
        return subniche.getQualificationQuestion();
    }

    /** Resolve pergunta de dor para validar qualidade do público capturado. */
    private String resolvePainQuestion(
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (StringUtils.hasText(request.painConfirmationQuestion())) {
            return request.painConfirmationQuestion().trim();
        }
        return "Qual é hoje sua maior dificuldade com: " + angle.getPain() + "?";
    }

    /** Resolve o que a pessoa recebe sem permitir formulário vazio. */
    private String resolveDeliveryDescription(
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (StringUtils.hasText(request.deliveryDescription())) {
            return request.deliveryDescription().trim();
        }
        return requiredText(resolveLeadMagnet(angle), "proofOrLeadMagnet");
    }

    /** Resolve a razão de pertinência da landing com base no mecanismo da dor. */
    private String resolveWhyItMakesSense(
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (StringUtils.hasText(request.whyItMakesSense())) {
            return request.whyItMakesSense().trim();
        }
        return StringUtils.hasText(angle.getMechanismDirection())
                ? angle.getMechanismDirection().trim()
                : "A isca foi criada para a dor informada pelo próprio subnicho.";
    }

    /** Resolve próximo passo sem iniciar venda direta automaticamente. */
    private String resolveNextStep(CreateGeneralAudienceLandingConfirmationRequest request) {
        if (StringUtils.hasText(request.nextStep())) {
            return request.nextStep().trim();
        }
        return "Responder o formulário, receber a isca e medir qualidade do lead antes de qualquer oferta.";
    }

    /** Resolve a isca principal cadastrada no ângulo. */
    private String resolveLeadMagnet(OprmGeneralAudiencePainAngle angle) {
        if (StringUtils.hasText(angle.getProofOrLeadMagnet())) {
            return angle.getProofOrLeadMagnet().trim();
        }
        return angle.getSafePromise();
    }

    /** Resolve nome operacional do fluxo criado para o Lead Portal. */
    private String resolveFlowName(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (StringUtils.hasText(request.name())) {
            return request.name().trim();
        }
        return shortText("Confirmação Público Geral - " + subniche.getName() + " - " + angle.getPain(), 150);
    }

    /** Cria slug único e rastreável sem depender de CNAE. */
    private String buildSlug(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            Long experimentId) {
        String base = "oprm-publico-geral-" + subniche.getId() + "-" + angle.getId() + "-" + experimentId;
        return slugify(base + "-" + UUID.randomUUID().toString().substring(0, 8));
    }

    /** Normaliza opções da pergunta qualificadora e exige alternativas suficientes. */
    private List<String> normalizeQualificationOptions(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "qualificationOptions é obrigatório");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                normalized.add(value.trim());
            }
        }
        if (normalized.size() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "qualificationOptions deve ter ao menos duas opções");
        }
        return List.copyOf(normalized);
    }

    /** Normaliza texto obrigatório e rejeita valor vazio. */
    private String requiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " não pode ficar vazio");
        }
        return value.trim();
    }

    /** Encurta texto para respeitar limite operacional do Lead Portal. */
    private String shortText(String value, int maxLength) {
        String text = requiredText(value, "text");
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim();
    }

    /** Converte texto em slug simples para rota pública futura. */
    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return shortText(normalized, 120);
    }
}
