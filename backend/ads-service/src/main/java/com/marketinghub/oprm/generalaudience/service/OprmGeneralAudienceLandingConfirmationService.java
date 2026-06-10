package com.marketinghub.oprm.generalaudience.service;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceLandingConfirmation;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngle;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubniche;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import com.marketinghub.oprm.generalaudience.service.landingConfirmation.CreateGeneralAudienceLandingConfirmationRequest;
import com.marketinghub.oprm.generalaudience.service.landingConfirmation.GeneralAudienceLandingConfirmationQuestionResponse;
import com.marketinghub.oprm.generalaudience.service.landingConfirmation.GeneralAudienceLandingConfirmationResponse;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceLandingConfirmationRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudiencePainAngleRepository;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsável por registrar no banco a situação de confirmação preparada pelo OPRM sem acionar outros módulos. */
@Service
public class OprmGeneralAudienceLandingConfirmationService {

    private static final String REGISTERED_STATUS = "REGISTERED";

    private final OprmGeneralAudiencePainAngleRepository painAngleRepository;
    private final OprmGeneralAudienceLandingConfirmationRepository landingConfirmationRepository;

    /** Inicializa o serviço somente com repositórios OPRM permitidos pela arquitetura. */
    public OprmGeneralAudienceLandingConfirmationService(
            OprmGeneralAudiencePainAngleRepository painAngleRepository,
            OprmGeneralAudienceLandingConfirmationRepository landingConfirmationRepository) {
        this.painAngleRepository = painAngleRepository;
        this.landingConfirmationRepository = landingConfirmationRepository;
    }

