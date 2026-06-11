package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesLibraryPricingGateway;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Valida o cálculo de custos OpenAI mantido dentro do pacote da Biblioteca MOIS.
 */
@ExtendWith(MockitoExtension.class)
class MoisSalesLibraryPricingServiceTest {

    @Mock
    private MoisSalesLibraryPricingGateway pricingGateway;

    @InjectMocks
    private MoisSalesLibraryPricingService service;

    /**
     * Garante cálculo batch usando os preços obtidos do banco pelo gateway permitido do MOIS.
     */
    @Test
    void shouldEstimateBatchCostFromMoisPricingGateway() {
        given(pricingGateway.findPricingByModelCode("gpt-5.2"))
                .willReturn(Optional.of(new MoisSalesLibraryPricingGateway.ModelPricing(
                        new BigDecimal("1.25000"), new BigDecimal("10.00000"))));

        BigDecimal cost = service.estimateBatchCost("gpt-5.2", 250_000, 125_000);

        assertThat(cost).isEqualByComparingTo("1.5625");
    }

    /**
     * Garante erro controlado quando o modelo ainda não existe no catálogo de preços.
     */
    @Test
    void shouldFailWhenModelPricingIsMissing() {
        given(pricingGateway.findPricingByModelCode("modelo-ausente")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.estimateBatchCost("modelo-ausente", 1000, 1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Modelo OpenAI não encontrado");
    }
}
