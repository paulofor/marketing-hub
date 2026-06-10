package com.marketinghub.oprm.generalaudience.service;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngle;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeed;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSourceEvidence;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubniche;
import com.marketinghub.oprm.generalaudience.service.createPainAngle.CreateGeneralAudiencePainAngleRequest;
import com.marketinghub.oprm.generalaudience.service.createSourceEvidence.CreateGeneralAudienceSourceEvidenceRequest;
import com.marketinghub.oprm.generalaudience.service.listPainAngles.GeneralAudiencePainAngleResponse;
import com.marketinghub.oprm.generalaudience.service.listSourceEvidences.GeneralAudienceSourceEvidenceResponse;
import com.marketinghub.oprm.generalaudience.service.qualityGate.GeneralAudienceQualityGateResponse;
import com.marketinghub.oprm.generalaudience.service.updatePainAngle.UpdateGeneralAudiencePainAngleRequest;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudiencePainAngleRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSeedRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSourceEvidenceRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSubnicheRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsável pelo pipeline de descoberta de subnichos, dores, evidências e quality gate de públicos gerais. */
@Service
public class OprmGeneralAudienceDiscoveryService {

    private static final Set<String> GENERIC_TERMS = Set.of("marketing digital", "renda extra", "beleza", "relacionamento");
    private static final Set<String> RISKY_PROMISE_TERMS = Set.of(
            "garantido", "garantida", "certeza", "sem esforço", "resultado absoluto", "renda garantida");
    private final OprmGeneralAudienceSeedRepository seedRepository;
    private final OprmGeneralAudienceSubnicheRepository subnicheRepository;
    private final OprmGeneralAudiencePainAngleRepository painAngleRepository;
    private final OprmGeneralAudienceSourceEvidenceRepository sourceEvidenceRepository;

    /** Inicializa o serviço com repositórios centralizados do módulo OPRM. */
    public OprmGeneralAudienceDiscoveryService(
            OprmGeneralAudienceSeedRepository seedRepository,
            OprmGeneralAudienceSubnicheRepository subnicheRepository,
            OprmGeneralAudiencePainAngleRepository painAngleRepository,
            OprmGeneralAudienceSourceEvidenceRepository sourceEvidenceRepository) {
        this.seedRepository = seedRepository;
        this.subnicheRepository = subnicheRepository;
        this.painAngleRepository = painAngleRepository;
        this.sourceEvidenceRepository = sourceEvidenceRepository;
    }

    /** Lista ângulos de dor de um subnicho para revisão antes de construir oferta. */
    @Transactional(readOnly = true)
    public List<GeneralAudiencePainAngleResponse> listPainAngles(Long subnicheId) {
        findSubniche(subnicheId);
        return painAngleRepository.findAllBySubnicheIdOrderByUpdatedAtDesc(subnicheId).stream()
                .map(this::toPainAngleResponse)
                .toList();
    }

    /** Cadastra dor e ângulo testável sem publicar campanha ou criar hipótese automaticamente. */
    @Transactional
    public GeneralAudiencePainAngleResponse createPainAngle(
            Long subnicheId,
            CreateGeneralAudiencePainAngleRequest request) {
        OprmGeneralAudienceSubniche subniche = findSubniche(subnicheId);
        OprmGeneralAudiencePainAngle angle = new OprmGeneralAudiencePainAngle();
        angle.setSubniche(subniche);
        angle.setPain(requiredText(request.pain(), "pain"));
        angle.setDesiredResult(requiredText(request.desiredResult(), "desiredResult"));
        angle.setMechanismDirection(normalizeOptionalText(request.mechanismDirection()));
        angle.setProofOrLeadMagnet(normalizeOptionalText(request.proofOrLeadMagnet()));
        angle.setSafePromise(normalizeOptionalText(request.safePromise()));
        angle.setFirstAdHook(normalizeOptionalText(request.firstAdHook()));
        angle.setLandingConfirmationQuestion(normalizeOptionalText(request.landingConfirmationQuestion()));
        angle.setComplianceNotes(normalizeOptionalText(request.complianceNotes()));
        angle.setStatus(request.status() == null ? OprmGeneralAudiencePainAngleStatus.DISCOVERED : request.status());
        validateSafeAngle(angle);
        return toPainAngleResponse(painAngleRepository.save(angle));
    }

