package com.marketinghub.oprm.generalaudience.service;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngle;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeed;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSourceEvidence;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubniche;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import com.marketinghub.oprm.generalaudience.service.createHypothesis.CreateGeneralAudienceHypothesisRequest;
import com.marketinghub.oprm.generalaudience.service.createLeadExperiment.CreateGeneralAudienceLeadExperimentRequest;
import com.marketinghub.oprm.generalaudience.service.createLeadExperiment.GeneralAudienceLeadExperimentResponse;
import com.marketinghub.oprm.generalaudience.service.createHypothesis.GeneralAudienceHypothesisResponse;
import com.marketinghub.oprm.generalaudience.service.prepareTargeting.GeneralAudienceTargetingElementResponse;
import com.marketinghub.oprm.generalaudience.service.prepareTargeting.GeneralAudienceTargetingPreparationRequest;
import com.marketinghub.oprm.generalaudience.service.prepareTargeting.GeneralAudienceTargetingPreparationResponse;
import com.marketinghub.oprm.generalaudience.service.createPainAngle.CreateGeneralAudiencePainAngleRequest;
import com.marketinghub.oprm.generalaudience.service.createSourceEvidence.CreateGeneralAudienceSourceEvidenceRequest;
import com.marketinghub.oprm.generalaudience.service.listPainAngles.GeneralAudiencePainAngleResponse;
import com.marketinghub.oprm.generalaudience.service.listSourceEvidences.GeneralAudienceSourceEvidenceResponse;
import com.marketinghub.oprm.generalaudience.service.qualityGate.GeneralAudienceQualityGateResponse;
import com.marketinghub.oprm.generalaudience.service.updatePainAngle.UpdateGeneralAudiencePainAngleRequest;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceHypothesisMaterializationRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceLeadExperimentMaterializationRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudiencePainAngleRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSeedRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSourceEvidenceRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSubnicheRepository;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementSource;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import com.marketinghub.targeting.service.TargetingElementService;
import java.math.BigDecimal;
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
    private static final BigDecimal MAX_GENERAL_AUDIENCE_DAILY_BUDGET = new BigDecimal("100.00");
    private final OprmGeneralAudienceHypothesisMaterializationRepository hypothesisMaterializationRepository;
    private final OprmGeneralAudienceLeadExperimentMaterializationRepository leadExperimentMaterializationRepository;
    private final TargetingElementService targetingElementService;

    /** Inicializa o serviço com repositórios centralizados do módulo OPRM. */
    public OprmGeneralAudienceDiscoveryService(
            OprmGeneralAudienceSeedRepository seedRepository,
            OprmGeneralAudienceSubnicheRepository subnicheRepository,
            OprmGeneralAudiencePainAngleRepository painAngleRepository,
            OprmGeneralAudienceSourceEvidenceRepository sourceEvidenceRepository,
            OprmGeneralAudienceHypothesisMaterializationRepository hypothesisMaterializationRepository,
            OprmGeneralAudienceLeadExperimentMaterializationRepository leadExperimentMaterializationRepository,
            TargetingElementService targetingElementService) {
        this.seedRepository = seedRepository;
        this.subnicheRepository = subnicheRepository;
        this.painAngleRepository = painAngleRepository;
        this.sourceEvidenceRepository = sourceEvidenceRepository;
        this.hypothesisMaterializationRepository = hypothesisMaterializationRepository;
        this.leadExperimentMaterializationRepository = leadExperimentMaterializationRepository;
        this.targetingElementService = targetingElementService;
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

    /** Cria uma hipótese específica para a dor principal de um ângulo aprovado de público geral. */
    @Transactional
    public GeneralAudienceHypothesisResponse createHypothesis(
            Long angleId,
            CreateGeneralAudienceHypothesisRequest request) {
        CreateGeneralAudienceHypothesisRequest safeRequest = request == null
                ? new CreateGeneralAudienceHypothesisRequest(null, null, null)
                : request;
        OprmGeneralAudiencePainAngle angle = findPainAngle(angleId);
        validateAngleCanCreateHypothesis(angle);
        OprmGeneralAudienceSubniche subniche = angle.getSubniche();
        String leadMagnetOrPromise = resolveLeadMagnetOrPromise(angle);
        String statement = buildHypothesisStatement(subniche, angle, leadMagnetOrPromise);
        String title = resolveDefaultText(safeRequest.title(), "Hipótese Público Geral - " + subniche.getName());
        String successRule = resolveDefaultText(
                safeRequest.successRule(),
                "Validar se o público qualificado responde melhor à isca/promessa segura do que a uma mensagem genérica.");
        var hypothesis = hypothesisMaterializationRepository.createHypothesis(
                subniche.getMarketNicheId(),
                title,
                statement,
                angle.getPain(),
                subniche.getName(),
                angle.getMechanismDirection(),
                leadMagnetOrPromise,
                successRule,
                safeRequest.kpiTargetCpl(),
                buildHypothesisAuditPrompt(subniche, angle));
        return new GeneralAudienceHypothesisResponse(
                angle.getId(),
                subniche.getId(),
                subniche.getMarketNicheId(),
                hypothesis.id(),
                hypothesis.title(),
                hypothesis.status(),
                statement,
                hypothesis.createdAt());
    }

    /** Cria um experimento curto de lead/isca para validar a qualidade do público geral. */
    @Transactional
    public GeneralAudienceLeadExperimentResponse createLeadExperiment(
            Long angleId,
            CreateGeneralAudienceLeadExperimentRequest request) {
        OprmGeneralAudiencePainAngle angle = findPainAngle(angleId);
        validateAngleCanCreateLeadExperiment(angle, request);
        OprmGeneralAudienceSubniche subniche = angle.getSubniche();
        String leadMagnet = requiredText(angle.getProofOrLeadMagnet(), "proofOrLeadMagnet");
        String safePromise = requiredText(angle.getSafePromise(), "safePromise");
        String qualificationQuestion = resolveQualificationQuestion(subniche, angle);
        String primaryMetric = resolveDefaultText(request.primaryMetric(), "CPL de lead qualificado");
        String experimentName = resolveDefaultText(
                request.name(),
                "Lead Público Geral - " + subniche.getName() + " - " + shortText(angle.getPain(), 60));
        String statement = buildHypothesisStatement(subniche, angle, leadMagnet);
        var experiment = leadExperimentMaterializationRepository.createLeadExperiment(
                subniche.getMarketNicheId(),
                request.hypothesisId(),
                experimentName,
                statement,
                primaryMetric,
                request.stopLossCpl(),
                request.dailyBudget(),
                request.durationDays(),
                request.kpiTargetCpl(),
                request.sampleSize(),
                buildLeadExperimentCampaignAngle(subniche, angle, leadMagnet, safePromise, qualificationQuestion),
                buildLeadExperimentAdCopy(angle, safePromise, qualificationQuestion),
                buildLeadExperimentLandingCopy(subniche, angle, leadMagnet, safePromise, qualificationQuestion));
        return new GeneralAudienceLeadExperimentResponse(
                angle.getId(),
                subniche.getId(),
                subniche.getMarketNicheId(),
                experiment.id(),
                experiment.name(),
                experiment.status(),
                experiment.primaryMetric(),
                experiment.stopLossCpl(),
                experiment.dailyBudget(),
                experiment.startDate(),
                experiment.endDate());
    }

    /** Prepara targeting inicial conservador para público geral sem depender de tabelas CNAE. */
    @Transactional
    public GeneralAudienceTargetingPreparationResponse prepareInitialTargeting(
            Long angleId,
            GeneralAudienceTargetingPreparationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload de targeting inicial é obrigatório");
        }
        OprmGeneralAudiencePainAngle angle = findPainAngle(angleId);
        validateAngleCanCreateHypothesis(angle);
        OprmGeneralAudienceSubniche subniche = angle.getSubniche();
        String qualificationQuestion = requiredText(resolveQualificationQuestion(subniche, angle), "qualificationQuestion");
        List<GeneralAudienceTargetingElementResponse> created = new ArrayList<>();
        created.addAll(createTargetingElements(subniche, angle, request, TargetingElementType.JOB_TITLE, request.jobTitles(), true));
        created.addAll(createTargetingElements(subniche, angle, request, TargetingElementType.INTEREST, request.interests(), false));
        created.addAll(createTargetingElements(subniche, angle, request, TargetingElementType.BEHAVIOR, request.behaviors(), false));
        long approvedJobTitles = created.stream()
                .filter(element -> element.type() == TargetingElementType.JOB_TITLE)
                .filter(GeneralAudienceTargetingElementResponse::publishableForCurrentPublisher)
                .count();
        List<String> blockers = conservativeTargetingBlockers(created, request, approvedJobTitles);
        List<String> recommendations = conservativeTargetingRecommendations(request, qualificationQuestion);
        return new GeneralAudienceTargetingPreparationResponse(
                angle.getId(),
                subniche.getId(),
                subniche.getMarketNicheId(),
                request.hypothesisId(),
                approvedJobTitles > 0 && blockers.isEmpty(),
                blockers,
                recommendations,
                created);
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

    /** Cria elementos de targeting com aprovação conservadora baseada em identificador oficial da Meta. */
    private List<GeneralAudienceTargetingElementResponse> createTargetingElements(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            GeneralAudienceTargetingPreparationRequest request,
            TargetingElementType type,
            List<String> terms,
            boolean requiredForPublication) {
        if (terms == null || terms.isEmpty()) {
            return List.of();
        }
        List<GeneralAudienceTargetingElementResponse> created = new ArrayList<>();
        for (int index = 0; index < terms.size(); index++) {
            String term = normalizeOptionalText(terms.get(index));
            if (!StringUtils.hasText(term) || created.stream().anyMatch(element -> element.term().equals(term))) {
                continue;
            }
            created.add(createTargetingElement(
                    subniche,
                    angle,
                    request,
                    type,
                    term,
                    resolveMetaId(request, type, index),
                    requiredForPublication));
        }
        return created;
    }

    /** Persiste um elemento individual mantendo público geral como origem operacional auditável. */
    private GeneralAudienceTargetingElementResponse createTargetingElement(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            GeneralAudienceTargetingPreparationRequest request,
            TargetingElementType type,
            String term,
            String metaId,
            boolean requiredForPublication) {
        CreateTargetingElementRequest elementRequest = new CreateTargetingElementRequest();
        elementRequest.setMarketNicheId(subniche.getMarketNicheId());
        elementRequest.setHypothesisId(request.hypothesisId());
        elementRequest.setType(type);
        elementRequest.setTerm(term);
        elementRequest.setDescription(buildTargetingDescription(subniche, angle, type, requiredForPublication));
        elementRequest.setPrompt(buildTargetingAuditPrompt(subniche, angle, request));
        elementRequest.setSource(TargetingElementSource.MANUAL);
        elementRequest.setMetaId(metaId);
        elementRequest.setStatus(resolveInitialTargetingStatus(request, type, metaId));
        elementRequest.setNotes(buildTargetingNotes(request, requiredForPublication));
        elementRequest.setLastReviewedBy(normalizeOptionalText(request.reviewedBy()));
        TargetingElement element = targetingElementService.create(elementRequest);
        return toTargetingElementResponse(element);
    }

    /** Define status inicial sem liberar publicação quando falta validação manual/Meta. */
    private TargetingElementStatus resolveInitialTargetingStatus(
            GeneralAudienceTargetingPreparationRequest request,
            TargetingElementType type,
            String metaId) {
        if (type == TargetingElementType.JOB_TITLE
                && Boolean.TRUE.equals(request.approvedJobTitlesAlreadyResolved())
                && StringUtils.hasText(metaId)) {
            return TargetingElementStatus.APPROVED;
        }
        return TargetingElementStatus.NEEDS_REVIEW;
    }

    /** Resolve o identificador oficial da Meta informado para o termo de cargo. */
    private String resolveMetaId(
            GeneralAudienceTargetingPreparationRequest request,
            TargetingElementType type,
            int index) {
        if (type != TargetingElementType.JOB_TITLE
                || request.jobTitleMetaIds() == null
                || request.jobTitleMetaIds().size() <= index) {
            return null;
        }
        return normalizeOptionalText(request.jobTitleMetaIds().get(index));
    }

    /** Monta descrição funcional do targeting inicial sem criar exceção escondida ao fluxo CNAE. */
    private String buildTargetingDescription(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            TargetingElementType type,
            boolean requiredForPublication) {
        return String.join("\n",
                "Origem: Público Geral OPRM > " + subniche.getSeed().getName() + " > " + subniche.getName(),
                "Tipo: " + type.name(),
                "Dor validada: " + angle.getPain(),
                requiredForPublication
                        ? "Função: cargo/termo conservador exigido pelo publicador atual."
                        : "Função: enriquecimento por interesse/comportamento; não substitui cargo aprovado.");
    }

    /** Monta trilha de auditoria do targeting para evitar publicação ampla pura. */
    private String buildTargetingAuditPrompt(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            GeneralAudienceTargetingPreparationRequest request) {
        return String.join("\n",
                "Origem operacional: OPRM_PUBLICO_GERAL_TARGETING_INICIAL",
                "seedId=" + subniche.getSeed().getId(),
                "subnicheId=" + subniche.getId(),
                "painAngleId=" + angle.getId(),
                "marketNicheId=" + subniche.getMarketNicheId(),
                "hypothesisId=" + request.hypothesisId(),
                "Targeting inicial não depende de CNAE e não libera público amplo puro.");
    }

    /** Monta observações de uso do criativo, landing e demografia como suporte à triagem. */
    private String buildTargetingNotes(
            GeneralAudienceTargetingPreparationRequest request,
            boolean requiredForPublication) {
        return String.join("\n",
                requiredForPublication
                        ? "Exigência conservadora: revisar e resolver cargo/termo antes de publicação."
                        : "Enriquecimento: usar junto com cargo aprovado, criativo de triagem e landing de confirmação.",
                "Frase de triagem no criativo: " + optionalText(request.creativeScreeningPhrase()),
                "Orientação demográfica: " + optionalText(request.demographicGuidance()),
                "Confirmação na landing: " + optionalText(request.landingConfirmationInstruction()));
    }

    /** Converte elemento salvo para resposta operacional de targeting inicial. */
    private GeneralAudienceTargetingElementResponse toTargetingElementResponse(TargetingElement element) {
        return new GeneralAudienceTargetingElementResponse(
                element.getId(),
                element.getType(),
                element.getTerm(),
                element.getStatus(),
                element.getMetaId(),
                element.getStatus() == TargetingElementStatus.APPROVED && StringUtils.hasText(element.getMetaId()));
    }

    /** Gera bloqueios explícitos para impedir ad set amplo puro no publicador atual. */
    private List<String> conservativeTargetingBlockers(
            List<GeneralAudienceTargetingElementResponse> created,
            GeneralAudienceTargetingPreparationRequest request,
            long approvedJobTitles) {
        List<String> blockers = new ArrayList<>();
        boolean hasJobTitle = created.stream().anyMatch(element -> element.type() == TargetingElementType.JOB_TITLE);
        if (!hasJobTitle) {
            blockers.add("Informe ao menos um cargo/termo de trabalho para revisão antes de publicação.");
        }
        if (approvedJobTitles == 0) {
            blockers.add("Nenhum JOB_TITLE aprovado e resolvido na Meta; o publicador atual não deve criar ad set amplo puro.");
        }
        if (!StringUtils.hasText(request.creativeScreeningPhrase())) {
            blockers.add("Frase de triagem do criativo é obrigatória para afastar público errado.");
        }
        return blockers;
    }

    /** Gera recomendações para enriquecer público geral sem trocar cargo aprovado por interesse amplo. */
    private List<String> conservativeTargetingRecommendations(
            GeneralAudienceTargetingPreparationRequest request,
            String qualificationQuestion) {
        List<String> recommendations = new ArrayList<>();
        if (request.interests() == null || request.interests().isEmpty()) {
            recommendations.add("Adicionar interesses específicos como enriquecimento, não como substituto do JOB_TITLE.");
        }
        if (request.behaviors() == null || request.behaviors().isEmpty()) {
            recommendations.add("Adicionar comportamentos reais do subnicho quando existirem.");
        }
        recommendations.add("Manter pergunta qualificadora na landing: " + qualificationQuestion);
        return recommendations;
    }

    /** Valida campos obrigatórios do pacote experimental de lead/isca. */
    private void validateAngleCanCreateLeadExperiment(
            OprmGeneralAudiencePainAngle angle,
            CreateGeneralAudienceLeadExperimentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload do experimento de lead é obrigatório");
        }
        validateAngleCanCreateHypothesis(angle);
        requiredText(angle.getProofOrLeadMagnet(), "proofOrLeadMagnet");
        requiredText(angle.getSafePromise(), "safePromise");
        requiredText(resolveQualificationQuestion(angle.getSubniche(), angle), "qualificationQuestion");
        requiredText(request.primaryMetric(), "primaryMetric");
        if (request.hypothesisId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hypothesisId é obrigatório");
        }
        if (request.stopLossCpl() == null || request.stopLossCpl().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stopLossCpl deve ser maior que zero");
        }
        if (request.dailyBudget() == null || request.dailyBudget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dailyBudget deve ser maior que zero");
        }
        if (request.dailyBudget().compareTo(MAX_GENERAL_AUDIENCE_DAILY_BUDGET) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dailyBudget deve manter orçamento pequeno até 100.00");
        }
        if (request.durationDays() == null || request.durationDays() < 1 || request.durationDays() > 14) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "durationDays deve representar duração curta entre 1 e 14 dias");
        }
    }

    /** Resolve a pergunta qualificadora priorizando a pergunta específica da landing. */
    private String resolveQualificationQuestion(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle) {
        if (StringUtils.hasText(angle.getLandingConfirmationQuestion())) {
            return angle.getLandingConfirmationQuestion().trim();
        }
        return subniche.getQualificationQuestion();
    }

    /** Monta o ângulo de campanha do experimento de lead sem publicar campanha. */
    private String buildLeadExperimentCampaignAngle(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            String leadMagnet,
            String safePromise,
            String qualificationQuestion) {
        return String.join("\n\n",
                "Origem: Público Geral OPRM > " + subniche.getSeed().getName() + " > " + subniche.getName(),
                "Dor principal: " + angle.getPain(),
                "Isca: " + leadMagnet,
                "Promessa segura: " + safePromise,
                "Pergunta qualificadora: " + qualificationQuestion,
                "Objetivo: capturar lead qualificado ou conversa de WhatsApp, sem venda direta.");
    }

    /** Monta rascunho de copy de anúncio com frase de triagem do público. */
    private String buildLeadExperimentAdCopy(
            OprmGeneralAudiencePainAngle angle,
            String safePromise,
            String qualificationQuestion) {
        return String.join("\n",
                StringUtils.hasText(angle.getFirstAdHook()) ? angle.getFirstAdHook().trim() : safePromise,
                qualificationQuestion,
                "Se fizer sentido para você, solicite a isca gratuita antes de qualquer oferta.");
    }

    /** Monta copy operacional da landing/formulário com confirmação de público. */
    private String buildLeadExperimentLandingCopy(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            String leadMagnet,
            String safePromise,
            String qualificationQuestion) {
        return String.join("\n\n",
                "Para quem é: " + subniche.getName(),
                "Dor principal: " + angle.getPain(),
                "O que a pessoa recebe: " + leadMagnet,
                "Promessa segura: " + safePromise,
                "Pergunta obrigatória: " + qualificationQuestion,
                "Próximo passo: entregar a isca e medir qualidade do lead antes de qualquer venda.");
    }

    /** Encurta texto para nomes operacionais sem perder a decisão principal. */
    private String shortText(String value, int maxLength) {
        String text = requiredText(value, "text");
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim();
    }

    /** Valida que o ângulo e o subnicho estão prontos para criar uma hipótese específica. */
    private void validateAngleCanCreateHypothesis(OprmGeneralAudiencePainAngle angle) {
        if (angle.getStatus() != OprmGeneralAudiencePainAngleStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ângulo precisa estar aprovado antes de criar hipótese");
        }
        OprmGeneralAudienceSubniche subniche = angle.getSubniche();
        if (subniche.getStatus() != OprmGeneralAudienceSubnicheStatus.CONVERTED_TO_NICHE
                || subniche.getMarketNicheId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subnicho precisa estar convertido em MarketNiche antes de criar hipótese");
        }
        requiredText(subniche.getName(), "subniche.name");
        requiredText(subniche.getPersonaSummary(), "subniche.personaSummary");
        requiredText(angle.getPain(), "pain");
        requiredText(angle.getDesiredResult(), "desiredResult");
        requiredText(angle.getMechanismDirection(), "mechanismDirection");
        requiredText(resolveLeadMagnetOrPromise(angle), "proofOrLeadMagnet ou safePromise");
    }

    /** Escolhe a isca concreta como primeira opção e a promessa segura como fallback explícito. */
    private String resolveLeadMagnetOrPromise(OprmGeneralAudiencePainAngle angle) {
        if (StringUtils.hasText(angle.getProofOrLeadMagnet())) {
            return angle.getProofOrLeadMagnet().trim();
        }
        if (StringUtils.hasText(angle.getSafePromise())) {
            return angle.getSafePromise().trim();
        }
        return null;
    }

    /** Monta a frase canônica da hipótese de público geral. */
    private String buildHypothesisStatement(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            String leadMagnetOrPromise) {
        return "Acreditamos que " + requiredText(subniche.getName(), "subniche.name")
                + " com " + requiredText(angle.getPain(), "pain")
                + " responderá melhor a " + leadMagnetOrPromise
                + " do que a uma mensagem genérica, porque "
                + requiredText(angle.getMechanismDirection(), "mechanismDirection") + ".";
    }

    /** Monta trilha de auditoria interna da hipótese sem publicar campanha ou experimento. */
    private String buildHypothesisAuditPrompt(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle) {
        return String.join("\n",
                "Origem operacional: OPRM_PUBLICO_GERAL_HYPOTHESIS",
                "seedId=" + subniche.getSeed().getId(),
                "subnicheId=" + subniche.getId(),
                "painAngleId=" + angle.getId(),
                "marketNicheId=" + subniche.getMarketNicheId(),
                "Hipótese criada sem experimento, campanha ou venda direta.");
    }

    /** Retorna texto opcional ou aviso claro para notas auditáveis. */
    private String optionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return "Não informado.";
        }
        return value.trim();
    }

    /** Aplica texto padrão quando a entrada opcional não tem conteúdo útil. */
    private String resolveDefaultText(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
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
