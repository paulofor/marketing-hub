package com.marketinghub.facebookads.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentTargetingSelection;
import com.marketinghub.experiment.mapper.ExperimentMapper;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentTargetingSelectionRepository;
import com.marketinghub.facebookads.dto.ExperimentReadyForAdSetDto;
import com.marketinghub.facebookads.service.targetingPackage.FacebookAdSetTargetingPackageDto;
import com.marketinghub.facebookads.dto.TargetingPackageDto;
import com.marketinghub.hypothesis.mapper.HypothesisMapper;
import com.marketinghub.niche.mapper.MarketNicheMapper;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.TargetingElementStatus;
import com.marketinghub.targeting.TargetingElementType;
import com.marketinghub.targeting.dto.TargetingElementDto;
import com.marketinghub.targeting.mapper.TargetingElementMapper;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Builds the payload consumed by workers to generate Facebook ad sets.
 */
@Service
public class FacebookAdSetExperimentService {
    private static final List<ExperimentStatus> STATUSES = List.of(
            ExperimentStatus.PLANNED, ExperimentStatus.RUNNING, ExperimentStatus.PAUSED);

    private final ExperimentRepository experimentRepository;
    private final ExperimentTargetingSelectionRepository targetingSelectionRepository;
    private final TargetingElementRepository targetingElementRepository;
    private final ExperimentMapper experimentMapper;
    private final MarketNicheMapper marketNicheMapper;
    private final HypothesisMapper hypothesisMapper;
    private final TargetingElementMapper targetingElementMapper;

    /**
     * Inicializa o serviço com repositórios e mapeadores usados para montar o contrato do worker.
     */
    public FacebookAdSetExperimentService(ExperimentRepository experimentRepository,
                                          ExperimentTargetingSelectionRepository targetingSelectionRepository,
                                          TargetingElementRepository targetingElementRepository,
                                          ExperimentMapper experimentMapper,
                                          MarketNicheMapper marketNicheMapper,
                                          HypothesisMapper hypothesisMapper,
                                          TargetingElementMapper targetingElementMapper) {
        this.experimentRepository = experimentRepository;
        this.targetingSelectionRepository = targetingSelectionRepository;
        this.targetingElementRepository = targetingElementRepository;
        this.experimentMapper = experimentMapper;
        this.marketNicheMapper = marketNicheMapper;
        this.hypothesisMapper = hypothesisMapper;
        this.targetingElementMapper = targetingElementMapper;
    }

    /**
     * Lista todos os experimentos prontos para conjuntos de anúncios.
     */
    public List<ExperimentReadyForAdSetDto> listExperimentsReadyForAdSets() {
        return listExperimentsReadyForAdSets(null);
    }

    /**
     * Lista experimentos prontos para conjuntos de anúncios, priorizando o público selecionado no experimento.
     */
    public List<ExperimentReadyForAdSetDto> listExperimentsReadyForAdSets(Long experimentId) {
        if (experimentId != null) {
            return experimentRepository.findForAdSetTargetingById(experimentId, ExperimentPlatform.FACEBOOK)
                    .map(this::toReadyForAdSetDto)
                    .map(List::of)
                    .orElseGet(List::of);
        }
        List<Experiment> experiments = experimentRepository.findAllReadyForAdSets(
                ExperimentPlatform.FACEBOOK, STATUSES);
        if (experiments.isEmpty()) {
            return List.of();
        }
        List<ExperimentReadyForAdSetDto> result = new ArrayList<>();
        for (Experiment experiment : experiments) {
            ExperimentReadyForAdSetDto dto = toReadyForAdSetDto(experiment);
            if (dto != null) {
                result.add(dto);
            }
        }
        return result;
    }

    /**
     * Retorna somente o pacote de segmentação necessário para o worker publicar a campanha do experimento.
     */
    public Optional<FacebookAdSetTargetingPackageDto> getTargetingPackageForCampaign(Long experimentId) {
        return experimentRepository.findForAdSetTargetingById(experimentId, ExperimentPlatform.FACEBOOK)
                .flatMap(experiment -> {
                    TargetingPackageDto targeting = buildTargetingPackage(experiment);
                    if (targeting == null) {
                        return Optional.empty();
                    }
                    return Optional.of(new FacebookAdSetTargetingPackageDto(experiment.getId(), targeting));
                });
    }

