package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentTargetingSelection;
import com.marketinghub.experiment.dto.ExperimentSimpleFlowStatusDto;
import com.marketinghub.experiment.dto.ExperimentTargetingSelectionDto;
import com.marketinghub.experiment.dto.SaveExperimentTargetingSelectionsRequest;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingCandidateStatus;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.TargetingRequestOrigin;
import com.marketinghub.targeting.TargetingRequestStatus;
import com.marketinghub.targeting.dto.TargetingRequestDto;
import com.marketinghub.targeting.mapper.TargetingRequestMapper;
import com.marketinghub.targeting.mapper.TargetingResolutionSummaryMapper;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import com.marketinghub.repository.jpa.targeting.TargetingCandidateRepository;
import com.marketinghub.repository.jpa.targeting.TargetingRequestRepository;
import com.marketinghub.targeting.service.TargetingResolutionJobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Gerencia as escolhas de público do experimento e evita seleções sem identificador oficial da Meta.
 */
@Service
public class ExperimentTargetingSelectionService {
    private static final int SIMPLE_FLOW_RESOLUTION_ETA_SECONDS = 90;
    private final ExperimentRepository experimentRepository;
    private final ExperimentTargetingSelectionRepository repository;
    private final TargetingRequestRepository targetingRequestRepository;
    private final TargetingCandidateRepository targetingCandidateRepository;
    private final TargetingElementRepository targetingElementRepository;
    private final TargetingResolutionJobService targetingResolutionJobService;
    private final TargetingRequestMapper targetingRequestMapper;
    private final TargetingResolutionSummaryMapper targetingResolutionSummaryMapper;

    public ExperimentTargetingSelectionService(
            ExperimentRepository experimentRepository,
            ExperimentTargetingSelectionRepository repository,
            TargetingRequestRepository targetingRequestRepository,
            TargetingCandidateRepository targetingCandidateRepository,
            TargetingElementRepository targetingElementRepository,
            TargetingResolutionJobService targetingResolutionJobService,
            TargetingRequestMapper targetingRequestMapper,
            TargetingResolutionSummaryMapper targetingResolutionSummaryMapper) {
        this.experimentRepository = experimentRepository;
        this.repository = repository;
        this.targetingRequestRepository = targetingRequestRepository;
        this.targetingCandidateRepository = targetingCandidateRepository;
        this.targetingElementRepository = targetingElementRepository;
        this.targetingResolutionJobService = targetingResolutionJobService;
        this.targetingRequestMapper = targetingRequestMapper;
        this.targetingResolutionSummaryMapper = targetingResolutionSummaryMapper;
    }

    /**
     * Resolve o elemento selecionado e bloqueia itens que não podem ser publicados na Meta.
     */
    private TargetingElement resolveTargetingElement(Long targetingElementId,
                                                     Experiment experiment,
                                                     TargetingCandidateType candidateType) {
        if (targetingElementId == null) {
            throw new IllegalArgumentException("Elemento de segmentação oficial da Meta é obrigatório");
        }
        TargetingElement element = targetingElementRepository.findById(targetingElementId)
                .orElseThrow(() -> new IllegalArgumentException("Elemento de segmentação não encontrado"));
        if (element.getNiche() == null || element.getNiche().getId() == null
                || experiment.getNiche() == null || experiment.getNiche().getId() == null
                || !Objects.equals(element.getNiche().getId(), experiment.getNiche().getId())) {
            throw new IllegalArgumentException("Elemento de segmentação não pertence ao mesmo nicho do experimento");
        }
        TargetingElementType expectedType = mapCandidateType(candidateType);
        if (element.getType() != expectedType) {
            throw new IllegalArgumentException("Tipo do elemento não corresponde ao candidato informado");
        }
        if (experiment.getHypothesisRef() != null && element.getHypothesis() != null
                && !Objects.equals(experiment.getHypothesisRef().getId(), element.getHypothesis().getId())) {
            throw new IllegalArgumentException("Elemento vinculado a outra hipótese");
        }
        if (!StringUtils.hasText(element.getMetaId())) {
            throw new IllegalArgumentException("Elemento de segmentação sem ID oficial da Meta");
        }
        return element;
    }

    /**
     * Converte o tipo salvo na seleção para o tipo canônico do elemento de targeting.
     */
    private TargetingElementType mapCandidateType(TargetingCandidateType candidateType) {
        return switch (candidateType) {
            case INTEREST -> TargetingElementType.INTEREST;
            case BEHAVIOR -> TargetingElementType.BEHAVIOR;
            case WORK_POSITION -> TargetingElementType.JOB_TITLE;
        };
    }

