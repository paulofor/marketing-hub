package com.marketinghub.repository.jpa.oprm.cnae;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a ponte OPRM que cria ou atualiza nichos confirmados. */
class OprmConfirmedMarketNicheRepositoryTest {

    /** Deve atualizar o nicho existente quando o CNAE já estiver vinculado a um MarketNiche. */
    @Test
    void shouldUpdateExistingConfirmedNiche() {
        MarketNicheRepository marketNicheRepository = mock(MarketNicheRepository.class);
        OprmConfirmedMarketNicheRepository repository = new OprmConfirmedMarketNicheRepository(marketNicheRepository);
        MarketNiche existing = new MarketNiche();
        existing.setId(24L);
        existing.setName("Nome antigo");
        when(marketNicheRepository.findById(24L)).thenReturn(Optional.of(existing));
        when(marketNicheRepository.save(existing)).thenReturn(existing);

        OprmConfirmedMarketNiche updated = repository.updateConfirmedNiche(
                24L, "Promoção de vendas atualizada", "Nova pesquisa auditável", new BigDecimal("0.19"));

        assertThat(updated.id()).isEqualTo(24L);
        assertThat(existing.getName()).isEqualTo("Promoção de vendas atualizada");
        assertThat(existing.getDescription()).isEqualTo("Nova pesquisa auditável");
        assertThat(existing.getCost()).isEqualByComparingTo("0.19");
        assertThat(existing.getTotalCost()).isEqualByComparingTo("0.19");
        verify(marketNicheRepository).save(existing);
    }
}
