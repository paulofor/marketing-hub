package com.marketinghub.hypothesis.service.finalizeHypothesis;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.HypothesisStatus;
import com.marketinghub.hypothesis.dto.HypothesisFrameworkDto;
import com.marketinghub.hypothesis.framework.HypothesisFrameworkMapperSupport;
import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.hypothesis.HypothesisPainStageExecutionRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Responsabilidade: executar a etapa de fechamento que transforma o framework concluído em hipótese de backlog. */
@Service
public class HypothesisPipelineFinalizationService {
    private static final String STAGE_CODE = "hypothesis-pain";
    private static final String RESULT_STAGE_CODE = "hypothesis-result";
    private static final String MECHANISM_STAGE_CODE = "hypothesis-mechanism";
    private static final String PROOF_STAGE_CODE = "hypothesis-proof";
    private static final String OFFER_STAGE_CODE = "hypothesis-offer";
    private static final String STATUS_COMPLETED = "CONCLUIDO";
    private static final List<String> STAGE_SEQUENCE = List.of(
            STAGE_CODE,
            RESULT_STAGE_CODE,
            MECHANISM_STAGE_CODE,
            PROOF_STAGE_CODE,
            OFFER_STAGE_CODE);

    private final MarketNicheRepository marketNicheRepository;
    private final HypothesisPainStageExecutionRepository executionRepository;
    private final HypothesisRepository hypothesisRepository;
    private final HypothesisFrameworkMapperSupport frameworkMapperSupport;

    /** Inicializa a etapa de fechamento com os repositórios e o normalizador canônico do framework. */
    public HypothesisPipelineFinalizationService(
            MarketNicheRepository marketNicheRepository,
            HypothesisPainStageExecutionRepository executionRepository,
            HypothesisRepository hypothesisRepository,
            HypothesisFrameworkMapperSupport frameworkMapperSupport) {
        this.marketNicheRepository = marketNicheRepository;
        this.executionRepository = executionRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.frameworkMapperSupport = frameworkMapperSupport;
    }

    /** Fecha o framework concluído em uma hipótese BACKLOG pronta para gerar experimento. */
    @Transactional
    public Hypothesis finalizeHypothesis(Long marketNicheId, FinalizeHypothesisRequest request) {
        MarketNiche niche = marketNicheRepository.findById(marketNicheId)
                .orElseThrow(() -> new EntityNotFoundException("Market niche not found: " + marketNicheId));
        String title = buildAutomaticHypothesisTitle(niche);
        String pain = requireCompletedStageResponse(marketNicheId, STAGE_CODE, "Dor");
        String result = requireCompletedStageResponse(marketNicheId, RESULT_STAGE_CODE, "Resultado");
        String mechanism = requireCompletedStageResponse(marketNicheId, MECHANISM_STAGE_CODE, "Mecanismo");
        String proof = requireCompletedStageResponse(marketNicheId, PROOF_STAGE_CODE, "Prova");
        String offer = requireCompletedStageResponse(marketNicheId, OFFER_STAGE_CODE, "Oferta");
        Hypothesis hypothesis = Hypothesis.builder()
                .marketNiche(niche)
                .title(title)
                .persona(niche.getName())
                .problem(pain)
                .promise(result)
                .mechanism(mechanism)
                .uniqueMechanism(mechanism)
                .entrega(offer)
                .successRule(proof)
                .model(latestCompletedStageModel(marketNicheId, OFFER_STAGE_CODE))
                .costUsd(totalCompletedCostUsd(marketNicheId))
                .status(HypothesisStatus.BACKLOG)
                .generatedAt(Instant.now())
                .build();
        frameworkMapperSupport.storeSnapshot(
                hypothesis,
                finalizedFramework(title, pain, result, mechanism, proof, offer),
                null);
        Hypothesis savedHypothesis = hypothesisRepository.save(hypothesis);
        relateCompletedExecutionsToHypothesis(marketNicheId, savedHypothesis);
        return savedHypothesis;
    }

    /** Monta o título automático da hipótese fechada com sigla do nicho e numeração sequencial. */
    private String buildAutomaticHypothesisTitle(MarketNiche niche) {
        long nextNumber = hypothesisRepository.countByMarketNicheId(niche.getId()) + 1;
        return "%s-H%03d".formatted(nicheAcronym(niche), nextNumber);
    }

