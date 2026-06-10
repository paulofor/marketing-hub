package com.marketinghub.oprm.generalaudience.service;

import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceAdSignalStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceAdSignalType;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceFacebookAdsData;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngle;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudiencePainAngleStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceQualityReading;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeed;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSourceEvidence;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubniche;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSubnicheStatus;
import com.marketinghub.oprm.generalaudience.service.createHypothesis.CreateGeneralAudienceHypothesisRequest;
import com.marketinghub.oprm.generalaudience.service.createHypothesis.GeneralAudienceHypothesisResponse;
import com.marketinghub.oprm.generalaudience.service.createLeadExperiment.CreateGeneralAudienceLeadExperimentRequest;
import com.marketinghub.oprm.generalaudience.service.createLeadExperiment.GeneralAudienceLeadExperimentResponse;
import com.marketinghub.oprm.generalaudience.service.createQualityReading.CreateGeneralAudienceQualityReadingRequest;
import com.marketinghub.oprm.generalaudience.service.createPainAngle.CreateGeneralAudiencePainAngleRequest;
import com.marketinghub.oprm.generalaudience.service.createSourceEvidence.CreateGeneralAudienceSourceEvidenceRequest;
import com.marketinghub.oprm.generalaudience.service.prepareTargeting.GeneralAudienceTargetingElementResponse;
import com.marketinghub.oprm.generalaudience.service.prepareTargeting.GeneralAudienceTargetingPreparationRequest;
import com.marketinghub.oprm.generalaudience.service.prepareTargeting.GeneralAudienceTargetingPreparationResponse;
import com.marketinghub.oprm.generalaudience.service.listPainAngles.GeneralAudiencePainAngleResponse;
import com.marketinghub.oprm.generalaudience.service.listQualityReadings.GeneralAudienceQualityReadingResponse;
import com.marketinghub.oprm.generalaudience.service.listSourceEvidences.GeneralAudienceSourceEvidenceResponse;
import com.marketinghub.oprm.generalaudience.service.qualityGate.GeneralAudienceQualityGateResponse;
import com.marketinghub.oprm.generalaudience.service.updatePainAngle.UpdateGeneralAudiencePainAngleRequest;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceHypothesisMaterializationRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceLeadExperimentMaterializationRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceFacebookAdsDataRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudiencePainAngleRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceQualityReadingRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSeedRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSourceEvidenceRepository;
import com.marketinghub.repository.jpa.oprm.generalaudience.OprmGeneralAudienceSubnicheRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
    private final OprmGeneralAudienceQualityReadingRepository qualityReadingRepository;
    private static final BigDecimal MAX_GENERAL_AUDIENCE_DAILY_BUDGET = new BigDecimal("100.00");
    private final OprmGeneralAudienceHypothesisMaterializationRepository hypothesisMaterializationRepository;
    private final OprmGeneralAudienceLeadExperimentMaterializationRepository leadExperimentMaterializationRepository;
    private final OprmGeneralAudienceFacebookAdsDataRepository facebookAdsDataRepository;

    /** Inicializa o serviço com repositórios centralizados do módulo OPRM. */
    public OprmGeneralAudienceDiscoveryService(
            OprmGeneralAudienceSeedRepository seedRepository,
            OprmGeneralAudienceSubnicheRepository subnicheRepository,
            OprmGeneralAudiencePainAngleRepository painAngleRepository,
            OprmGeneralAudienceSourceEvidenceRepository sourceEvidenceRepository,
            OprmGeneralAudienceQualityReadingRepository qualityReadingRepository,
            OprmGeneralAudienceHypothesisMaterializationRepository hypothesisMaterializationRepository,
            OprmGeneralAudienceLeadExperimentMaterializationRepository leadExperimentMaterializationRepository,
            OprmGeneralAudienceFacebookAdsDataRepository facebookAdsDataRepository) {
        this.seedRepository = seedRepository;
        this.subnicheRepository = subnicheRepository;
        this.painAngleRepository = painAngleRepository;
        this.sourceEvidenceRepository = sourceEvidenceRepository;
        this.qualityReadingRepository = qualityReadingRepository;
        this.hypothesisMaterializationRepository = hypothesisMaterializationRepository;
        this.leadExperimentMaterializationRepository = leadExperimentMaterializationRepository;
        this.facebookAdsDataRepository = facebookAdsDataRepository;
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

    /** Lista leituras de qualidade real de leads de um subnicho. */
    @Transactional(readOnly = true)
    public List<GeneralAudienceQualityReadingResponse> listQualityReadings(Long subnicheId) {
        findSubniche(subnicheId);
        return qualityReadingRepository.findAllBySubnicheIdOrderByCapturedAtDesc(subnicheId).stream()
                .map(this::toQualityReadingResponse)
                .toList();
    }

    /** Registra sinais bons e ruins para medir qualidade real além de CTR e CPL. */
    @Transactional
    public GeneralAudienceQualityReadingResponse createQualityReading(
            Long subnicheId,
            CreateGeneralAudienceQualityReadingRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload de leitura de qualidade é obrigatório");
        }
        OprmGeneralAudienceSubniche subniche = findSubniche(subnicheId);
        OprmGeneralAudiencePainAngle painAngle = null;
        if (request.painAngleId() != null) {
            painAngle = findPainAngle(request.painAngleId());
            if (!subnicheId.equals(painAngle.getSubniche().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "painAngleId não pertence ao subnicho informado");
            }
        }
        OprmGeneralAudienceQualityReading reading = new OprmGeneralAudienceQualityReading();
        reading.setSubniche(subniche);
        reading.setPainAngle(painAngle);
        reading.setExperimentId(request.experimentId());
        reading.setTotalLeads(nonNegativeOrZero(request.totalLeads(), "totalLeads"));
        reading.setCorrectProfessionLeads(nonNegativeOrZero(request.correctProfessionLeads(), "correctProfessionLeads"));
        reading.setRealPainResponses(nonNegativeOrZero(request.realPainResponses(), "realPainResponses"));
        reading.setMaterialRequests(nonNegativeOrZero(request.materialRequests(), "materialRequests"));
        reading.setWhatsappReplies(nonNegativeOrZero(request.whatsappReplies(), "whatsappReplies"));
        reading.setPriceOrNextStepQuestions(
                nonNegativeOrZero(request.priceOrNextStepQuestions(), "priceOrNextStepQuestions"));
        reading.setOutOfProfileLeads(nonNegativeOrZero(request.outOfProfileLeads(), "outOfProfileLeads"));
        reading.setCuriousWithoutProfession(
                nonNegativeOrZero(request.curiousWithoutProfession(), "curiousWithoutProfession"));
        reading.setLowCompletionEvents(nonNegativeOrZero(request.lowCompletionEvents(), "lowCompletionEvents"));
        reading.setConfusingPromiseReports(
                nonNegativeOrZero(request.confusingPromiseReports(), "confusingPromiseReports"));
        reading.setLeadMagnetNoResponse(nonNegativeOrZero(request.leadMagnetNoResponse(), "leadMagnetNoResponse"));
        reading.setNotes(normalizeOptionalText(request.notes()));
        reading.setCapturedAt(request.capturedAt() == null ? Instant.now() : request.capturedAt());
        List<String> blockers = qualityReadingBlockers(reading);
        List<String> recommendations = qualityReadingRecommendations(reading);
        reading.setQualityScore(calculateQualityScore(reading));
        reading.setApproved(blockers.isEmpty() && reading.getQualityScore().compareTo(new BigDecimal("60.00")) >= 0);
        reading.setBlockers(joinLines(blockers));
        reading.setRecommendations(joinLines(recommendations));
        return toQualityReadingResponse(qualityReadingRepository.save(reading));
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
        qualityReadingRepository.findTopBySubnicheIdOrderByCapturedAtDesc(subnicheId).ifPresentOrElse(reading -> {
            if (!reading.isApproved()) {
                blockers.add("Leitura de qualidade mais recente não aprovou o público.");
            }
            if (reading.getWhatsappReplies() == 0) {
                recommendations.add("Medir resposta no WhatsApp antes de escalar o público.");
            }
            if (reading.getPriceOrNextStepQuestions() == 0) {
                recommendations.add(
                        "Validar pergunta de preço ou próximo passo antes de tratar como público comprador.");
            }
        }, () -> recommendations.add("Registrar leitura de qualidade real após a primeira captação de leads."));
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

    /** Registra dados de público para o Facebook Ads buscar depois pelo backend, sem acessar o módulo de targeting. */
    @Transactional
    public GeneralAudienceTargetingPreparationResponse prepareInitialTargeting(
            Long angleId,
            GeneralAudienceTargetingPreparationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload de dados para Facebook Ads é obrigatório");
        }
        OprmGeneralAudiencePainAngle angle = findPainAngle(angleId);
        validateAngleCanCreateHypothesis(angle);
        OprmGeneralAudienceSubniche subniche = angle.getSubniche();
        String qualificationQuestion = requiredText(resolveQualificationQuestion(subniche, angle), "qualificationQuestion");
        facebookAdsDataRepository.deleteByPainAngle_Id(angle.getId());
        List<OprmGeneralAudienceFacebookAdsData> saved = new ArrayList<>();
        saved.addAll(saveFacebookAdsData(subniche, angle, request, OprmGeneralAudienceAdSignalType.JOB_TITLE, request.jobTitles(), true));
        saved.addAll(saveFacebookAdsData(subniche, angle, request, OprmGeneralAudienceAdSignalType.INTEREST, request.interests(), false));
        saved.addAll(saveFacebookAdsData(subniche, angle, request, OprmGeneralAudienceAdSignalType.BEHAVIOR, request.behaviors(), false));
        long readyJobTitles = saved.stream()
                .filter(element -> element.getSignalType() == OprmGeneralAudienceAdSignalType.JOB_TITLE)
                .filter(OprmGeneralAudienceFacebookAdsData::isReadyForFacebookAds)
                .count();
        List<GeneralAudienceTargetingElementResponse> elements = saved.stream()
                .map(this::toFacebookAdsDataResponse)
                .toList();
        List<String> blockers = conservativeTargetingBlockers(saved, request, readyJobTitles);
        List<String> recommendations = conservativeTargetingRecommendations(request, qualificationQuestion);
        return new GeneralAudienceTargetingPreparationResponse(
                angle.getId(),
                subniche.getId(),
                subniche.getMarketNicheId(),
                request.hypothesisId(),
                readyJobTitles > 0 && blockers.isEmpty(),
                blockers,
                recommendations,
                elements);
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

    /** Salva sinais de público no domínio OPRM para consumo posterior pelo Facebook Ads via backend. */
    private List<OprmGeneralAudienceFacebookAdsData> saveFacebookAdsData(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            GeneralAudienceTargetingPreparationRequest request,
            OprmGeneralAudienceAdSignalType type,
            List<String> terms,
            boolean requiredForPublication) {
        if (terms == null || terms.isEmpty()) {
            return List.of();
        }
        List<OprmGeneralAudienceFacebookAdsData> items = new ArrayList<>();
        for (int index = 0; index < terms.size(); index++) {
            String term = normalizeOptionalText(terms.get(index));
            if (!StringUtils.hasText(term) || items.stream().anyMatch(element -> element.getTerm().equals(term))) {
                continue;
            }
            items.add(buildFacebookAdsData(
                    subniche,
                    angle,
                    request,
                    type,
                    term,
                    resolveMetaId(request, type, index),
                    requiredForPublication));
        }
        return facebookAdsDataRepository.saveAll(items);
    }

    /** Monta um dado de público sem chamar serviços de targeting ou publicação de anúncios. */
    private OprmGeneralAudienceFacebookAdsData buildFacebookAdsData(
            OprmGeneralAudienceSubniche subniche,
            OprmGeneralAudiencePainAngle angle,
            GeneralAudienceTargetingPreparationRequest request,
            OprmGeneralAudienceAdSignalType type,
            String term,
            String metaId,
            boolean requiredForPublication) {
        OprmGeneralAudienceFacebookAdsData data = new OprmGeneralAudienceFacebookAdsData();
        data.setPainAngle(angle);
        data.setSubniche(subniche);
        data.setMarketNicheId(subniche.getMarketNicheId());
        data.setHypothesisId(request.hypothesisId() == null ? null : request.hypothesisId().toString());
        data.setSignalType(type);
        data.setTerm(term);
        data.setMetaId(metaId);
        data.setRequiredForPublication(requiredForPublication);
        data.setReadyForFacebookAds(isReadyForFacebookAds(request, type, metaId));
        data.setStatus(resolveFacebookAdsDataStatus(data));
        data.setCreativeScreeningPhrase(normalizeOptionalText(request.creativeScreeningPhrase()));
        data.setDemographicGuidance(normalizeOptionalText(request.demographicGuidance()));
        data.setLandingConfirmationInstruction(normalizeOptionalText(request.landingConfirmationInstruction()));
        data.setReviewedBy(normalizeOptionalText(request.reviewedBy()));
        data.setNotes(buildFacebookAdsDataNotes(request, requiredForPublication));
        return data;
    }

    /** Define se o dado está pronto para coleta do Facebook Ads sem materializar targeting no OPRM. */
    private boolean isReadyForFacebookAds(
            GeneralAudienceTargetingPreparationRequest request,
            OprmGeneralAudienceAdSignalType type,
            String metaId) {
        return type == OprmGeneralAudienceAdSignalType.JOB_TITLE
                && Boolean.TRUE.equals(request.approvedJobTitlesAlreadyResolved())
                && StringUtils.hasText(metaId);
    }

    /** Define o status canônico do dado armazenado para coleta posterior pelo Facebook Ads. */
    private OprmGeneralAudienceAdSignalStatus resolveFacebookAdsDataStatus(OprmGeneralAudienceFacebookAdsData data) {
        if (data.isReadyForFacebookAds()) {
            return OprmGeneralAudienceAdSignalStatus.READY_FOR_FACEBOOK_ADS;
        }
        return OprmGeneralAudienceAdSignalStatus.RECEIVED;
    }

    /** Resolve o identificador oficial da Meta informado para o termo de cargo. */
    private String resolveMetaId(
            GeneralAudienceTargetingPreparationRequest request,
            OprmGeneralAudienceAdSignalType type,
            int index) {
        if (type != OprmGeneralAudienceAdSignalType.JOB_TITLE
                || request.jobTitleMetaIds() == null
                || request.jobTitleMetaIds().size() <= index) {
            return null;
        }
        return normalizeOptionalText(request.jobTitleMetaIds().get(index));
    }

    /** Monta observações de coleta pelo Facebook Ads sem criar contrato de targeting dentro do OPRM. */
    private String buildFacebookAdsDataNotes(
            GeneralAudienceTargetingPreparationRequest request,
            boolean requiredForPublication) {
        return String.join("\n",
                requiredForPublication
                        ? "Dado obrigatório para o Facebook Ads validar cargo antes de publicar."
                        : "Dado complementar para o Facebook Ads avaliar junto com cargo validado.",
                "Frase de triagem no criativo: " + optionalText(request.creativeScreeningPhrase()),
                "Orientação demográfica: " + optionalText(request.demographicGuidance()),
                "Confirmação na landing: " + optionalText(request.landingConfirmationInstruction()));
    }

    /** Converte dado salvo no OPRM para resposta operacional ao backend/UI. */
    private GeneralAudienceTargetingElementResponse toFacebookAdsDataResponse(OprmGeneralAudienceFacebookAdsData data) {
        return new GeneralAudienceTargetingElementResponse(
                data.getId(),
                data.getSignalType(),
                data.getTerm(),
                data.getStatus(),
                data.getMetaId(),
                data.isReadyForFacebookAds());
    }

    /** Gera bloqueios explícitos para impedir que o Facebook Ads colete público amplo puro. */
    private List<String> conservativeTargetingBlockers(
            List<OprmGeneralAudienceFacebookAdsData> saved,
            GeneralAudienceTargetingPreparationRequest request,
            long readyJobTitles) {
        List<String> blockers = new ArrayList<>();
        boolean hasJobTitle = saved.stream()
                .anyMatch(element -> element.getSignalType() == OprmGeneralAudienceAdSignalType.JOB_TITLE);
        if (!hasJobTitle) {
            blockers.add("Informe ao menos um cargo/termo de trabalho para revisão antes de publicação.");
        }
        if (readyJobTitles == 0) {
            blockers.add("Nenhum JOB_TITLE pronto para Facebook Ads; o publicador atual não deve criar ad set amplo puro.");
        }
        if (!StringUtils.hasText(request.creativeScreeningPhrase())) {
            blockers.add("Frase de triagem do criativo é obrigatória para afastar público errado.");
        }
        return blockers;
    }

    /** Gera recomendações para enriquecer público geral sem trocar cargo validado por interesse amplo. */
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

    /** Calcula bloqueios objetivos da leitura de qualidade de público. */
    private List<String> qualityReadingBlockers(OprmGeneralAudienceQualityReading reading) {
        List<String> blockers = new ArrayList<>();
        if (reading.getTotalLeads() == 0) {
            blockers.add("Sem leads observados para medir qualidade do público.");
        }
        if (reading.getTotalLeads() > 0 && reading.getCorrectProfessionLeads() * 2 < reading.getTotalLeads()) {
            blockers.add("Menos da metade dos leads informou profissão correta.");
        }
        if (reading.getRealPainResponses() == 0) {
            blockers.add("Nenhuma resposta trouxe dor real do público.");
        }
        if (badSignalCount(reading) > goodSignalCount(reading)) {
            blockers.add("Sinais ruins superam sinais bons na leitura do público.");
        }
        if (reading.getOutOfProfileLeads() > reading.getCorrectProfessionLeads()) {
            blockers.add("Há mais leads fora do perfil do que leads com profissão correta.");
        }
        if (reading.getConfusingPromiseReports() > 0) {
            blockers.add("Existem relatos de promessa confusa que precisam ajustar criativo ou landing.");
        }
        return blockers;
    }

    /** Calcula recomendações para melhorar a qualidade antes de escalar o público. */
    private List<String> qualityReadingRecommendations(OprmGeneralAudienceQualityReading reading) {
        List<String> recommendations = new ArrayList<>();
        if (reading.getMaterialRequests() == 0) {
            recommendations.add("Revisar isca: ninguém pediu claramente o material.");
        }
        if (reading.getWhatsappReplies() == 0) {
            recommendations.add("Acompanhar ou reforçar convite de WhatsApp antes de escalar.");
        }
        if (reading.getPriceOrNextStepQuestions() == 0) {
            recommendations.add("Validar próximo passo comercial, pois ainda não houve pergunta de preço ou avanço.");
        }
        if (reading.getLowCompletionEvents() > 0) {
            recommendations.add("Simplificar formulário ou pergunta qualificadora para reduzir baixo preenchimento.");
        }
        if (reading.getLeadMagnetNoResponse() > 0) {
            recommendations.add("Revisar entrega e follow-up: há lead que baixa a isca, mas não responde.");
        }
        return recommendations;
    }

    /** Calcula score percentual ponderando sinais bons contra sinais ruins. */
    private BigDecimal calculateQualityScore(OprmGeneralAudienceQualityReading reading) {
        int goodSignals = goodSignalCount(reading);
        int badSignals = badSignalCount(reading);
        int totalSignals = goodSignals + badSignals;
        if (totalSignals == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(goodSignals)
                .multiply(new BigDecimal("100.00"))
                .divide(BigDecimal.valueOf(totalSignals), 2, RoundingMode.HALF_UP);
    }

    /** Soma sinais bons definidos na etapa dez do plano de públicos gerais. */
    private int goodSignalCount(OprmGeneralAudienceQualityReading reading) {
        return reading.getCorrectProfessionLeads()
                + reading.getRealPainResponses()
                + reading.getMaterialRequests()
                + reading.getWhatsappReplies()
                + reading.getPriceOrNextStepQuestions();
    }

    /** Soma sinais ruins definidos na etapa dez do plano de públicos gerais. */
    private int badSignalCount(OprmGeneralAudienceQualityReading reading) {
        return reading.getOutOfProfileLeads()
                + reading.getCuriousWithoutProfession()
                + reading.getLowCompletionEvents()
                + reading.getConfusingPromiseReports()
                + reading.getLeadMagnetNoResponse();
    }

    /** Normaliza número opcional para zero e rejeita valores negativos. */
    private int nonNegativeOrZero(Integer value, String fieldName) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " não pode ser negativo");
        }
        return value;
    }

    /** Junta mensagens em linhas para persistir sem serializar JSON dentro de texto. */
    private String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        return String.join("\n", lines);
    }

    /** Separa mensagens persistidas em linhas simples. */
    private List<String> splitLines(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
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

    /** Converte entidade de leitura de qualidade para contrato HTTP. */
    private GeneralAudienceQualityReadingResponse toQualityReadingResponse(OprmGeneralAudienceQualityReading reading) {
        return new GeneralAudienceQualityReadingResponse(
                reading.getId(),
                reading.getSubniche().getId(),
                reading.getPainAngle() == null ? null : reading.getPainAngle().getId(),
                reading.getExperimentId(),
                reading.getTotalLeads(),
                reading.getCorrectProfessionLeads(),
                reading.getRealPainResponses(),
                reading.getMaterialRequests(),
                reading.getWhatsappReplies(),
                reading.getPriceOrNextStepQuestions(),
                reading.getOutOfProfileLeads(),
                reading.getCuriousWithoutProfession(),
                reading.getLowCompletionEvents(),
                reading.getConfusingPromiseReports(),
                reading.getLeadMagnetNoResponse(),
                reading.getQualityScore(),
                reading.isApproved(),
                splitLines(reading.getBlockers()),
                splitLines(reading.getRecommendations()),
                reading.getNotes(),
                reading.getCapturedAt(),
                reading.getCreatedAt(),
                reading.getUpdatedAt());
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
