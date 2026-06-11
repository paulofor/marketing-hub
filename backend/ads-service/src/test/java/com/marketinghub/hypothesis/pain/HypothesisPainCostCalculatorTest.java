package com.marketinghub.hypothesis.pain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar o cálculo interno de custo flex da etapa Dor. */
@ExtendWith(MockitoExtension.class)
class HypothesisPainCostCalculatorTest {

    @Mock
    private OpenAiModelRepository openAiModelRepository;

    @Mock
    private MarketNicheRepository marketNicheRepository;

    @Mock
    private CurrencyConversionService currencyConversionService;

    private HypothesisPainCostCalculator calculator;

    /** Prepara o calculador com dependências isoladas para cada cenário. */
    @BeforeEach
    void setup() {
        calculator = new HypothesisPainCostCalculator(
                openAiModelRepository,
                marketNicheRepository,
                currencyConversionService);
    }

    /** Deve calcular custo usando preços flex por um milhão de tokens do modelo cadastrado. */
    @Test
    void calculateFlexCostUsdUsesBatchPricesPerMillionTokens() {
        OpenAiModel model = OpenAiModel.builder()
                .code("gpt-5.2")
                .priceInputBatch(new BigDecimal("1.25000"))
                .priceOutputBatch(new BigDecimal("10.00000"))
                .build();
        when(openAiModelRepository.findByCode("gpt-5.2")).thenReturn(Optional.of(model));

        BigDecimal cost = calculator.calculateFlexCostUsd("gpt-5.2", 1200, 300);

        assertThat(cost).isEqualByComparingTo(new BigDecimal("0.00450000"));
    }

    /** Deve converter o delta USD para BRL e incrementar o custo acumulado do nicho. */
    @Test
    void addFlexCostDeltaToNicheConvertsUsdAndIncrementsNicheTotalCost() {
        MarketNiche niche = new MarketNiche();
        niche.setId(18L);
        niche.setTotalCost(new BigDecimal("2.00"));
        when(currencyConversionService.usdToBrl(new BigDecimal("0.00450000")))
                .thenReturn(new BigDecimal("0.02"));

        calculator.addFlexCostDeltaToNiche(niche, new BigDecimal("0.00450000"));

        assertThat(niche.getTotalCost()).isEqualByComparingTo(new BigDecimal("2.02"));
        verify(marketNicheRepository).incrementTotalCost(18L, new BigDecimal("0.02"));
    }
}