    /** Gera uma sigla estável a partir do nome do nicho para identificar hipóteses. */
    private String nicheAcronym(MarketNiche niche) {
        if (niche == null || !StringUtils.hasText(niche.getName())) {
            return "GER";
        }
        String normalized = Normalizer.normalize(niche.getName(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        StringBuilder acronym = new StringBuilder();
        for (String word : normalized.split("[^A-Z0-9]+")) {
            if (!word.isBlank()) {
                acronym.append(word.charAt(0));
            }
            if (acronym.length() == 4) {
                break;
            }
        }
        if (acronym.isEmpty()) {
            return "GER";
        }
        while (acronym.length() < 3) {
            acronym.append('X');
        }
        return acronym.toString();
    }

    /** Exige uma resposta concluída de etapa antes de permitir fechar a hipótese. */
    private String requireCompletedStageResponse(Long marketNicheId, String stageCode, String stageLabel) {
        String response = latestCompletedStageResponse(marketNicheId, stageCode);
        if (!StringUtils.hasText(response)) {
            throw new IllegalStateException(
                    "A etapa " + stageLabel + " precisa estar concluída antes de fechar a hipótese.");
        }
        return response;
    }

    /** Retorna a resposta concluída mais recente de uma etapa do framework. */
    private String latestCompletedStageResponse(Long marketNicheId, String stageCode) {
        return executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusAndHypothesisIdIsNullOrderByExecutionRequestedAtDesc(
                        marketNicheId,
                        stageCode,
                        STATUS_COMPLETED)
                .map(HypothesisPainStageExecution::getModelResponse)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    /** Retorna o modelo da etapa concluída mais recente para rastreabilidade da hipótese final. */
    private String latestCompletedStageModel(Long marketNicheId, String stageCode) {
        return executionRepository.findTopByMarketNicheIdAndStageCodeAndStatusAndHypothesisIdIsNullOrderByExecutionRequestedAtDesc(
                        marketNicheId,
                        stageCode,
                        STATUS_COMPLETED)
                .map(HypothesisPainStageExecution::getOpenAiModel)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    /** Soma o custo das execuções concluídas usadas para formar a hipótese final. */
    private BigDecimal totalCompletedCostUsd(Long marketNicheId) {
        return STAGE_SEQUENCE.stream()
                .map(stageCode -> executionRepository
                        .findTopByMarketNicheIdAndStageCodeAndStatusAndHypothesisIdIsNullOrderByExecutionRequestedAtDesc(
                                marketNicheId,
                                stageCode,
                                STATUS_COMPLETED))
                .flatMap(Optional::stream)
                .map(HypothesisPainStageExecution::getCostUsd)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Vincula as execuções concluídas usadas no fechamento à hipótese gerada para auditoria posterior. */
    private void relateCompletedExecutionsToHypothesis(Long marketNicheId, Hypothesis hypothesis) {
        STAGE_SEQUENCE.stream()
                .map(stageCode -> executionRepository
                        .findTopByMarketNicheIdAndStageCodeAndStatusAndHypothesisIdIsNullOrderByExecutionRequestedAtDesc(
                                marketNicheId,
                                stageCode,
                                STATUS_COMPLETED))
                .flatMap(Optional::stream)
                .forEach(execution -> execution.setHypothesisId(hypothesis.getId()));
    }

    /** Monta o snapshot canônico Dor → Resultado → Mecanismo → Prova → Oferta aprovado para experimento. */
    private HypothesisFrameworkDto finalizedFramework(
            String title,
            String pain,
            String result,
            String mechanism,
            String proof,
            String offer) {
        HypothesisFrameworkDto dto = new HypothesisFrameworkDto();
        dto.getPain().setRoot(pain);
        dto.getPain().setSummary(pain);
        dto.getResult().setDesiredResult(result);
        dto.getResult().setSummary(result);
        dto.getMechanism().setCore(mechanism);
        dto.getMechanism().setUnique(mechanism);
        dto.getMechanism().setSummary(mechanism);
        dto.getProof().setMessage(proof);
        dto.getProof().setSummary(proof);
        dto.getOffer().setName(title);
        dto.getOffer().setCorePromise(result);
        dto.getOffer().setDeliverables(offer);
        dto.getOffer().setSummary(offer);
        dto.getChecklist().setPainReady(Boolean.TRUE);
        dto.getChecklist().setResultReady(Boolean.TRUE);
        dto.getChecklist().setMechanismReady(Boolean.TRUE);
        dto.getChecklist().setProofReady(Boolean.TRUE);
        dto.getChecklist().setOfferReady(Boolean.TRUE);
        dto.getChecklist().setApprovedForExperiment(Boolean.TRUE);
        return dto;
    }
}