    /**
     * Converte um experimento em contrato de ad set quando existe pacote de segmentação publicável.
     */
    private ExperimentReadyForAdSetDto toReadyForAdSetDto(Experiment experiment) {
        TargetingPackageDto targeting = buildTargetingPackage(experiment);
        if (targeting == null) {
            return null;
        }
        return new ExperimentReadyForAdSetDto(
                experimentMapper.toDto(experiment),
                marketNicheMapper.toDto(experiment.getNiche()),
                experiment.getHypothesisRef() != null ? hypothesisMapper.toDto(experiment.getHypothesisRef()) : null,
                targeting);
    }

    /**
     * Monta o pacote de público usando as seleções do experimento e, se não existirem, o pacote aprovado do nicho.
     */
    private TargetingPackageDto buildTargetingPackage(Experiment experiment) {
        if (experiment.getNiche() == null) {
            return null;
        }
        TargetingPackageDto selectedTargeting = buildSelectedTargetingPackage(experiment);
        if (selectedTargeting != null) {
            return selectedTargeting;
        }
        return buildApprovedNicheTargetingPackage(experiment);
    }

    /**
     * Monta o público salvo na tela do experimento com elementos aprovados e identificáveis pela Meta.
     */
    private TargetingPackageDto buildSelectedTargetingPackage(Experiment experiment) {
        List<ExperimentTargetingSelection> selections = targetingSelectionRepository
                .findByExperimentIdWithTargetingElement(experiment.getId());
        if (selections.isEmpty()) {
            return null;
        }
        Map<TargetingElementType, List<TargetingElementDto>> mapped = emptyTargetingMap();
        for (ExperimentTargetingSelection selection : selections) {
            TargetingElement element = selection.getTargetingElement();
            if (!isPublishableTargetingElement(element)) {
                continue;
            }
            mapped.get(element.getType()).add(targetingElementMapper.toDto(element));
        }
        if (mapped.get(TargetingElementType.JOB_TITLE).isEmpty()) {
            return null;
        }
        return toTargetingPackage(mapped);
    }

    /**
     * Monta o pacote aprovado por nicho quando o experimento ainda não tem seleção manual salva.
     */
    private TargetingPackageDto buildApprovedNicheTargetingPackage(Experiment experiment) {
        Long nicheId = experiment.getNiche().getId();
        UUID hypothesisId = experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getId() : null;
        Map<TargetingElementType, List<TargetingElementDto>> mapped = emptyTargetingMap();
        for (TargetingElementType type : TargetingElementType.values()) {
            List<TargetingElementDto> dtos = mapElements(nicheId, hypothesisId, type);
            if (type == TargetingElementType.JOB_TITLE && dtos.isEmpty()) {
                return null;
            }
            mapped.put(type, dtos);
        }
        return toTargetingPackage(mapped);
    }

    /**
     * Cria o mapa base com as categorias de público suportadas pela Meta.
     */
    private Map<TargetingElementType, List<TargetingElementDto>> emptyTargetingMap() {
        Map<TargetingElementType, List<TargetingElementDto>> mapped = new EnumMap<>(TargetingElementType.class);
        mapped.put(TargetingElementType.INTEREST, new ArrayList<>());
        mapped.put(TargetingElementType.JOB_TITLE, new ArrayList<>());
        mapped.put(TargetingElementType.BEHAVIOR, new ArrayList<>());
        return mapped;
    }

    /**
     * Converte o mapa de elementos no DTO consumido pelo worker de Facebook Ads.
     */
    private TargetingPackageDto toTargetingPackage(Map<TargetingElementType, List<TargetingElementDto>> mapped) {
        return new TargetingPackageDto(
                List.copyOf(mapped.get(TargetingElementType.INTEREST)),
                List.copyOf(mapped.get(TargetingElementType.JOB_TITLE)),
                List.copyOf(mapped.get(TargetingElementType.BEHAVIOR)));
    }

    /**
     * Indica se o elemento selecionado pode ser enviado para a Meta com identificador oficial.
     */
    private boolean isPublishableTargetingElement(TargetingElement element) {
        return element != null
                && element.getStatus() == TargetingElementStatus.APPROVED
                && element.getType() != null
                && StringUtils.hasText(element.getMetaId());
    }

    /**
     * Busca elementos aprovados por nicho e hipótese para o fallback sem seleção manual.
     */
    private List<TargetingElementDto> mapElements(Long nicheId, UUID hypothesisId, TargetingElementType type) {
        List<TargetingElement> elements = targetingElementRepository.findApprovedForExperiment(nicheId, type, hypothesisId);
        if (elements.isEmpty()) {
            return List.of();
        }
        return elements.stream()
                .filter(this::isPublishableTargetingElement)
                .map(targetingElementMapper::toDto)
                .toList();
    }
}