    /** Registra a situação de confirmação sem criar fluxo no Lead Portal ou alterar experimento. */
    @Transactional
    public GeneralAudienceLandingConfirmationResponse createConfirmationFlow(
            Long angleId,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload da landing/formulário é obrigatório");
        }
        OprmGeneralAudiencePainAngle angle = findPainAngle(angleId);
        validateAngleCanCreateConfirmationRecord(angle, request);
        OprmGeneralAudienceSubniche subniche = angle.getSubniche();
        String audienceQuestion = resolveAudienceQuestion(subniche, angle, request);
        List<String> qualificationOptions = normalizeQualificationOptions(request.qualificationOptions());
        String deliveryDescription = resolveDeliveryDescription(angle, request);
        String whyItMakesSense = resolveWhyItMakesSense(angle, request);
        String nextStep = resolveNextStep(request);
        String painQuestion = resolvePainConfirmationQuestion(angle, request);
        OprmGeneralAudienceLandingConfirmation confirmation = landingConfirmationRepository.save(buildConfirmation(
                angle,
                subniche,
                request,
                audienceQuestion,
                qualificationOptions,
                painQuestion,
                deliveryDescription,
                whyItMakesSense,
                nextStep));
        return toResponse(confirmation, qualificationOptions);
    }

    /** Busca o ângulo ou devolve erro HTTP de recurso inexistente. */
    private OprmGeneralAudiencePainAngle findPainAngle(Long angleId) {
        return painAngleRepository.findById(angleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ângulo de público geral não encontrado: " + angleId));
    }

    /** Valida pré-condições para registrar a situação sem materializar outros módulos. */
    private void validateAngleCanCreateConfirmationRecord(
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

    /** Monta o registro OPRM que descreve a situação a ser consumida por etapa posterior. */
    private OprmGeneralAudienceLandingConfirmation buildConfirmation(
            OprmGeneralAudiencePainAngle angle,
            OprmGeneralAudienceSubniche subniche,
            CreateGeneralAudienceLandingConfirmationRequest request,
            String audienceQuestion,
            List<String> qualificationOptions,
            String painQuestion,
            String deliveryDescription,
            String whyItMakesSense,
            String nextStep) {
        OprmGeneralAudienceLandingConfirmation confirmation = new OprmGeneralAudienceLandingConfirmation();
        confirmation.setPainAngle(angle);
        confirmation.setExperimentId(request.experimentId());
        confirmation.setMarketNicheId(subniche.getMarketNicheId());
        confirmation.setName(resolveRecordName(subniche, angle, request));
        confirmation.setSlug(buildSlug(subniche, angle, request.experimentId()));
        confirmation.setAudienceSummary(subniche.getName().trim());
        confirmation.setPainSummary(angle.getPain().trim());
        confirmation.setAudienceConfirmationQuestion(audienceQuestion);
        confirmation.setQualificationOptions(String.join("\n", qualificationOptions));
        confirmation.setPainConfirmationQuestion(painQuestion);
        confirmation.setDeliveryDescription(deliveryDescription);
        confirmation.setWhyItMakesSense(whyItMakesSense);
        confirmation.setNextStep(nextStep);
        confirmation.setStatus(REGISTERED_STATUS);
        return confirmation;
    }

    /** Converte o registro persistido em resposta sem expor dependência do Lead Portal. */
    private GeneralAudienceLandingConfirmationResponse toResponse(
            OprmGeneralAudienceLandingConfirmation confirmation,
            List<String> qualificationOptions) {
        OprmGeneralAudiencePainAngle angle = confirmation.getPainAngle();
        OprmGeneralAudienceSubniche subniche = angle.getSubniche();
        return new GeneralAudienceLandingConfirmationResponse(
                confirmation.getId(),
                angle.getId(),
                subniche.getId(),
                confirmation.getMarketNicheId(),
                confirmation.getExperimentId(),
                confirmation.getSlug(),
                confirmation.getName(),
                confirmation.getAudienceSummary(),
                confirmation.getPainSummary(),
                confirmation.getDeliveryDescription(),
                confirmation.getWhyItMakesSense(),
                confirmation.getNextStep(),
                confirmation.getStatus(),
                List.of(
                        new GeneralAudienceLandingConfirmationQuestionResponse(
                                confirmation.getAudienceConfirmationQuestion(),
                                "audience_confirmation",
                                "SINGLE_CHOICE",
                                true,
                                qualificationOptions),
                        new GeneralAudienceLandingConfirmationQuestionResponse(
                                confirmation.getPainConfirmationQuestion(),
                                "pain_confirmation",
                                "TEXT",
                                true,
                                List.of())));
    }

    /** Resolve a pergunta de confirmação de público a partir do pedido ou dos dados já aprovados. */
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
        return subniche.getQualificationQuestion().trim();
    }

    /** Resolve a descrição de entrega usando primeiro o pedido e depois a isca aprovada. */
    private String resolveDeliveryDescription(
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (StringUtils.hasText(request.deliveryDescription())) {
            return request.deliveryDescription().trim();
        }
        return resolveLeadMagnet(angle);
    }

    /** Resolve o motivo operacional para a confirmação antes de outra etapa usar o registro. */
    private String resolveWhyItMakesSense(
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (StringUtils.hasText(request.whyItMakesSense())) {
            return request.whyItMakesSense().trim();
        }
        if (StringUtils.hasText(angle.getMechanismDirection())) {
            return angle.getMechanismDirection().trim();
        }
        return "Confirmar público real e dor antes de avançar para oferta ou escala.";
    }

    /** Resolve o próximo passo operacional que será registrado para consumo posterior. */
    private String resolveNextStep(CreateGeneralAudienceLandingConfirmationRequest request) {
        if (StringUtils.hasText(request.nextStep())) {
            return request.nextStep().trim();
        }
        return "Registrar respostas, medir qualidade do público e decidir se o experimento deve avançar.";
    }

    /** Resolve a pergunta de confirmação de dor que será persistida. */
    private String resolvePainConfirmationQuestion(
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (StringUtils.hasText(request.painConfirmationQuestion())) {
            return request.painConfirmationQuestion().trim();
        }
        return "Essa dor acontece com você hoje: " + angle.getPain().trim() + "?";
    }

    /** Resolve o nome operacional do registro salvo pelo OPRM. */
    private String resolveRecordName(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLandingConfirmationRequest request) {
        if (StringUtils.hasText(request.name())) {
            return request.name().trim();
        }
        return "Confirmação Público Geral - " + subniche.getName().trim() + " - " + angle.getId();
    }

    /** Normaliza opções de qualificação removendo vazios e duplicados sem serializar JSON em texto. */
    private List<String> normalizeQualificationOptions(List<String> options) {
        if (options == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "qualificationOptions é obrigatório");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String option : options) {
            if (StringUtils.hasText(option)) {
                normalized.add(option.trim());
            }
        }
        if (normalized.size() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "qualificationOptions precisa ter pelo menos duas opções úteis");
        }
        return List.copyOf(normalized);
    }

    /** Resolve a isca ou prova aprovada para a situação de confirmação. */
    private String resolveLeadMagnet(OprmGeneralAudiencePainAngle angle) {
        if (StringUtils.hasText(angle.getProofOrLeadMagnet())) {
            return angle.getProofOrLeadMagnet().trim();
        }
        if (StringUtils.hasText(angle.getSafePromise())) {
            return angle.getSafePromise().trim();
        }
        return null;
    }

    /** Exige texto preenchido para impedir registro operacional incompleto. */
    private void requiredText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " é obrigatório para registrar confirmação");
        }
    }

    /** Monta slug determinístico para o registro OPRM sem publicar rota externa. */
    private String buildSlug(OprmGeneralAudienceSubniche subniche, OprmGeneralAudiencePainAngle angle, Long experimentId) {
        String base = "oprm-publico-geral-" + subniche.getId() + "-" + angle.getId() + "-" + experimentId;
        return normalizeSlug(base);
    }

    /** Normaliza texto para slug compatível com armazenamento e leitura operacional. */
    private String normalizeSlug(String value) {
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            return "oprm-confirmacao";
        }
        return normalized;
    }
}