    /**
     * Lista as escolhas de público já salvas para o experimento.
     */
    @Transactional(readOnly = true)
    public List<ExperimentTargetingSelectionDto> list(Long experimentId) {
        return repository.findByExperimentIdOrderByCandidateTypeAscTermAsc(experimentId).stream()
                .map(item -> ExperimentTargetingSelectionDto.builder()
                        .id(item.getId())
                        .experimentId(item.getExperiment().getId())
                        .candidateType(item.getCandidateType())
                        .term(item.getTerm())
                        .targetingElementId(item.getTargetingElement() != null ? item.getTargetingElement().getId() : null)
                        .build())
                .toList();
    }

    /**
     * Substitui as escolhas de público por elementos oficiais e publicáveis na Meta.
     */
    @Transactional
    public List<ExperimentTargetingSelectionDto> save(Long experimentId, SaveExperimentTargetingSelectionsRequest request) {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
        repository.deleteByExperimentId(experimentId);
        List<ExperimentTargetingSelection> items = new ArrayList<>();
        if (request != null && request.getItems() != null) {
            for (SaveExperimentTargetingSelectionsRequest.Item item : request.getItems()) {
                if (item.getCandidateType() == null || item.getTerm() == null || item.getTerm().isBlank()) {
                    continue;
                }
                TargetingElement linkedElement = resolveTargetingElement(item.getTargetingElementId(), experiment, item.getCandidateType());
                items.add(ExperimentTargetingSelection.builder()
                        .experiment(experiment)
                        .candidateType(item.getCandidateType())
                        .term(linkedElement.getTerm())
                        .targetingElement(linkedElement)
                        .build());
            }
        }
        return repository.saveAll(items).stream()
                .map(item -> ExperimentTargetingSelectionDto.builder()
                        .id(item.getId())
                        .experimentId(item.getExperiment().getId())
                        .candidateType(item.getCandidateType())
                        .term(item.getTerm())
                        .targetingElementId(item.getTargetingElement() != null ? item.getTargetingElement().getId() : null)
                        .build())
                .toList();
    }

    /**
     * Cria uma solicitação simples de resolução de público a partir das escolhas salvas.
     */
    @Transactional
    public TargetingRequestDto runSimpleFlow(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId).orElseThrow();
        List<ExperimentTargetingSelection> selections = repository.findByExperimentIdOrderByCandidateTypeAscTermAsc(experimentId);

        TargetingRequest request = TargetingRequest.builder()
                .descricao("Fluxo simples de público do experimento " + experimentId)
                .locale("pt_BR")
                .country("BR")
                .status(TargetingRequestStatus.COMPLETED)
                .origin(TargetingRequestOrigin.INTERNAL)
                .niche(experiment.getNiche())
                .hypothesis(experiment.getHypothesisRef())
                .experiment(experiment)
                .build();
        TargetingRequest savedRequest = targetingRequestRepository.save(request);

        List<TargetingCandidate> candidates = selections.stream()
                .map(selection -> TargetingCandidate.builder()
                        .request(savedRequest)
                        .seed(selection.getTerm().trim())
                        .seedVariants(List.of(selection.getTerm().trim().toLowerCase(Locale.ROOT)))
                        .type(selection.getCandidateType())
                        .status(TargetingCandidateStatus.PENDING_FACEBOOK_MATCH)
                        .origem("EXPERIMENT_SIMPLE_FLOW")
                        .localeHint("pt_BR")
                        .country("BR")
                        .build())
                .toList();
        List<TargetingCandidate> savedCandidates = targetingCandidateRepository.saveAll(candidates);
        targetingResolutionJobService.enqueueAfterCommit(savedRequest, savedCandidates);
        return targetingRequestMapper.toDetailedDto(savedRequest, SIMPLE_FLOW_RESOLUTION_ETA_SECONDS);
    }

    /**
     * Consulta o status mais recente do fluxo simples de resolução de público.
     */
    @Transactional(readOnly = true)
    public ExperimentSimpleFlowStatusDto getSimpleFlowStatus(Long experimentId) {
        if (experimentId == null) {
            return ExperimentSimpleFlowStatusDto.builder().build();
        }
        return targetingRequestRepository.findFirstByExperimentIdOrderByCreatedAtDesc(experimentId)
                .map(request -> {
                    TargetingRequestDto requestDto = targetingRequestMapper
                            .toDetailedDto(request, SIMPLE_FLOW_RESOLUTION_ETA_SECONDS);
                    var summary = targetingResolutionJobService
                            .summarizeByRequestIds(List.of(request.getId()))
                            .get(request.getId());
                    return ExperimentSimpleFlowStatusDto.builder()
                            .request(requestDto)
                            .resolution(targetingResolutionSummaryMapper.toDto(summary))
                            .build();
                })
                .orElseGet(() -> ExperimentSimpleFlowStatusDto.builder().build());
    }
}
