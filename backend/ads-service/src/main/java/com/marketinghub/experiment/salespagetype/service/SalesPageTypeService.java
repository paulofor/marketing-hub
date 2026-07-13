package com.marketinghub.experiment.salespagetype.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.salespagetype.ExperimentSalesPageTypeSelection;
import com.marketinghub.experiment.salespagetype.SalesPageType;
import com.marketinghub.experiment.salespagetype.service.listtypes.ExperimentSalesPageTypeSelectionResponse;
import com.marketinghub.experiment.salespagetype.service.listtypes.SalesPageTypeResponse;
import com.marketinghub.experiment.salespagetype.service.updateselection.UpdateExperimentSalesPageTypeSelectionItem;
import com.marketinghub.experiment.salespagetype.service.updateselection.UpdateExperimentSalesPageTypeSelectionRequest;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.salespagetype.ExperimentSalesPageTypeSelectionRepository;
import com.marketinghub.repository.jpa.experiment.salespagetype.SalesPageTypeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: orquestrar catalogo e selecao de tipos de pagina de venda por experimento. */
@Service
public class SalesPageTypeService {
    private static final BigDecimal DEFAULT_WEIGHT = new BigDecimal("50.00");

    private final SalesPageTypeRepository typeRepository;
    private final ExperimentSalesPageTypeSelectionRepository selectionRepository;
    private final ExperimentRepository experimentRepository;

    /** Inicializa o servico com repositories de catalogo, selecao e experimento. */
    public SalesPageTypeService(
            SalesPageTypeRepository typeRepository,
            ExperimentSalesPageTypeSelectionRepository selectionRepository,
            ExperimentRepository experimentRepository) {
        this.typeRepository = typeRepository;
        this.selectionRepository = selectionRepository;
        this.experimentRepository = experimentRepository;
    }

    /** Lista todos os tipos ativos disponiveis para campanhas e testes A/B. */
    @Transactional(readOnly = true)
    public List<SalesPageTypeResponse> listActiveTypes() {
        return typeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toTypeResponse)
                .toList();
    }

    /** Lista os tipos selecionados para o experimento informado. */
    @Transactional(readOnly = true)
    public List<ExperimentSalesPageTypeSelectionResponse> listExperimentSelections(Long experimentId) {
        ensureExperimentExists(experimentId);
        return selectionRepository.findByExperimentIdOrderByVariantKeyAsc(experimentId).stream()
                .map(this::toSelectionResponse)
                .toList();
    }

    /** Substitui a selecao completa de tipos de pagina de venda de um experimento. */
    @Transactional
    public List<ExperimentSalesPageTypeSelectionResponse> replaceExperimentSelections(
            Long experimentId,
            UpdateExperimentSalesPageTypeSelectionRequest request) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "experiment not found"));
        List<UpdateExperimentSalesPageTypeSelectionItem> requestedSelections =
                request == null || request.selections() == null ? List.of() : request.selections();
        if (requestedSelections.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "at least one sales page type is required");
        }

        selectionRepository.deleteByExperimentId(experimentId);
        List<ExperimentSalesPageTypeSelection> entities = new ArrayList<>();
        Set<String> typeCodes = new HashSet<>();
        Set<String> variantKeys = new HashSet<>();
        int index = 0;
        for (UpdateExperimentSalesPageTypeSelectionItem item : requestedSelections) {
            index++;
            String typeCode = normalizeRequired(item.typeCode(), "typeCode");
            if (!typeCodes.add(typeCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicated typeCode: " + typeCode);
            }
            SalesPageType type = typeRepository.findById(typeCode)
                    .filter(SalesPageType::isActive)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "active sales page type not found: " + typeCode));
            String variantKey = normalizeVariantKey(item.variantKey(), index);
            if (!variantKeys.add(variantKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicated variantKey: " + variantKey);
            }
            entities.add(ExperimentSalesPageTypeSelection.builder()
                    .experiment(experiment)
                    .salesPageType(type)
                    .variantKey(variantKey)
                    .trafficWeight(normalizeTrafficWeight(item.trafficWeight()))
                    .active(item.active() == null || Boolean.TRUE.equals(item.active()))
                    .notes(normalizeOptional(item.notes(), 1024))
                    .build());
        }
        return selectionRepository.saveAll(entities).stream()
                .map(this::toSelectionResponse)
                .toList();
    }

    /** Confirma que o experimento existe antes de consultar selecoes. */
    private void ensureExperimentExists(Long experimentId) {
        if (!experimentRepository.existsById(experimentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "experiment not found");
        }
    }

    /** Normaliza campos obrigatorios textuais. */
    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /** Normaliza a chave da variante ou gera uma chave sequencial para A/B. */
    private String normalizeVariantKey(String value, int index) {
        if (!StringUtils.hasText(value)) {
            return String.valueOf((char) ('A' + index - 1));
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 16) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "variantKey must have at most 16 characters");
        }
        return normalized;
    }

    /** Normaliza o peso de trafego da variante. */
    private BigDecimal normalizeTrafficWeight(BigDecimal value) {
        BigDecimal normalized = value == null ? DEFAULT_WEIGHT : value;
        if (normalized.compareTo(BigDecimal.ZERO) <= 0 || normalized.compareTo(new BigDecimal("100.00")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trafficWeight must be between 0 and 100");
        }
        return normalized.setScale(2, RoundingMode.HALF_UP);
    }

    /** Normaliza texto opcional com limite de tamanho. */
    private String normalizeOptional(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    /** Converte entidade de catalogo para contrato de resposta. */
    private SalesPageTypeResponse toTypeResponse(SalesPageType type) {
        return new SalesPageTypeResponse(
                type.getCode(),
                type.getName(),
                type.getDescription(),
                type.getCommercialMechanism(),
                type.getLeadCaptureStrategy(),
                type.getDigitalBaitDelivery(),
                type.isDefaultForAbTest(),
                type.isActive());
    }

    /** Converte selecao persistida para contrato de resposta. */
    private ExperimentSalesPageTypeSelectionResponse toSelectionResponse(ExperimentSalesPageTypeSelection selection) {
        SalesPageTypeResponse type = toTypeResponse(selection.getSalesPageType());
        return new ExperimentSalesPageTypeSelectionResponse(
                selection.getId(),
                type.code(),
                type.name(),
                selection.getVariantKey(),
                selection.getTrafficWeight(),
                selection.isActive(),
                selection.getNotes(),
                type);
    }
}