    /** Atualiza um ângulo mantendo a validação contra saída genérica ou promessa arriscada. */
    @Transactional
    public GeneralAudiencePainAngleResponse updatePainAngle(
            Long angleId,
            UpdateGeneralAudiencePainAngleRequest request) {
        OprmGeneralAudiencePainAngle angle = findPainAngle(angleId);
        if (request.pain() != null) {
            angle.setPain(requiredText(request.pain(), "pain"));
        }
        if (request.desiredResult() != null) {
            angle.setDesiredResult(requiredText(request.desiredResult(), "desiredResult"));
        }
        if (request.mechanismDirection() != null) {
            angle.setMechanismDirection(normalizeOptionalText(request.mechanismDirection()));
        }
        if (request.proofOrLeadMagnet() != null) {
            angle.setProofOrLeadMagnet(normalizeOptionalText(request.proofOrLeadMagnet()));
        }
        if (request.safePromise() != null) {
            angle.setSafePromise(normalizeOptionalText(request.safePromise()));
        }
        if (request.firstAdHook() != null) {
            angle.setFirstAdHook(normalizeOptionalText(request.firstAdHook()));
        }
        if (request.landingConfirmationQuestion() != null) {
            angle.setLandingConfirmationQuestion(normalizeOptionalText(request.landingConfirmationQuestion()));
        }
        if (request.complianceNotes() != null) {
            angle.setComplianceNotes(normalizeOptionalText(request.complianceNotes()));
        }
        if (request.status() != null) {
            angle.setStatus(request.status());
        }
        validateSafeAngle(angle);
        return toPainAngleResponse(painAngleRepository.save(angle));
    }

    /** Aprova um ângulo somente quando ele passa pela validação de qualidade e promessa segura. */
    @Transactional
    public GeneralAudiencePainAngleResponse approvePainAngle(Long angleId) {
        OprmGeneralAudiencePainAngle angle = findPainAngle(angleId);
        angle.setStatus(OprmGeneralAudiencePainAngleStatus.APPROVED);
        validateSafeAngle(angle);
        return toPainAngleResponse(painAngleRepository.save(angle));
    }

    /** Lista evidências agregadas de uma semente para auditoria do mapeamento. */
    @Transactional(readOnly = true)
    public List<GeneralAudienceSourceEvidenceResponse> listSeedEvidences(Long seedId) {
        findSeed(seedId);
        return sourceEvidenceRepository.findAllBySeedIdOrderByCapturedAtDesc(seedId).stream()
                .map(this::toSourceEvidenceResponse)
                .toList();
    }

    /** Registra evidência agregada e rastreável sem persistir comentários integrais ou dados pessoais. */
    @Transactional
    public GeneralAudienceSourceEvidenceResponse createSourceEvidence(
            Long seedId,
            CreateGeneralAudienceSourceEvidenceRequest request) {
        OprmGeneralAudienceSeed seed = findSeed(seedId);
        OprmGeneralAudienceSubniche subniche = null;
        if (request.subnicheId() != null) {
            subniche = findSubniche(request.subnicheId());
            if (!seed.getId().equals(subniche.getSeed().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "subnicheId não pertence à semente informada");
            }
        }
        OprmGeneralAudienceSourceEvidence evidence = new OprmGeneralAudienceSourceEvidence();
        evidence.setSeed(seed);
        evidence.setSubniche(subniche);
        evidence.setSourceUrl(normalizeOptionalText(request.sourceUrl()));
        evidence.setSourceDomain(normalizeOptionalText(request.sourceDomain()));
        evidence.setSourceType(normalizeOptionalText(request.sourceType()));
        evidence.setEvidenceSummary(requiredText(request.evidenceSummary(), "evidenceSummary"));
        evidence.setCapturedAt(request.capturedAt());
        return toSourceEvidenceResponse(sourceEvidenceRepository.save(evidence));
    }

