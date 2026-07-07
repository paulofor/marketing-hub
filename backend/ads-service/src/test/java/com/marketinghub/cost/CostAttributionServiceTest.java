package com.marketinghub.cost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Garante que a atribuição de custos não duplica valores persistidos.
 */
@ExtendWith(MockitoExtension.class)
class CostAttributionServiceTest {

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private HypothesisRepository hypothesisRepository;

    @Mock
    private MarketNicheRepository marketNicheRepository;

    @Mock
    private CurrencyConversionService currencyConversionService;

    @Mock
    private EntityManager entityManager;

    private CostAttributionService service;

    /** Prepara o serviço real com dependências controladas. */
    @BeforeEach
    void setUp() {
        service = new CostAttributionService(
                experimentRepository,
                hypothesisRepository,
                marketNicheRepository,
                currencyConversionService);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    /**
     * Confirma que entidade gerenciada recebe apenas alteração em memória para o flush do JPA persistir.
     */
    @Test
    void addCostToExperimentHierarchyDoesNotSqlIncrementManagedExperiment() {
        Experiment experiment = Experiment.builder()
                .id(59L)
                .totalCost(new BigDecimal("10.00"))
                .build();

        when(currencyConversionService.normalizeBrl(new BigDecimal("1.25")))
                .thenReturn(new BigDecimal("1.25"));
        when(entityManager.contains(experiment)).thenReturn(true);

        service.addCostToExperimentHierarchy(experiment, new BigDecimal("1.25"));

        assertThat(experiment.getTotalCost()).isEqualByComparingTo("11.25");
        verify(experimentRepository, never()).incrementTotalCost(59L, new BigDecimal("1.25"));
    }

    /**
     * Confirma que entidade destacada ainda recebe incremento SQL para persistir o delta.
     */
    @Test
    void addCostToExperimentHierarchySqlIncrementsDetachedExperiment() {
        Experiment experiment = Experiment.builder()
                .id(59L)
                .totalCost(new BigDecimal("10.00"))
                .build();

        when(currencyConversionService.normalizeBrl(new BigDecimal("1.25")))
                .thenReturn(new BigDecimal("1.25"));
        when(entityManager.contains(experiment)).thenReturn(false);

        service.addCostToExperimentHierarchy(experiment, new BigDecimal("1.25"));

        assertThat(experiment.getTotalCost()).isEqualByComparingTo("11.25");
        verify(experimentRepository).incrementTotalCost(59L, new BigDecimal("1.25"));
    }
}
