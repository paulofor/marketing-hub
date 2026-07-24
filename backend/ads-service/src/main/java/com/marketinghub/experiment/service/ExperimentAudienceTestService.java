package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentAudienceTest;
import com.marketinghub.experiment.ExperimentAudienceTestItem;
import com.marketinghub.experiment.ExperimentAudienceTestStatus;
import com.marketinghub.experiment.dto.CreateExperimentAudienceTestRequest;
import com.marketinghub.experiment.dto.ExperimentAudienceTestDto;
import com.marketinghub.repository.jpa.experiment.ExperimentAudienceTestRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Coordena o planejamento de variações de público sem alterar campanhas em execução automaticamente.
 */
@Service
public class ExperimentAudienceTestService {
    private final ExperimentRepository experimentRepository;
    private final ExperimentAudienceTestRepository audienceTestRepository;
    private final TargetingElementRepository targetingElementRepository;

    public ExperimentAudienceTestService(
            ExperimentRepository experimentRepository,
            ExperimentAudienceTestRepository audienceTestRepository,
            TargetingElementRepository targetingElementRepository) {
        this.experimentRepository = experimentRepository;
        this.audienceTestRepository = audienceTestRepository;
        this.targetingElementRepository = targetingElementRepository;
    }

    /**
     * Lista as variações de público planejadas para o experimento.
     */
    @Transactional(readOnly = true)
    public List<ExperimentAudienceTestDto> list(Long experimentId) {
        return audienceTestRepository.findByExperimentIdWithItems(experimentId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Cria uma variação de público em rascunho com elementos Meta válidos do mesmo nicho.
     */
    @Transactional
    public ExperimentAudienceTestDto create(Long experimentId, CreateExperimentAudienceTestRequest request) {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
        ExperimentAudienceTest test = ExperimentAudienceTest.builder()
                .experiment(experiment)
                .name(request.getName().trim())
                .hypothesis(request.getHypothesis().trim())
                .successMetric(request.getSuccessMetric().trim())
                .dailyBudget(normalizeBudget(request.getDailyBudget()))
                .status(ExperimentAudienceTestStatus.DRAFT)
                .build();
        List<ExperimentAudienceTestItem> items = request.getItems().stream()
                .map(item -> toItem(experiment, item))
                .toList();
        test.replaceItems(items);
        return toDto(audienceTestRepository.save(test));
    }

    /**
     * Remove uma variação de público que ainda não foi publicada.
     */
    @Transactional
    public void delete(Long experimentId, Long audienceTestId) {
        ExperimentAudienceTest test = audienceTestRepository.findById(audienceTestId)
                .orElseThrow(() -> new IllegalArgumentException("Teste de público não encontrado"));
        if (test.getExperiment() == null || !Objects.equals(test.getExperiment().getId(), experimentId)) {
            throw new IllegalArgumentException("Teste de público não pertence ao experimento informado");
        }
        if (test.getStatus() == ExperimentAudienceTestStatus.RUNNING) {
            throw new IllegalArgumentException("Teste de público em execução não pode ser removido");
        }
        audienceTestRepository.delete(test);
    }

    /**
     * Converte a requisição em item persistível validando nicho, tipo e ID oficial da Meta.
     */
    private ExperimentAudienceTestItem toItem(Experiment experiment, CreateExperimentAudienceTestRequest.Item item) {
        TargetingElement element = targetingElementRepository.findById(item.getTargetingElementId())
                .orElseThrow(() -> new IllegalArgumentException("Elemento de segmentação não encontrado"));
        validateElement(experiment, element, item.getCandidateType());
        return ExperimentAudienceTestItem.builder()
                .candidateType(item.getCandidateType())
                .term(element.getTerm())
                .targetingElement(element)
                .build();
    }

    /**
     * Bloqueia públicos fora do nicho, de tipo errado ou sem identificador oficial da Meta.
     */
    private void validateElement(Experiment experiment, TargetingElement element, TargetingCandidateType candidateType) {
        if (element.getNiche() == null || element.getNiche().getId() == null
                || experiment.getNiche() == null || experiment.getNiche().getId() == null
                || !Objects.equals(element.getNiche().getId(), experiment.getNiche().getId())) {
            throw new IllegalArgumentException("Elemento de segmentação não pertence ao mesmo nicho do experimento");
        }
        if (element.getType() != mapCandidateType(candidateType)) {
            throw new IllegalArgumentException("Tipo do elemento não corresponde ao candidato informado");
        }
        if (!StringUtils.hasText(element.getMetaId())) {
            throw new IllegalArgumentException("Elemento de segmentação sem ID oficial da Meta");
        }
    }

    /**
     * Converte o tipo de candidato para o tipo canônico do elemento de targeting.
     */
    private TargetingElementType mapCandidateType(TargetingCandidateType candidateType) {
        return switch (candidateType) {
            case INTEREST -> TargetingElementType.INTEREST;
            case BEHAVIOR -> TargetingElementType.BEHAVIOR;
            case WORK_POSITION -> TargetingElementType.JOB_TITLE;
        };
    }

    /**
     * Normaliza orçamento vazio ou negativo para ausência de orçamento próprio.
     */
    private BigDecimal normalizeBudget(BigDecimal dailyBudget) {
        if (dailyBudget == null || dailyBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return dailyBudget;
    }

    /**
     * Monta o DTO com alcance estimado pela soma dos intervalos dos itens.
     */
    private ExperimentAudienceTestDto toDto(ExperimentAudienceTest test) {
        Long lower = 0L;
        Long upper = 0L;
        List<ExperimentAudienceTestDto.Item> items = test.getItems().stream()
                .map(item -> {
                    TargetingElement element = item.getTargetingElement();
                    return ExperimentAudienceTestDto.Item.builder()
                            .id(item.getId())
                            .candidateType(item.getCandidateType())
                            .term(item.getTerm())
                            .targetingElementId(element != null ? element.getId() : null)
                            .metaId(element != null ? element.getMetaId() : null)
                            .metaKey(element != null ? element.getMetaKey() : null)
                            .metaAudienceSizeLowerBound(element != null ? element.getMetaAudienceSizeLowerBound() : null)
                            .metaAudienceSizeUpperBound(element != null ? element.getMetaAudienceSizeUpperBound() : null)
                            .build();
                })
                .toList();
        for (ExperimentAudienceTestDto.Item item : items) {
            long itemLower = item.metaAudienceSizeLowerBound() != null ? item.metaAudienceSizeLowerBound() : 0L;
            long itemUpper = item.metaAudienceSizeUpperBound() != null ? item.metaAudienceSizeUpperBound() : itemLower;
            lower += itemLower;
            upper += itemUpper;
        }
        return ExperimentAudienceTestDto.builder()
                .id(test.getId())
                .experimentId(test.getExperiment().getId())
                .name(test.getName())
                .hypothesis(test.getHypothesis())
                .successMetric(test.getSuccessMetric())
                .dailyBudget(test.getDailyBudget())
                .status(test.getStatus())
                .audienceSizeLowerBound(lower)
                .audienceSizeUpperBound(upper)
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .items(items)
                .build();
    }
}