    /** Executa o quality gate de um subnicho para bloquear saída genérica antes de experimento. */
    @Transactional(readOnly = true)
    public GeneralAudienceQualityGateResponse evaluateQualityGate(Long subnicheId) {
        OprmGeneralAudienceSubniche subniche = findSubniche(subnicheId);
        List<String> blockers = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        requireText(blockers, subniche.getPersonaSummary(), "Persona/contexto do subnicho ausente.");
        requireText(blockers, subniche.getPainSummary(), "Dor principal do subnicho ausente.");
        requireText(blockers, subniche.getQualificationQuestion(), "Pergunta qualificadora obrigatória ausente.");
        if (isGeneric(subniche.getName()) || isGeneric(subniche.getPainSummary())) {
            blockers.add("Subnicho ou dor ainda genérico demais para experimento.");
        }
        long approvedAngles = painAngleRepository.countBySubnicheIdAndStatusIn(
                subnicheId,
                Set.of(OprmGeneralAudiencePainAngleStatus.APPROVED));
        if (approvedAngles == 0) {
            blockers.add("Nenhum ângulo de dor aprovado com promessa segura.");
        }
        if (sourceEvidenceRepository.countBySubnicheId(subnicheId) == 0) {
            blockers.add("Nenhuma evidência agregada associada ao subnicho.");
        }
        if (!StringUtils.hasText(subniche.getChannelsSummary())) {
            recommendations.add("Informar canais onde a linguagem real do público aparece.");
        }
        if (!StringUtils.hasText(subniche.getDesiredOutcomeSummary())) {
            recommendations.add("Informar resultado desejado antes de criar isca ou promessa.");
        }
        return new GeneralAudienceQualityGateResponse(subnicheId, blockers.isEmpty(), blockers, recommendations);
    }

    /** Busca uma semente ou devolve erro HTTP de recurso inexistente. */
    private OprmGeneralAudienceSeed findSeed(Long seedId) {
        return seedRepository.findById(seedId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Semente de público geral não encontrada: " + seedId));
    }

    /** Busca um subnicho ou devolve erro HTTP de recurso inexistente. */
    private OprmGeneralAudienceSubniche findSubniche(Long subnicheId) {
        return subnicheRepository.findById(subnicheId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subnicho de público geral não encontrado: " + subnicheId));
    }

    /** Busca um ângulo ou devolve erro HTTP de recurso inexistente. */
    private OprmGeneralAudiencePainAngle findPainAngle(Long angleId) {
        return painAngleRepository.findById(angleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ângulo de público geral não encontrado: " + angleId));
    }

    /** Valida se o ângulo não contém saída genérica ou promessa arriscada. */
    private void validateSafeAngle(OprmGeneralAudiencePainAngle angle) {
        if (isGeneric(angle.getPain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pain genérico demais para público geral");
        }
        if (containsRiskyPromise(angle.getSafePromise()) || containsRiskyPromise(angle.getFirstAdHook())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ângulo contém promessa arriscada ou absoluta");
        }
    }

    /** Indica se o texto é curto ou amplo demais para orientar uma decisão comercial. */
    private boolean isGeneric(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.length() < 12 || GENERIC_TERMS.contains(normalized);
    }

    /** Indica se o texto contém promessa sensível, absoluta ou garantida. */
    private boolean containsRiskyPromise(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return RISKY_PROMISE_TERMS.stream().anyMatch(normalized::contains);
    }

    /** Adiciona bloqueio quando um campo textual obrigatório para qualidade está ausente. */
    private void requireText(List<String> blockers, String value, String message) {
        if (!StringUtils.hasText(value)) {
            blockers.add(message);
        }
    }

    /** Normaliza texto obrigatório e rejeita valor vazio. */
    private String requiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " não pode ficar vazio");
        }
        return value.trim();
    }

    /** Normaliza texto opcional para persistir nulo quando não há conteúdo útil. */
    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /** Converte entidade de ângulo para contrato HTTP. */
    private GeneralAudiencePainAngleResponse toPainAngleResponse(OprmGeneralAudiencePainAngle angle) {
        return new GeneralAudiencePainAngleResponse(
                angle.getId(),
                angle.getSubniche().getId(),
                angle.getPain(),
                angle.getDesiredResult(),
                angle.getMechanismDirection(),
                angle.getProofOrLeadMagnet(),
                angle.getSafePromise(),
                angle.getFirstAdHook(),
                angle.getLandingConfirmationQuestion(),
                angle.getComplianceNotes(),
                angle.getStatus(),
                angle.getCreatedAt(),
                angle.getUpdatedAt());
    }

    /** Converte entidade de evidência para contrato HTTP. */
    private GeneralAudienceSourceEvidenceResponse toSourceEvidenceResponse(OprmGeneralAudienceSourceEvidence evidence) {
        return new GeneralAudienceSourceEvidenceResponse(
                evidence.getId(),
                evidence.getSeed().getId(),
                evidence.getSubniche() == null ? null : evidence.getSubniche().getId(),
                evidence.getSourceUrl(),
                evidence.getSourceDomain(),
                evidence.getSourceType(),
                evidence.getEvidenceSummary(),
                evidence.getCapturedAt());
    }
}
