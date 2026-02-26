package com.marketinghub.experiment.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentTargetingSelection;
import com.marketinghub.experiment.dto.ExperimentSimpleFlowStatusDto;
import com.marketinghub.experiment.dto.ExperimentTargetingSelectionDto;
import com.marketinghub.experiment.dto.SaveExperimentTargetingSelectionsRequest;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.experiment.repository.ExperimentTargetingSelectionRepository;
import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.TargetingCandidateStatus;
import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.TargetingRequestOrigin;
import com.marketinghub.targeting.TargetingRequestStatus;
import com.marketinghub.targeting.dto.TargetingRequestDto;
import com.marketinghub.targeting.mapper.TargetingRequestMapper;
import com.marketinghub.targeting.mapper.TargetingResolutionSummaryMapper;
import com.marketinghub.targeting.repository.TargetingCandidateRepository;
import com.marketinghub.targeting.repository.TargetingRequestRepository;
import com.marketinghub.targeting.service.TargetingResolutionJobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ExperimentTargetingSelectionService {
    private static final int SIMPLE_FLOW_RESOLUTION_ETA_SECONDS = 90;
    private final ExperimentRepository experimentRepository;
    private final ExperimentTargetingSelectionRepository repository;
    private final TargetingRequestRepository targetingRequestRepository;
    private final TargetingCandidateRepository targetingCandidateRepository;
    private final TargetingResolutionJobService targetingResolutionJobService;
    private final TargetingRequestMapper targetingRequestMapper;
    private final TargetingResolutionSummaryMapper targetingResolutionSummaryMapper;

    public ExperimentTargetingSelectionService(
            ExperimentRepository experimentRepository,
            ExperimentTargetingSelectionRepository repository,
            TargetingRequestRepository targetingRequestRepository,
            TargetingCandidateRepository targetingCandidateRepository,
            TargetingResolutionJobService targetingResolutionJobService,
            TargetingRequestMapper targetingRequestMapper,
            TargetingResolutionSummaryMapper targetingResolutionSummaryMapper) {
        this.experimentRepository = experimentRepository;
        this.repository = repository;
        this.targetingRequestRepository = targetingRequestRepository;
        this.targetingCandidateRepository = targetingCandidateRepository;
        this.targetingResolutionJobService = targetingResolutionJobService;
        this.targetingRequestMapper = targetingRequestMapper;
        this.targetingResolutionSummaryMapper = targetingResolutionSummaryMapper;
    }

    @Transactional(readOnly = true)
    public List<ExperimentTargetingSelectionDto> list(Long experimentId) {
        return repository.findByExperimentIdOrderByCandidateTypeAscTermAsc(experimentId).stream()
                .map(item -> ExperimentTargetingSelectionDto.builder()
                        .id(item.getId())
                        .experimentId(item.getExperiment().getId())
                        .candidateType(item.getCandidateType())
                        .term(item.getTerm())
                        .build())
                .toList();
    }

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
                items.add(ExperimentTargetingSelection.builder()
                        .experiment(experiment)
                        .candidateType(item.getCandidateType())
                        .term(item.getTerm().trim())
                        .build());
            }
        }
        return repository.saveAll(items).stream()
                .map(item -> ExperimentTargetingSelectionDto.builder()
                        .id(item.getId())
                        .experimentId(item.getExperiment().getId())
                        .candidateType(item.getCandidateType())
                        .term(item.getTerm())
                        .build())
                .toList();
    }

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
